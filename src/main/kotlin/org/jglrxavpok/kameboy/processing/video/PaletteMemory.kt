package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent

class PaletteMemory(name: String): MemoryComponent {

    private val internalData = IntArray(0x40 * 2) { 0 }

    override val name = "$name Palette Memory"

    fun getColorAt(startAddress: Int): Long {
        val low = read(startAddress)
        val high = read(startAddress+1)
        val redIntensity = low and 0b11111
        val greenIntensity = (low shr 5) or (high and 0b11)
        val blueIntensity = high shr 3
        val rgb =
                (((redIntensity shl 16) / 32f) * 255).toInt()
                + (((greenIntensity shl 8) / 32f) * 255).toInt()
                + (((blueIntensity shl 0) / 32f) * 255).toInt()
        return 0xFF000000 + rgb
    }

    override fun write(address: Int, value: Int) {
        internalData[address] = value
    }

    override fun read(address: Int): Int {
        return internalData[address]
    }
}