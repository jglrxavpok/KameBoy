package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import java.util.*

class Video(val gameboy: Gameboy) {

    companion object {
        val BackgroundTileMapSize = 32*32
        val VBlankStartLine = 144
    }

    val memory: MemoryMapper = gameboy.mapper
    val interruptManager: InterruptManager = gameboy.interruptManager

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
    val bgIndex = IntArray(256*256)
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
    var mode2OamInterrupt by lcdStatus.bitVar(5)
    var mode1VBlankInterrupt by lcdStatus.bitVar(4)
    var mode0HBlankInterrupt by lcdStatus.bitVar(3)
    var coincidenceFlag by lcdStatus.bitVar(2)
    val modeFlag get() = lcdStatus.getValue() and 0b11

    var currentClockCycles = 0
    var mode: VideoMode = VideoMode.VBlank

    var dmgPalette: ColorPalette = DefaultPalette

    val cgbBgPalettes = Array<ColorPalette>(32) { paletteIndex ->
        { index: Int ->
            memory.backgroundPaletteMemory.getColorAt(index*2+paletteIndex*8)
        }
    }

    val cgbSpritePalettes = Array<ColorPalette>(32) { paletteIndex ->
        { index: Int ->
            memory.spritePaletteMemory.getColorAt(index*2 + paletteIndex*8)
        }
    }

    enum class VideoMode(val durationInCycles: Int) {
        HBlank(200),
        VBlank(100000),
        Mode2(80),
        Mode3(170)
    }

    fun drawTileRow(x: Int, row: Int, tileLocalRow: Int, tileAddress: Int, palette: ColorPalette,
                    target: IntArray = pixelData,
                    isBackground: Boolean = false,
                    vMirror: Boolean = false,
                    hMirror: Boolean = false,
                    tileHeight: Int = 8,
                    backgroundPriority: Boolean = false,
                    inVram1: Boolean = false, // should fetch tile in VRAM bank 1 ?
                    bgOamPriority: Boolean = false /* TODO: unused for now*/) {
        var screenY = row
        if(isBackground) { // background wraps
            screenY = wrapInBounds(row)
        } else if(screenY >= 256 || screenY < 0) {
            return
        }
        val effectiveTileRow = if(vMirror) tileHeight-1-tileLocalRow else tileLocalRow
        val mem = if(inVram1) memory.vram1 else memory
        val lineDataLS = mem.read(tileAddress + effectiveTileRow*2)
        val lineDataMS = mem.read(tileAddress + effectiveTileRow*2 +1)
        for(i in 0..7) {
            var screenX = x + i
            if(isBackground) { // background wraps
                screenX = wrapInBounds(screenX)
            } else if(screenX >= 256 || screenX < 0) {
                continue
            }
            if(!(gameboy.inCGBMode && bgDisplay)) { // CGB with bit 0 of LCDC cleared puts priority to sprites
                if(backgroundPriority && isBackgroundColorWithPriority(bgIndex[screenY*256+screenX])) {
                    continue
                }
            }
            val effectiveTileColumn = if(hMirror) i else (7-i)
            val highColor = if(lineDataMS and (1 shl effectiveTileColumn) != 0) 1 else 0
            val lowColor = if(lineDataLS and (1 shl effectiveTileColumn) != 0) 1 else 0
            val pixelColorIndex = (highColor shl 1) + lowColor

            if(pixelColorIndex == 0 && !isBackground) // transparent pixel
                continue
            if(isBackground) {
                bgIndex[screenY*256+screenX] = pixelColorIndex
            }
            val color = pixelColor(pixelColorIndex, palette)
            target[screenY*256+screenX] = color
        }
    }

    private fun isBackgroundColorWithPriority(index: Int): Boolean {
        return index in 1..3
    }

    private tailrec fun wrapInBounds(value: Int): Int {
        if(value >= 0)
            return value % 256
        return wrapInBounds(value+256)
    }

    private fun pixelColor(index: Int, palette: ColorPalette): Int {
        return palette(index).toInt()
    }

    val WIDTH = 256
    val HEIGHT = 256

