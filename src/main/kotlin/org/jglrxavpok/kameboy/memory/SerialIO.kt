package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.helpful.toBit
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.network.guest.GuestSession
import org.jglrxavpok.kameboy.network.host.Server

class SerialIO(val interruptManager: InterruptManager, val memoryMapper: MemoryMapper, outputToConsole: Boolean) {

    // TODO: 3 steps process
    // send 1 bit from master
    // receive 1 bit from slave
    // confirm transaction
    // repeat...

    private var currentCycle = 0
    private var data = 0x00
    private var transferring = false
    val connectedPeripherals = mutableListOf<SerialPeripheral>()

    private val frequency = 8192

    private val controlRegister = MemoryRegister("SC", memoryMapper, 0xFF02)
    val hasInternalClock get() = controlRegister.getValue() and 1 != 0//by controlRegister.bitVar(0)
    private val fastClock by controlRegister.bitVar(1)

    private var bitsSent = 0

    // for synchronisation
    var waitingForConfirmation = false
        private set

    init {
        if(outputToConsole)
            connectedPeripherals += ConsoleOutputPeripheral
    }

    fun step(cycles: Int) {
        if(!transferring || !hasInternalClock)
            return
        val gameboy = memoryMapper.gameboy
        val speedMultiplier = when {
            !gameboy.isCGB -> 1
            !fastClock -> memoryMapper.currentSpeedFactor
            fastClock -> memoryMapper.currentSpeedFactor * 32
            else -> 1
        }
        if(!waitingForConfirmation) {
            currentCycle += cycles

            val period = (frequency * speedMultiplier/* /8*/).toClockCycles()
            while(currentCycle >= period) {
               currentCycle -= period

                sendSingleBit()
                bitsSent++
            }

        }
    }

    private fun sendSingleBit(overridedMasterFlag: Boolean? = null) {
        val type = if(hasInternalClock) "Master" else "Slave"
        val bit = data and 0b1000_0000 != 0
        //println(">> ($type) Sending $bit")
        connectedPeripherals.forEach { it.transfer(overridedMasterFlag ?: hasInternalClock, bit) }

        val isConnected = connectedPeripherals.filterNot { it === ConsoleOutputPeripheral || !it.isConnected }.isNotEmpty()
        waitingForConfirmation = true // wait for slave answer

        if(!isConnected) {
            waitingForConfirmation = false
       //     println("not connected, not waiting for confirmation")
            data = 0xFF // don't receive anything as we're not connected :c
        }
    }

    private fun sendTransferEnd() {
        transferFinish()
        connectedPeripherals.forEach { it.transferFinish() }
    }

    fun transfer(value: Int) {
        data = value
    }

    fun startTransfer() {
        if (!transferring) {
            transferring = true
        //    currentCycle = 0
        }
    }

    fun receive(bit: Boolean, fromMaster: Boolean) {
        /*if(fromMaster) {
            if(hasInternalClock) {
                println("Forced to switch from Master to Slave")
            }
            val sc = memoryMapper.read(0xFF02)
            memoryMapper.write(0xFF02, sc.setBits(0, 0..0)) // we are now using an external clock
        }*/
        if(hasInternalClock) { // if master, send confirmation and end bit transfer
            if(bitsSent >= 8) {
                sendTransferEnd()
                bitsSent = 0
            } else {
                sendConfirmation()
            }
        }
        if(!hasInternalClock || fromMaster) {
            // transfer happening!
            val sc = memoryMapper.read(0xFF02)
            memoryMapper.write(0xFF02, sc.setBits(1, 7..7))
            sendSingleBit(overridedMasterFlag = false) // send slave bit
        }
        data = (data shl 1) or bit.toBit()
        data = data and 0xFF
    }

    fun sendConfirmation() {
        confirmTransfer()
        if(Server.isRunning())
            Server.confirmTransfer()
        else if(GuestSession.isRunning())
            GuestSession.confirmTransfer()
    }

    fun transferFinish() {
        confirmTransfer()

        val type = if(hasInternalClock) "Master" else "Slave"
       // println(">> ($type) Transfer finished")

        transferring = false
        interruptManager.fireSerialIOTransferComplete()

        // transfer finished
        val sc = memoryMapper.read(0xFF02)
       // memoryMapper.write(0xFF02, sc.setBits(0, 7..7))
        memoryMapper.write(0xFF02, hasInternalClock.toBit())

        // TODO: test - set to external clock (test)
    //    memoryMapper.write(0xFF02, sc.setBits(0, 0..0))
    }

    fun confirmTransfer() {
        val type = if(hasInternalClock) "Master" else "Slave"
        //println(">> ($type) Bit transfer confirmed")

        waitingForConfirmation = false
    }

    fun readFromTransfer(): Int {
        val isConnected = connectedPeripherals.filterNot { it === ConsoleOutputPeripheral || !it.isConnected }.isNotEmpty()
        if(!isConnected)
            return 0xFF
        return data
    }
}

interface SerialPeripheral {
    val isConnected: Boolean
    fun transfer(fromMaster: Boolean, bit: Boolean)
    fun transferFinish() {}
}

object ConsoleOutputPeripheral: SerialPeripheral {
    override val isConnected = true

    private var bitsSent = 0
    private var byte = 0

    override fun transfer(fromMaster: Boolean, bit: Boolean) {
        byte = ((byte shl 1) or (if(bit) 1  else 0)) and 0xFF
        bitsSent++
        if(bitsSent >= 8) {
            print(byte.toChar())
            bitsSent = 0
        }
    }
}