package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import java.util.*

class Video(val memory: MemoryMapper, val interruptManager: InterruptManager) {

    companion object {
        val BackgroundTileMapSize = 32*32
        val VBlankStartLine = 144
    }
    val windowX = MemoryRegister("WindowX", memory, 0xFF4B)
    val windowY = MemoryRegister("WindowY", memory, 0xFF4A)
    val objPalette1Data = MemoryRegister("OBJ Palette 1 Data", memory, 0xFF49)
    val objPalette0Data = MemoryRegister("OBJ Palette 0 Data", memory, 0xFF48)
    val bgPaletteData = MemoryRegister("BG Palette Data", memory, 0xFF47)
    // TODO: CGB palettes + VRAM bank
    val lyCompare = MemoryRegister("LYC", memory, 0xFF45)
    val lcdcY = memory.lyRegister
    val scrollX = MemoryRegister("ScrollX", memory, 0xFF43)
    val scrollY = MemoryRegister("ScrollY", memory, 0xFF42)
    val windowTileMapAddress get()= if(windowTileMapSelect) 0x9C00 else 0x9800
    val backgroundTileMapAddress get()= if(bgTileMapSelect) 0x9C00 else 0x9800
    val tileDataAddress get()= if(dataSelect) 0x8000 else 0x9000 // 0x8800
    val pixelData = IntArray(256*256)
    val tileDataTable = TileDataTable(this, memory)

    val lcdControl = MemoryRegister("LCD Control", memory, 0xFF40)
    var lcdDisplayEnable by lcdControl.bitVar(7)
    var windowTileMapSelect by lcdControl.bitVar(6)
    var windowDisplayEnable by lcdControl.bitVar(5)
    var dataSelect by lcdControl.bitVar(4)
    var bgTileMapSelect by lcdControl.bitVar(3)
    var spriteSizeSelect by lcdControl.bitVar(2)
    var spriteDisplayEnable by lcdControl.bitVar(1)
    var bgDisplay by lcdControl.bitVar(0) // FIXME: GB only

    val lcdStatus = MemoryRegister("LCDC Status", memory, 0xFF41)
    var coincidenceInterrupt by lcdStatus.bitVar(6)
    var mode2OamInterrupt by lcdStatus.bitVar(5)
    var mode1VBlankInterrupt by lcdStatus.bitVar(4)
    var mode0HBlankInterrupt by lcdStatus.bitVar(3)
    var coincidenceFlag by lcdStatus.bitVar(2)
    val modeFlag get() = lcdStatus.getValue() and 0b11

    var currentClockCycles = 0
    var mode: VideoMode = VideoMode.VBlank

    enum class VideoMode(val durationInCycles: Int) {
        HBlank(200),
        VBlank(100000),
        Mode2(80),
        Mode3(170)
    }

    fun drawTileRow(x: Int, row: Int, tileLocalRow: Int, tileAddress: Int, palette: MemoryRegister,
                    target: IntArray = pixelData,
                    isBackground: Boolean = false,
                    vMirror: Boolean = false,
                    hMirror: Boolean = false,
                    tileHeight: Int = 8) {
        var screenY = row
        if(isBackground) { // background wraps
            screenY = wrapInBounds(row)
        } else if(screenY >= 256 || screenY < 0) {
            return
        }
        val effectiveTileRow = if(vMirror) tileHeight-1-tileLocalRow else tileLocalRow
        val lineDataLS = memory.read(tileAddress + effectiveTileRow*2)
        val lineDataMS = memory.read(tileAddress + effectiveTileRow*2 +1)
        for(i in 0..7) {
            var screenX = x + i
            if(isBackground) { // background wraps
                screenX = wrapInBounds(screenX)
            } else if(screenX >= 256 || screenX < 0) {
                continue
            }
            val effectiveTileColumn = if(hMirror) i else (7-i)
            val highColor = if(lineDataMS and (1 shl effectiveTileColumn) != 0) 1 else 0
            val lowColor = if(lineDataLS and (1 shl effectiveTileColumn) != 0) 1 else 0
            val pixelColorIndex = (highColor shl 1) + lowColor
            if(pixelColorIndex == 0 && !isBackground) // transparent pixel
                continue
            val color = pixelColor(pixelColorIndex, palette)
            target[screenY*256+screenX] = color
        }
    }

    private tailrec fun wrapInBounds(value: Int): Int {
        if(value >= 0)
            return value % 256
        return wrapInBounds(value+256)
    }

    private fun pixelColor(index: Int, palette: MemoryRegister): Int {
        val data = palette.getValue() and (0b11 shl (index*2)) shr (index*2)
        return when(data) {
            3 -> 0xFF000000.toInt()
            2 -> 0xFF555555.toInt()
            1 -> 0xFFAAAAAA.toInt()
            0 -> 0xFFFFFFFF.toInt()
            else -> error("This is impossible!")
        }
    }

    val WIDTH = 256
    val HEIGHT = 256