    fun scanLine() {
        val line = lcdcY.getValue()

        Arrays.fill(pixelData, line*WIDTH, (line+1)*WIDTH, dmgPalette(0).toInt())

        if(line < VBlankStartLine && lcdDisplayEnable) {
            val shouldDisplayBackground = when {
                !gameboy.isCGB -> bgDisplay
                else -> {
                    if(gameboy.inCGBMode) {
                        true
                    } else {
                        bgDisplay
                    }
                }
            }
            if(shouldDisplayBackground) {
                val scrolledY = wrapInBounds(line + scrollY.getValue())
                for(x in 0 until 32) {
                    val scrolledX = wrapInBounds(x * 8 + scrollX.getValue())
                    if(gameboy.inCGBMode) {
                        val tileNumber = memory.read(backgroundTileMapAddress + scrolledY/8 *32 + scrolledX/8)
                        val attribs = memory.vram1.read(backgroundTileMapAddress + scrolledY/8 *32 + scrolledX/8)
                        val tileAddress = tileDataAddress
                        val offset = (if(dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                        drawTileRow(x*8-scrolledX%8, line, scrolledY %8, tileAddress + offset,
                                cgbBgPalettes[attribs and 0b111],
                                isBackground = true,
                                inVram1 = attribs and (1 shl 3) != 0,
                                hMirror = attribs and (1 shl 5) != 0,
                                vMirror = attribs and (1 shl 6) != 0,
                                bgOamPriority = attribs and (1 shl 7) != 0)
                    } else {
                        val tileNumber = memory.read(backgroundTileMapAddress + scrolledY/8 *32 + scrolledX/8)
                        val tileAddress = tileDataAddress
                        val offset = (if(dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                        drawTileRow(x*8-scrolledX%8, line, scrolledY %8, tileAddress + offset, this::bgPalette, isBackground = true)
                    }
                }
            }
            val overrideWindow = gameboy.isCGB && !gameboy.inCGBMode && bgDisplay
            if(windowDisplayEnable && !overrideWindow) {
                val effectiveLine = line-windowY.getValue()
                if(effectiveLine in 0..255) {
                    for(x in 0 until 32) {
                        val effectiveX = x*8-windowX.getValue()+7
                        if(gameboy.inCGBMode) {
                            val tileNumber = memory.read(windowTileMapAddress + effectiveLine/8 *32 + effectiveX/8)
                            val attribs = memory.vram1.read(windowTileMapAddress + effectiveLine/8 *32 + effectiveX/8)
                            val tileAddress = tileDataAddress
                            val offset = (if(dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                            drawTileRow(effectiveX, line, effectiveLine %8, tileAddress + offset,
                                    cgbBgPalettes[attribs and 0b111],
                                    isBackground = true,
                                    inVram1 = attribs and (1 shl 3) != 0,
                                    hMirror = attribs and (1 shl 5) != 0,
                                    vMirror = attribs and (1 shl 6) != 0,
                                    bgOamPriority = attribs and (1 shl 7) != 0)
                        } else {
                            val tileNumber = memory.read(windowTileMapAddress + effectiveLine/8 *32 + effectiveX/8)
                            val tileAddress = tileDataAddress
                            val offset = (if(dataSelect) tileNumber else tileNumber.asSigned8()) * 0x10
                            drawTileRow(effectiveX, line, effectiveLine %8, tileAddress + offset, this::bgPalette, isBackground = true)
                        }
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
                            val palette = when {
                                gameboy.inCGBMode -> cgbSpritePalettes[sprite.cgbPaletteNumber]
                                else -> {
                                    if(sprite.dmgPaletteNumber) this::objPalette1 else this::objPalette0
                                }
                            }
                            val posY = sprite.positionY.getValue()-8-1
                            val posX = sprite.positionX.getValue()-8
                            val tileNumber = sprite.tileNumber.getValue()
                            val offset = tileNumber.asUnsigned8()
                            val tileAddress = 0x8000 + offset*2*8

                            if(spriteSizeSelect) {
                                if(posY+8 in line..(line+15)) {
                                    drawTileRow(posX, line, posY+8-line, tileAddress, palette, hMirror = sprite.hMirror, vMirror = !sprite.vMirror, tileHeight = 16, backgroundPriority = sprite.priority, inVram1 = sprite.inVram1 && gameboy.inCGBMode)
                                }
                            } else {
                                if(posY in line..(line+7)) {
                                    drawTileRow(posX, line, posY-line, tileAddress, palette, hMirror = sprite.hMirror, vMirror = !sprite.vMirror, backgroundPriority = sprite.priority, inVram1 = sprite.inVram1 && gameboy.inCGBMode)
                                }
                            }
                }
            }
        }
    }

    fun bgPalette(index: Int): Long {
        val data = bgPaletteData.getValue() and (0b11 shl (index*2)) shr (index*2)
        return when(data) {
            in 0..3 -> dmgPalette(data)
            else -> error("This is impossible!")
        }
    }

    private fun objPalette1(index: Int): Long {
        val data = objPalette1Data.getValue() and (0b11 shl (index*2)) shr (index*2)
        return when(data) {
            in 0..3 -> dmgPalette(data)
            else -> error("This is impossible!")
        }
    }

    private fun objPalette0(index: Int): Long {
        val data = objPalette0Data.getValue() and (0b11 shl (index*2)) shr (index*2)
        return when(data) {
            in 0..3 -> dmgPalette(data)
            else -> error("This is impossible!")
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
}