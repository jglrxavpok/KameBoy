package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent

class PaletteMemory(name: String): MemoryComponent {

    private val internalData = IntArray(64) { 0 }

    override val name = "$name Palette Memory"

    fun getColorAt(startAddress: Int): Long {
        val low = read(startAddress)
        val high = read(startAddress+1)
        val blueIntensity = low and 0x1F
        val greenIntensity = (((low shr 5) and 0b111) or ((high and 0b11) shl 3)) and 0x1F
        val redIntensity = (high shr 2) and 0x1F
        val blue = ((redIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF
        val green = ((greenIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF
        val red = ((blueIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF

        val r = ((redIntensity*13+greenIntensity*2+blueIntensity*1)) and 0xFF
        val g = (greenIntensity*3+blueIntensity)
        val b = (redIntensity * 3 + greenIntensity*2 + blueIntensity*11)
/*        val rgb = (r shl 16) or
                (g shl 9) or
                (b shr 1)*/

        // SameBoy
        val curve = intArrayOf(
                0,2,4,7,12,18,25,34,42,52,62,73,85,97,109,121,134,146,158,170,182,193,203,213,221,230,237,243,248,251,253,255
        )
        /*val rgb = (redIntensity shl 3) or (redIntensity shr 2) or
                (greenIntensity shl 3) or (greenIntensity shr 2) or
                (blueIntensity shl 3) or (blueIntensity shr 2)*/
        val rgb = (curve[redIntensity] shl 16) or
                (curve[greenIntensity] shl 8) or
                curve[blueIntensity]
        // val rgb = (red shl 16) or (green shl 8) or (blue shl 0)
        return rgb.toLong()
    }

    override fun write(address: Int, value: Int) {
        internalData[address] = value
    }

    override fun read(address: Int): Int {
        return internalData[address]
    }
}