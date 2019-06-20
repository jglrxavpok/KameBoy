package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.RAM
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering

class PaletteMemory(name: String): RAM(64) {

    companion object {
        val SameBoyCurve = intArrayOf(
                0,2,4,7,12,18,25,34,42,52,62,73,85,97,109,121,134,146,158,170,182,193,203,213,221,230,237,243,248,251,253,255
        )
    }

    init {
        data.fill(0)
    }

    override fun correctAddress(address: Int) = address

    override val name = "$name Palette Memory"

    fun getColorAt(startAddress: Int): Long {
        val low = read(startAddress)
        val high = read(startAddress+1)

        // SameBoy
        val blueIntensity = low and 0x1F
        val greenIntensity = (((low shr 5) and 0b111) or ((high and 0b11) shl 3)) and 0x1F
        val redIntensity = (high shr 2) and 0x1F


        // val rgb = (red shl 16) or (green shl 8) or (blue shl 0)
        val rgb = when(Config[Rendering.CGBColorCurve]) {

            // from https://github.com/LIJI32/SameBoy/blob/master/Core/display.c
            CGBColorCurves.SameboyCurve -> {
                ((SameBoyCurve[redIntensity] shl 16) or
                        (SameBoyCurve[greenIntensity] shl 8) or
                        SameBoyCurve[blueIntensity]).toLong()
            }

            CGBColorCurves.Linear -> {
                val blue = ((blueIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF
                val green = ((greenIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF
                val red = ((redIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF

                (red shl 16) or
                        (green shl 8) or
                        blue
            }

            // from https://github.com/TASVideos/BizHawk/blob/master/BizHawk.Emulation.Cores/Consoles/Nintendo/Gameboy/GBColors.cs (MIT License)
            CGBColorCurves.Gambatte -> {
                val red = (redIntensity * 13 + greenIntensity * 2 + blueIntensity) shr 1
                val green = (greenIntensity * 3 + blueIntensity) shl 1
                val blue = (redIntensity * 3 + greenIntensity * 2 + blueIntensity * 11) shr 1
                ((red shl 16) or
                        (green shl 8) or
                        blue).toLong()
            }
        }
        return rgb
    }

    enum class CGBColorCurves {
        Linear, SameboyCurve, Gambatte;

    }
}