package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.helpful.setBits

class ROMOnly(val cartridge: Cartridge): MemoryComponent {
    override val name = "ROM Only"
    private val data = cartridge.rawData

    override fun write(address: Int, value: Int) {
       // data[address] = value.asUnsigned8().toByte()
        //error("WRITE TO ROM to $address ; value is $value")
    }

    override fun read(address: Int) = data[address].asUnsigned()

    override fun toString(): String {
        return "ROM Only"
    }
}

class MBC1(val cartridge: Cartridge): MemoryComponent {
    override val name = "MBC1"
    var mode = Mode.Rom16Ram8
    var currentBank = 0
    val BankSize = 0x4000
    val banks = Array(cartridge.romBankCount+1) { index ->
        val start = index*BankSize
        val end = start+BankSize
        cartridge.rawData.sliceArray(start until end)
    }

    override fun write(address: Int, value: Int) {
        when(address) {
            in 0x6000..0x7FFF -> {
                mode = if(value == 1) Mode.Rom4Ram32 else Mode.Rom16Ram8
            }
            in 0x2000..0x3FFF -> {
                val bank = value and 0b00011111
                // set lower 5 bits
                currentBank = currentBank.setBits(if(bank == 0)
                    1
                else
                    bank, 0..4)
            }
            in 0x4000..0x5FFF -> {
                when(mode) {
                    Mode.Rom16Ram8 -> currentBank = currentBank.setBits((value and 0xF), 5..6)
                    Mode.Rom4Ram32 -> cartridge.selectedRAMBankIndex = value and 0b11
                }
            }
            in 0x0000..0x1FFF -> {
                cartridge.currentRAMBank.enabled = value and 0xF == 0xA
            }
            else -> error("Invalid address for MBC1 $address")
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
            else -> error("Invalid read address for MBC1 $address")
        }
    }

    enum class Mode {
        Rom16Ram8,
        Rom4Ram32
    }

    override fun toString(): String {
        return "MBC1(CurrentBank=$currentBank)"
    }
}