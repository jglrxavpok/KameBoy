package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister

class Video(val memory: MemoryMapper, val interruptManager: InterruptManager) {

    companion object {
        val BackgroundTileMapSize = 32*32
        val VBlankStartLine = 144
    }
    val windowX = MemoryRegister("WindowX", memory, 0xFF4B)
    val windowY = MemoryRegister("WindowY", memory, 0xFF4A)
    val objPalette1Data = MemoryRegister("OBJ Palette 0 Data", memory, 0xFF49)
    val objPalette0Data = MemoryRegister("OBJ Palette 0 Data", memory, 0xFF48)
    val bgPaletteData = MemoryRegister("BG Palette Data", memory, 0xFF47)
    // TODO: CGB palettes + VRAM bank
    val lyCompare = MemoryRegister("LYC", memory, 0xFF45)
    val lcdcY = memory.lyRegister
    val scrollX = MemoryRegister("ScrollX", memory, 0xFF43)
    val scrollY = MemoryRegister("ScrollY", memory, 0xFF42)
    val windowTileMapAddress get()= if(windowTileMapSelect) 0x9C00 else 0x9800
    val backgroundTileMapAddress get()= if(bgTileMapSelect) 0x9C00 else 0x9800
    val tileDataAddress get()= if(dataSelect) 0x8000 else 0x8800
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
    val coincidenceFlag by lcdStatus.bitVar(2)
    val modeFlag get() = lcdStatus.getValue() and 0b11

    fun drawTileRow(x: Int, row: Int, tileAddress: Int, palette: MemoryRegister) {
        val isBackground = palette.address == bgPaletteData.address
        var screenY = row
        if(isBackground) { // background wraps
            screenY %= 256
        } else if(screenY >= 256) {
            return
        }
        val lineDataLS = memory.read(tileAddress + row*2)
        val lineDataMS = memory.read(tileAddress + row*2 +1)
        for(i in 0..7) {
            var screenX = x + i
            if(isBackground) { // background wraps
                screenX %= 256
            } else if(screenX >= 256) {
                break
            }
            val highColor = if(lineDataMS and (1 shl i) != 0) 1 else 0
            val lowColor = if(lineDataLS and (1 shl i) != 0) 1 else 0
            val pixelColorIndex = (highColor shl 1) + lowColor
            if(pixelColorIndex == 0 && !isBackground) // transparent pixel
                continue
            pixelData[screenY+screenX*256] = pixelColor(pixelColorIndex, palette)
        }
    }

    private fun pixelColor(index: Int, palette: MemoryRegister): Int {
        val data = palette.getValue() and (0b11 shl (index*2)) shr (index*2)
        return when(data) {
            0 -> 0xFF000000.toInt()
            1 -> 0xFF555555.toInt()
            2 -> 0xFFAAAAAA.toInt()
            3 -> 0xFFFFFFFF.toInt()
            else -> error("This is impossible!")
        }
    }

    fun scanLine() {
        val line = lcdcY.getValue()
        if(line == VBlankStartLine) {
            if(mode1VBlankInterrupt)
                interruptManager.fireVBlank()
        }
        if(line == lyCompare.getValue()) {
            if(coincidenceInterrupt)
                interruptManager.fireLCDC()
        }

        if(line < VBlankStartLine) {
            if(bgDisplay) {
                for(x in 0 until 32) {
                    val tileNumber = memory.read(backgroundTileMapAddress + (line/32)*32 + x)
                    println(tileNumber)
                    val tileAddress = tileDataAddress
                    val offset = if(bgTileMapSelect) tileNumber else tileNumber.asSigned8()
                    drawTileRow(x * 32, line, tileAddress + offset, bgPaletteData)
                }
            }
            if(windowDisplayEnable) {
                for(x in 0 until 32) {
                    val tileNumber = memory.read(windowTileMapAddress + line/32 + x)
                    val tileAddress = windowTileMapAddress
                    val offset = if(windowTileMapSelect) tileNumber else tileNumber.asSigned8()
                    drawTileRow(x * 32, line, tileAddress + offset, bgPaletteData)
                }
            }

            // TODO: Sprites
        }
        lcdcY.setValue(line+1)
    }
}