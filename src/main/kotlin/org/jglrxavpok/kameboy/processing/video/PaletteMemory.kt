package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering

class PaletteMemory(name: String): MemoryComponent {

    private val internalData = IntArray(64) { 0 }

    override val name = "$name Palette Memory"

    fun getColorAt(startAddress: Int): Long {
        val low = read(startAddress)
        val high = read(startAddress+1)

        // SameBoy
        val curve = intArrayOf(
                0,2,4,7,12,18,25,34,42,52,62,73,85,97,109,121,134,146,158,170,182,193,203,213,221,230,237,243,248,251,253,255
        )
        val blueIntensity = low and 0x1F
        val greenIntensity = (((low shr 5) and 0b111) or ((high and 0b11) shl 3)) and 0x1F
        val redIntensity = (high shr 2) and 0x1F


        // val rgb = (red shl 16) or (green shl 8) or (blue shl 0)
        val rgb = when(Config[Rendering.CGBColorCurve]) {
            CGBColorCurves.SameboyCurve -> {
                ((curve[redIntensity] shl 16) or
                        (curve[greenIntensity] shl 8) or
                        curve[blueIntensity]).toLong()
            }

            CGBColorCurves.Linear -> {
                val blue = ((blueIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF
                val green = ((greenIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF
                val red = ((redIntensity * 255 / 0x1F.toFloat())).toLong() and 0xFF

                (red shl 16) or
                        (green shl 8) or
                        blue
            }
        }
        return rgb
    }

    override fun write(address: Int, value: Int) {
        internalData[address] = value
    }

    override fun read(address: Int): Int {
        return internalData[address]
    }

    enum class CGBColorCurves {
        Linear, SameboyCurve
    }
}