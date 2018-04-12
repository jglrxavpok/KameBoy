package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.hz2cycles
import org.jglrxavpok.kameboy.helpful.setBits

class SerialIO(val interruptManager: InterruptManager, val memoryMapper: MemoryMapper) {

    private var currentCycle = 0
    private var byteToTransfer = 0xFF

    private val speed = 8192.hz2cycles
    private var bitIndex = 0

    fun step(cycles: Int) {
        if(memoryMapper.read(0xFF02) and (1 shl 7) != 0) {
            currentCycle += cycles
            if(currentCycle > speed) {
                currentCycle %= speed
                bitIndex++
                if(bitIndex == 8) {
                    // reset transfer flag
                    val readValue = memoryMapper.read(0xFF02)
                    memoryMapper.write(0xFF02, readValue or readValue.setBits(0, 7..7))

                    print(byteToTransfer.toChar())

                    // fire interrupt
                    interruptManager.fireSerialIOTransferComplete()
                }
            }
        } else {
            currentCycle = 0
            bitIndex = 0
        }
    }

    fun transfer(value: Int) {
        byteToTransfer = value
        bitIndex = 0
    }
}