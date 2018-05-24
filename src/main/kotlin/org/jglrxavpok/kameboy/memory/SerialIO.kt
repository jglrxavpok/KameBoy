package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.toClockCycles

class SerialIO(val interruptManager: InterruptManager, val memoryMapper: MemoryMapper, outputToConsole: Boolean) {

    private var currentCycle = 0
    private var byteToTransfer = 0xFF
    private var transferring = false
    private var received = 0xFF
    val connectedPeripherals = mutableListOf<SerialPeripheral>()

    private val speed = 8192.toClockCycles()

    private val controlRegister = MemoryRegister("SC", memoryMapper, 0xFF02)
    private val isInternalClock by controlRegister.bitVar(0)

    init {
        if(outputToConsole)
            connectedPeripherals += ConsoleOutputPeripheral
    }

    var last = System.currentTimeMillis()

    fun step(cycles: Int) {
        if(System.currentTimeMillis()-last > 1000) {
            println(">> is internal clock: $isInternalClock")
            last = System.currentTimeMillis()
        }
        if(!transferring || !isInternalClock)
            return
        currentCycle += cycles
        if(currentCycle >= speed) {
            currentCycle %= speed
            // reset transfer flag
            val readValue = memoryMapper.read(0xFF02)
            memoryMapper.write(0xFF02, readValue and 0b0111_1111)

            connectedPeripherals.forEach { it.transfer(byteToTransfer) }

            transferring = false
        }
    }

    fun transfer(value: Int) {
        byteToTransfer = value
    }

    fun startTransfer() {
        transferring = true
        currentCycle = 0
    }

    fun receive(value: Int) {
        received = value
        if(!isInternalClock /*&& transferring*/) {
            val readValue = memoryMapper.read(0xFF02)
            memoryMapper.write(0xFF02, readValue and 0b0111_1111)

            connectedPeripherals.forEach { it.transfer(byteToTransfer) }

            transferring = false
           // interruptManager.fireSerialIOTransferComplete()
        }
        interruptManager.fireSerialIOTransferComplete()
    }

    fun readFromTransfer(): Int {
        val isConnected = connectedPeripherals.filterNot { it === ConsoleOutputPeripheral }.isNotEmpty()
        return if(isConnected) received else 0xFF
    }
}

interface SerialPeripheral {
    fun transfer(byte: Int)
}

object ConsoleOutputPeripheral: SerialPeripheral {
    override fun transfer(byte: Int) {
        print(byte.toChar())
    }
}