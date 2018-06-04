package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.network.guest.GuestSession
import org.jglrxavpok.kameboy.network.host.Server

class SerialIO(val interruptManager: InterruptManager, val memoryMapper: MemoryMapper, outputToConsole: Boolean) {

    private var currentCycle = 0
    private var data = 0
    private var transferring = false
    val connectedPeripherals = mutableListOf<SerialPeripheral>()

    private val frequency = 8192

    private val controlRegister = MemoryRegister("SC", memoryMapper, 0xFF02)
    val hasInternalClock by controlRegister.bitVar(0)
    private val fastClock by controlRegister.bitVar(1)

    // for synchronisation
    private var newlyReceived = 0

    init {
        if(outputToConsole)
            connectedPeripherals += ConsoleOutputPeripheral
    }

    fun step(cycles: Int) {
        if(!transferring || !hasInternalClock)
            return
        currentCycle += cycles
        val gameboy = memoryMapper.gameboy
        val speedMultiplier = when {
            !gameboy.isCGB -> 1
            !fastClock -> memoryMapper.currentSpeedFactor
            fastClock -> memoryMapper.currentSpeedFactor * 32
            else -> 1
        }
        val period = (frequency * speedMultiplier /8).toClockCycles()
        if(currentCycle >= period) {
            currentCycle %= period

            actualTransfer()

            transferring = false
        }
    }

    private fun actualTransfer() {
        val type = if(hasInternalClock) "Master" else "Slave"
        println(">> ($type) Sent $data")
        connectedPeripherals.forEach { it.transfer(data) }
    }

    fun transfer(value: Int) {
        data = value
    }

    fun startTransfer() {
        if (!transferring) {
            transferring = true
            currentCycle = 0
        }
    }

    fun receive(value: Int) {
        newlyReceived = value
        if(hasInternalClock) {
            sendConfirmation()
            confirmTransfer()
        } else {
            actualTransfer()
        }
    }

    private fun sendConfirmation() {
        if(Server.isRunning())
            Server.confirmTransfer()
        else if(GuestSession.isRunning())
            GuestSession.confirmTransfer()
    }

    fun confirmTransfer() {
        data = newlyReceived

        // reset transfer flag
        val sc = memoryMapper.read(0xFF02)
        memoryMapper.write(0xFF02, sc.setBits(0, 7..7))

        interruptManager.fireSerialIOTransferComplete()

        val type = if(hasInternalClock) "Master" else "Slave"
        println(">> ($type) Confirming transfer")
    }

    fun readFromTransfer(): Int {
        val isConnected = connectedPeripherals.filterNot { it === ConsoleOutputPeripheral }.isNotEmpty()
        return when {
            isConnected -> data
            !hasInternalClock -> 0x0
            else -> 0xFF
        }
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