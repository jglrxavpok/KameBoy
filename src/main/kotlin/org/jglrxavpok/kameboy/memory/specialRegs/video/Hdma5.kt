package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register

/**
 * TODO: H-Blank DMA
 */
class Hdma5(val memory: MemoryMapper) : Register("HDMA 5") {

    override fun write(address: Int, value: Int) {
        val length = value and 0b0111_1111
        val sourceHigh = memory.hdma1.getValue()
        val sourceLow = memory.hdma2.getValue() and 0b1111_1000

        val destinationHigh = memory.hdma3.getValue() and 0b0001_1111
        val destinationLow = memory.hdma4.getValue() and 0b1111_1000
        val source = (sourceHigh shl 8) + sourceLow
        val destination = (destinationHigh shl 8) + destinationLow

        // Copy all at once
        for(index in 0 until length) {
            memory.write(destination+index, memory.read(source+index))
        }
    }

    override fun read(address: Int): Int {
        return 0xFF // TODO: all transfers are immediate for the moment
    }
}