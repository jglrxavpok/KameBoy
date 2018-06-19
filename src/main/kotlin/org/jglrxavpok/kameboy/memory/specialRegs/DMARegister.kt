package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register

class DMARegister(val memoryMapper: MemoryMapper): Register("DMA") {

    override fun write(address: Int, value: Int) {
        // DMA Transfer
        val startAddress = value * 0x100
        val length = 0xA0
        for(index in 0 until length) {
            val data = memoryMapper.read(startAddress + index)
            memoryMapper.write(0xFE00 + index, data)
        }
    //    println("transfer from $startAddress")
    }

    override fun read(address: Int): Int {
        return 0xFF // read only
    }
}