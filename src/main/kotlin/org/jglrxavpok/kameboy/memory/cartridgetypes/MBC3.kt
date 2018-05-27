package org.jglrxavpok.kameboy.memory.cartridgetypes

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.Battery
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.specialRegs.DHCounterRegister
import org.jglrxavpok.kameboy.memory.specialRegs.DLCounterRegister

class MBC3(val cartridge: Cartridge, val battery: Battery): CartridgeType() {
    override val name = "MBC3"
    private var enabled = true
    private var latchStart = false
    var mode = Mode.Ram
    var currentBank = 1
    val BankSize = 0x4000
    val banks = Array(cartridge.romBankCount) { index ->
        val start = index*BankSize
        val end = start+BankSize
        cartridge.rawData.sliceArray(start until end)
    }
    private var milliseconds = 0
    private val seconds = Register("RTC S")
    private val minutes = Register("RTC M")
    private val hours = Register("RTC H")
    private val upperDayCounter = DHCounterRegister()
    private val lowerDayCounter = DLCounterRegister(upperDayCounter)
    private var selectedClockRegister = 0
    private var lastTime = System.currentTimeMillis()

    init {
        battery.loadRAM(cartridge)
    }

    override fun tick(cycles: Int) {
        super.tick(cycles)
        if(upperDayCounter.timerHalt) {
            lastTime = System.currentTimeMillis()
        }
    }

    override fun write(address: Int, value: Int) {
        when(address) {
            in 0x2000..0x3FFF -> {
                currentBank = if(value == 0x00)
                    0x01
                else
                    value
            }
            in 0x4000..0x5FFF -> {
                if(value in 0..3) {
                    cartridge.selectedRAMBankIndex = value and 0b11
                    mode = Mode.Ram
                } else if(value in 0x08..0x0C) {
                    mode = Mode.Clock
                    selectedClockRegister = value
                }
            }
            in 0x6000..0x7FFF -> {
                when(value) {
                    0x00 -> latchStart = true
                    0x01 -> if(latchStart) {
                        latchClockData()
                        latchStart = false
                    }
                    else -> latchStart = false
                }
            }
            in 0x0000..0x1FFF -> {
                val enable = value and 0xF == 0b1010
                if(enabled && !enable && mode == Mode.Ram) {
                    battery.saveRAM(cartridge)
                }
                enabled = enable
            }
            in 0xA000..0xBFFF -> {
                when (mode) {
                    Mode.Clock -> {
                        if(enabled)
                            writeRTC(address-0xA000, value)
                    }
                    Mode.Ram -> {
                        if(enabled)
                            cartridge.currentRAMBank.write(address, value)
                    }
                }
            }
        }
    }

    private fun latchClockData() {
        val time = (System.currentTimeMillis()-lastTime)/1000
        var newMilliseconds = milliseconds + (System.currentTimeMillis()-lastTime) % 1000
        var newSeconds = seconds.getValue() + (time % 60)
        var newMinutes = minutes.getValue() + ((time / 60) % 60)
        var newHours = hours.getValue() + ((time / 3600) % 60)
        var newDays = ((upperDayCounter.getValue() and 0x1) shl 8) + lowerDayCounter.getValue() + ((time / 3600 / 24) % 512)
        while(newMilliseconds >= 1000) {
            newMilliseconds -= 1000
            newSeconds++
        }
        while(newSeconds >= 60) {
            newSeconds -= 60
            newMinutes++
        }
        while(newMinutes >= 60) {
            newMinutes -= 60
            newHours++
        }
        while(newHours >= 24) {
            newHours -= 24
            newDays++
        }
        seconds.setValue(newSeconds.toInt())
        minutes.setValue(newMinutes.toInt())
        hours.setValue(newHours.toInt())
        lowerDayCounter.setValue(newDays.toInt() and 0xFF)
        if(newDays > 0xFF) {
            upperDayCounter.setValue(upperDayCounter.getValue().setBits(1, 0..0))
            if(newDays > 0x1FF) {
                upperDayCounter.setValue(upperDayCounter.getValue().setBits(0, 0..0))
                upperDayCounter.setValue(upperDayCounter.getValue().setBits(1, 7..7))
            }
        }
        milliseconds = newMilliseconds.toInt()
        lastTime = System.currentTimeMillis()
    }

    override fun read(address: Int): Int {
        return when(address) {
            in 0x0000..0x3FFF -> {
                banks[0][address].asUnsigned()
            }
            in 0x4000..0x7FFF -> {
                val bank = banks[currentBank]
                bank[address-0x4000].asUnsigned()
            }
            in 0xA000..0xBFFF -> {
                if(!enabled)
                    return 0xFF
                return when (mode) {
                    Mode.Clock -> {
                        readRTC(address)
                    }
                    Mode.Ram -> {
                        cartridge.currentRAMBank.read(address)
                    }
                }
            }
            else -> 0xFF
        }
    }

    private fun writeRTC(addr: Int, value: Int) {
        when(selectedClockRegister) {
            0x08 -> seconds.setValue(value)
            0x09 -> minutes.setValue(value)
            0x0A -> hours.setValue(value)
            0x0B -> lowerDayCounter.setValue(value)
            0x0C -> upperDayCounter.setValue(value)
        }
    }

    private fun readRTC(addr: Int): Int {
        return when(selectedClockRegister) {
            0x08 -> seconds.getValue()
            0x09 -> minutes.getValue()
            0x0A -> hours.getValue()
            0x0B -> lowerDayCounter.getValue()
            0x0C -> upperDayCounter.getValue()
            else -> 0xFF
        }
    }

    override fun accepts(address: Int): Boolean {
        return address in 0..0x8000 || address in 0xA000..0xBFFF
    }

    enum class Mode {
        Ram,
        Clock
    }

    override fun toString(): String {
        return "MBC3(CurrentBank=$currentBank, Battery=$battery)"
    }
}