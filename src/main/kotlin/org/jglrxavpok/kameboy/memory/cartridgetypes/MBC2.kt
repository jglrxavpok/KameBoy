package org.jglrxavpok.kameboy.memory.cartridgetypes

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.Battery
import org.jglrxavpok.kameboy.memory.Cartridge

class MBC2(val cartridge: Cartridge, val battery: Battery): CartridgeType<MBC2.SaveStateData>() {
    override val name = "MBC2"
    var currentBank = 1
    val BankSize = 0x4000
    val banks = Array(cartridge.romBankCount) { index ->
        val start = index*BankSize
        val end = start+BankSize
        cartridge.rawData.sliceArray(start until end)
    }
    private var ramWriteEnabled = true
    init {
        battery.loadRAM(cartridge)
    }

    override fun write(address: Int, value: Int) {
        when(address) {
            in 0x2000..0x3FFF -> {
                val bank = value and 0b00011111
                // set lower 5 bits
                currentBank = if(bank == 0) 1 else currentBank.setBits(bank, 0..4)
            }
            in 0x0000..0x1FFF -> {
                val enable = value and 0xF == 0b1010
                if(value and (1 shl 4) != 0)
                    return
                if(ramWriteEnabled && !enable) {
                    battery.saveRAM(cartridge)
                }
                ramWriteEnabled = enable
            }
            in 0xA000..0xBFFF -> {
                if(cartridge.ramBankCount == 0)
                    return
                if(ramWriteEnabled) {
                    cartridge.ramBanks[0].write(address, value)
                }
            }
        }
    }

    override fun accepts(address: Int): Boolean {
        return address in 0..0x8000 || address in 0xA000..0xA1FF
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
                return cartridge.ramBanks[0].read(address)
            }
            else -> 0xFF
        }
    }

    override fun toString(): String {
        return "MBC2(CurrentBank=$currentBank, Battery=$battery)"
    }

    override fun createSaveStateData() = SaveStateData(currentBank, ramWriteEnabled)

    override fun internalLoadSaveStateData(data: SaveStateData) {
        currentBank = data.currentBank
        ramWriteEnabled = data.ramWriteEnabled
    }

    data class SaveStateData(val currentBank: Int, val ramWriteEnabled: Boolean)
}