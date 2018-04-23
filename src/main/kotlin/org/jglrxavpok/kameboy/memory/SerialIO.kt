package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.toClockCycles

class SerialIO(val interruptManager: InterruptManager, val memoryMapper: MemoryMapper, val outputToConsole: Boolean) {

    private var currentCycle = 0
    private var byteToTransfer = 0xFF
    private var transferring = false

    private val speed = 8192.toClockCycles()

    fun step(cycles: Int) {
        if(!transferring)
            return
        currentCycle += cycles
        if(currentCycle >= speed) {
            currentCycle %= speed
            // reset transfer flag
            val readValue = memoryMapper.read(0xFF02)
            memoryMapper.write(0xFF02, readValue and 0b0111_1111)

            if(outputToConsole)
                print(byteToTransfer.toChar())

            // fire interrupt
            transferring = false
            interruptManager.fireSerialIOTransferComplete()
        }
    }

    fun transfer(value: Int) {
        byteToTransfer = value
    }

    fun startTransfer() {
        transferring = true
        currentCycle = 0
    }
}