    fun scanLine() {
        val line = lcdcY.getValue()

        Arrays.fill(pixelData, line*WIDTH, (line+1)*WIDTH, 0xFFFF0000.toInt())

        if(line < VBlankStartLine && lcdDisplayEnable) {
            if(bgDisplay) {
                val scrolledY = wrapInBounds(line + scrollY.getValue())
                for(x in 0 until 32) {
                    val scrolledX = wrapInBounds(x * 8 + scrollX.getValue())
                    val tileNumber = memory.read(backgroundTileMapAddress + scrolledY/8 *32 + scrolledX/8)
                    val tileAddress = tileDataAddress
                    val offset = (if(dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                    drawTileRow(x*8-scrolledX%8, line, scrolledY %8, tileAddress + offset, bgPaletteData, isBackground = true)
                }
            }
            if(windowDisplayEnable) {
                val effectiveLine = line-windowY.getValue()
                if(effectiveLine in 0..255) {
                    for(x in 0 until 32) {
                        val effectiveX = x*8-windowX.getValue()+7
                        val tileNumber = memory.read(windowTileMapAddress + (effectiveLine / 8) * 32 + effectiveX/8)
                        val tileAddress = tileDataAddress
                        val offset = (if (dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                        drawTileRow(effectiveX, line, effectiveLine % 8, tileAddress + offset, bgPaletteData, isBackground = true)
                    }
                }
            }

            if(spriteDisplayEnable) {
                val spriteTable = memory.spriteAttributeTable
                val sprites = try {
                    spriteTable.sprites.sorted()
                } catch (e: IllegalArgumentException) {
                    spriteTable.reloadSprites()
                    // for some reason the sort fails sometimes
                    spriteTable.sprites.asList()
                }
                // draw only the 10 first sprites on this scanline
                sprites
                        .filter(SpriteAttributeTable.Sprite::visible)
                        .filter { sprite ->
                            val posX = sprite.positionX.getValue()-8
                            posX+8 > 0 && posX <= 256
                        }
                        /*.take(10)*/
                        .forEach { sprite ->
                            val palette = if(sprite.paletteNumber) objPalette1Data else objPalette0Data
                            val posY = sprite.positionY.getValue()-8-1
                            val posX = sprite.positionX.getValue()-8
                            val tileNumber = sprite.tileNumber.getValue()
                            val offset = tileNumber.asUnsigned8()
                            val tileAddress = 0x8000 + offset*2*8

                            if(spriteSizeSelect) {
                                if(posY+8 in line..(line+15)) {
                                    drawTileRow(posX, line, posY+8-line, tileAddress, palette, hMirror = sprite.hMirror, vMirror = !sprite.vMirror, tileHeight = 16)
                                }
                            } else {
                                if(posY in line..(line+7)) {
                                    drawTileRow(posX, line, posY-line, tileAddress, palette, hMirror = sprite.hMirror, vMirror = !sprite.vMirror)
                                }
                            }
                }
            }
        }
    }

    fun step(clockCycles: Int) {
        currentClockCycles += clockCycles

        if(!lcdDisplayEnable) {
            mode = VideoMode.HBlank
            lcdStatus.setValue(lcdStatus.getValue().setBits(mode.ordinal, 0..1))
            lcdcY.setValue(0)
            currentClockCycles = 456
            return
        }
        val line = lcdcY.getValue()
        /*if(coincidenceInterrupt && line == lyCompare.getValue()) {
            interruptManager.fireLCDC()
        }*/

        if(line < VBlankStartLine) {
            mode = when {
                currentClockCycles >= 80 -> {
                    if(mode != VideoMode.Mode2 && mode2OamInterrupt) {
                        interruptManager.fireLcdStat()
                    }
                    VideoMode.Mode2
                }
                currentClockCycles >= 172 + 80 -> VideoMode.Mode3
                else -> {
                    if(mode != VideoMode.HBlank && mode0HBlankInterrupt) {
                        interruptManager.fireLcdStat()
                    }
                    VideoMode.HBlank
                }
            }
        } else {
            if(mode != VideoMode.VBlank) {
                interruptManager.fireVBlank()

                if(mode1VBlankInterrupt)
                    interruptManager.fireLcdStat()
            }
            mode = VideoMode.VBlank
        }

        if(currentClockCycles >= 456) {
            currentClockCycles %= 456
            if(line < VBlankStartLine) {
                scanLine()
            }

            lcdcY.setValue(line+1)

            if(line >= 154) {
                lcdcY.setValue(0)
            }
        }
        lcdStatus.setValue(lcdStatus.getValue().setBits(mode.ordinal, 0..1))

        coincidenceFlag = lyCompare.getValue() == lcdcY.getValue()
    }

    fun drawLogo() {
        for(line in 0 until VBlankStartLine) {
            for(x in 0 until 32) {
                val offset = (line/8)*32 + x
                drawTileRow(x * 8, line, line%8, 0x104 + offset, bgPaletteData)
            }
        }
    }
}