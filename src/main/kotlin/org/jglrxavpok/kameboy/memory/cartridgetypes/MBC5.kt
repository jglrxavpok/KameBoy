package org.jglrxavpok.kameboy.memory.cartridgetypes

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.Battery
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.NoRumble
import org.jglrxavpok.kameboy.memory.Rumble
import org.jglrxavpok.kameboy.time.SaveStateElement

class MBC5(val cartridge: Cartridge, val battery: Battery, val rumble: Rumble): CartridgeType() {
    override val name = "MBC5"
    @SaveStateElement
    var mode = Mode.Rom16Ram8
    var currentBank = 1
    val BankSize = 0x4000
    val banks = Array(cartridge.romBankCount) { index ->
        val start = index*BankSize
        val end = start+BankSize
        cartridge.rawData.sliceArray(start until end)
    }
    @SaveStateElement
    private var ramWriteEnabled = true
    init {
        battery.loadRAM(cartridge)
    }

    override fun write(address: Int, value: Int) {
        when(address) {
            in 0x6000..0x7FFF -> {
                mode = if(value == 1) Mode.Rom4Ram32 else Mode.Rom16Ram8
            }
            in 0x2000..0x2FFF -> {
                // set lower 5 bits
                currentBank = value
            }
            in 0x3000..0x3FFF -> {
                currentBank = currentBank.setBits((value and 0b1), 8..8)
            }
            in 0x4000..0x5FFF -> {
                if(rumble != NoRumble) {
                    val motorOn = value and 0b1000 != 0
                    if(motorOn)
                        rumble.turnMotorOn()
                    else
                        rumble.turnMotorOff()
                    cartridge.selectedRAMBankIndex = value and 0b111
                } else {
                    cartridge.selectedRAMBankIndex = value and 0xF
                }
            }
            in 0x0000..0x1FFF -> {
                val enable = value and 0xF == 0b1010
                if(ramWriteEnabled && !enable) {
                    battery.saveRAM(cartridge)
                }
                ramWriteEnabled = enable
            }
            in 0xA000..0xBFFF -> {
                if(cartridge.ramBankCount == 0)
                    return
                if(ramWriteEnabled) {
                    cartridge.currentRAMBank.write(address, value)
                }
            }
            else -> error("Invalid address for MBC1 $address")
        }
    }

    override fun accepts(address: Int): Boolean {
        return address in 0..0x8000 || address in 0xA000..0xBFFF
    }

    override fun read(address: Int): Int {
        return when(address) {
            in 0x0000..0x3FFF -> {
                banks[0][address].asUnsigned()
            }
            in 0x4000..0x7FFF -> {
                if(currentBank >= banks.size)
                    return 0xFF
                val bank = banks[currentBank]
                bank[address-0x4000].asUnsigned()
            }
            in 0xA000..0xBFFF -> {
                if(cartridge.ramBankCount == 0)
                    return 0xFF
                return cartridge.currentRAMBank.read(address)
            }
            else -> 0xFF//error("Invalid read address for MBC1 $address")
        }
    }

    enum class Mode {
        Rom16Ram8,
        Rom4Ram32
    }

    override fun toString(): String {
        return "MBC5(CurrentBank=$currentBank, Battery=$battery)"
    }
}