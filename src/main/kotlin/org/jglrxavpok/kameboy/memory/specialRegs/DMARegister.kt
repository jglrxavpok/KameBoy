package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register

class DMARegister(val memoryMapper: MemoryMapper): Register("DMA") {

    override fun setValue(value: Int) {
        // DMA Transfer
        val address = value * 0x100
        val length = 0xA0
        for(index in 0 until length) {
            val data = memoryMapper.read(address + index)
            memoryMapper.write(0xFE00 + index, data)
        }
    //    println("transfer from $address")
    }

    override fun getValue(): Int {
        return 0xFF // read only
    }
}