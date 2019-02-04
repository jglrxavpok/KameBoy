package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.time.SaveStateElement

class DMARegister(val memoryMapper: MemoryMapper): Register("DMA") {

    private val length = 0xA0
    @SaveStateElement
    private var index = 0
    @SaveStateElement
    private var startAddress = 0
    @SaveStateElement
    private var clockCycles = 0
    @SaveStateElement
    private var transferring = false

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        // DMA Transfer
        index = 0
        transferring = true
        clockCycles = 0
        startAddress = value * 0x100
    //    println("transfer from $startAddress")
    }

    fun step(clockCycles: Int) {
        if(!transferring)
            return
        this.clockCycles += clockCycles
        while(this.clockCycles >= 4) {
            this.clockCycles -= 4
            // copy one byte
            val data = memoryMapper.read(startAddress + index)
            memoryMapper.write(0xFE00 + index, data)
            index++
            if(index >= length) {
                transferring = false
                index = 0
            }
        }
    }
}