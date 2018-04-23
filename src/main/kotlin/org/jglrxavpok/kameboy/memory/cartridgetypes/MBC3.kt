package org.jglrxavpok.kameboy.memory.cartridgetypes

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.memory.Battery
import org.jglrxavpok.kameboy.memory.Cartridge

/**
 * TODO: better implementation of RTC
 */
class MBC3(val cartridge: Cartridge, val battery: Battery): CartridgeType() {
    override val name = "MBC3"
    private var enabled = true
    var mode = Mode.Ram
    var currentBank = 1
    val BankSize = 0x4000
    val banks = Array(cartridge.romBankCount) { index ->
        val start = index*BankSize
        val end = start+BankSize
        cartridge.rawData.sliceArray(start until end)
    }

    init {
        battery.loadRAM(cartridge)
    }

    private var selectedClockRegister = 0
    private var rtcHaltStatus = 0
    private var startTime = (System.currentTimeMillis()/1000).toInt()

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
                // TODO
            }
            in 0x0000..0x1FFF -> {
                val enable = value and 0xF == 0b1010
                if(enabled && !enable && mode==Mode.Ram) {
                    battery.saveRAM(cartridge)
                }
                enabled = enable
            }
            in 0xA000..0xBFFF -> {
                when (mode) {
                    Mode.Clock -> {
                        writeRTC(address-0xA000, value)
                    }
                    Mode.Ram -> {
                        if(enabled)
                            cartridge.currentRAMBank.write(address, value)
                    }
                }
//                error("Invalid address for MBC3 $address")
            }
        }
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
            0x0C -> {
                rtcHaltStatus = value and (0b0100_0000)
                if(value and 0b1000_0000 == 0) {
                    startTime = (System.currentTimeMillis()/1000).toInt()
                }
            }
        }
    }

    private fun readRTC(addr: Int): Int {
        val timeInSeconds = (System.currentTimeMillis()/1000).toInt()
        val dayCountSinceFirstRead = (timeInSeconds-startTime)/60/60/24
        val dayCounterCarry = dayCountSinceFirstRead > 0b1_1111_1111
        return when(selectedClockRegister) {
            0x08 -> timeInSeconds % 60
            0x09 -> timeInSeconds/60 % 60
            0x0A -> timeInSeconds/60/60 % 24
            0x0B -> dayCountSinceFirstRead.asUnsigned8()
            0x0C -> ((dayCountSinceFirstRead shr 8) and 1) or (rtcHaltStatus shl 6) or ((if(dayCounterCarry) 1 else 0) shl 7)
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