package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent

class PaletteMemory(name: String): MemoryComponent {

    private val internalData = IntArray(64) { 0 }

    override val name = "$name Palette Memory"

    fun getColorAt(startAddress: Int): Long {
        val low = read(startAddress)
        val high = read(startAddress+1)
        val redIntensity = low and 0x1F
        val greenIntensity = (((low shr 5) and 0b111) or ((high and 0b11) shl 3)) and 0x1F
        val blueIntensity = (high shr 3) and 0x1F
        val red = ((redIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF
        val green = ((greenIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF
        val blue = ((blueIntensity / 0x1F.toFloat()) * 255).toLong() and 0xFF
        return 0xFF000000 or (blue shl 16) or (green shl 8) or (red shl 0)
    }

    override fun write(address: Int, value: Int) {
        internalData[address] = value
    }

    override fun read(address: Int): Int {
        return internalData[address]
    }
}