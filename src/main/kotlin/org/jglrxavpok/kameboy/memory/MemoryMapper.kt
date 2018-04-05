package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.specialRegs.*
import org.jglrxavpok.kameboy.processing.SpriteAttributeTable
class MemoryMapper(val cartridgeData: Cartridge, val input: PlayerInput): MemoryComponent {

    companion object {
        val ExecutionEntryPoint = 0x100
        val NintendoLogo = 0x104
    }

    override val name = "Memory mapper (internal)"

    val interruptManager = InterruptManager(this)

    val interruptEnableRegister = Register("Interrupt Enable Register")
    val lyRegister = LYRegister()
    val divRegister = DivRegister()
    val timerRegister = TimerRegister(this)
    val ioPorts = arrayOf(
            P1Register(input),
            Register("SB"),
            Register("SC"),
            divRegister,
            timerRegister,
            Register("TMA"),
            Register("TAC"),
            Register("Unknown FF08"),
            Register("Unknown FF09"),
            Register("Unknown FF0A"),
            Register("Unknown FF0B"),
            Register("Unknown FF0C"),
            Register("Unknown FF0D"),
            Register("Unknown FF0E"),
            Register("IF"),
            Register("NR 10"),
            Register("NR 11"),
            Register("NR 12"),
            Register("NR 13"),
            Register("NR 14"),
            Register("Unknown FF15"),
            Register("NR 21"),
            Register("NR 22"),
            Register("NR 23"),
            Register("NR 24"),
            Register("NR 30"),
            Register("NR 31"),
            Register("NR 32"),
            Register("NR 33"),
            Register("NR 34"),
            Register("Unknown FF1F"),
            Register("NR 40"),
            Register("NR 41"),
            Register("NR 42"),
            Register("NR 43"),
            Register("NR 44"),
            Register("NR 50"),
            Register("NR 51"),
            Register("NR 52"),
            Register("Unknown FF27"),
            Register("Unknown FF28"),
            Register("Unknown FF29"),
            Register("Unknown FF2A"),
            Register("Unknown FF2B"),
            Register("Unknown FF2C"),
            Register("Unknown FF2D"),
            Register("Unknown FF2E"),
            Register("Unknown FF2F"),
            Register("Wave Pattern RAM 0"),
            Register("Wave Pattern RAM 1"),
            Register("Wave Pattern RAM 2"),
            Register("Wave Pattern RAM 3"),
            Register("Wave Pattern RAM 4"),
            Register("Wave Pattern RAM 5"),
            Register("Wave Pattern RAM 6"),
            Register("Wave Pattern RAM 7"),
            Register("Wave Pattern RAM 8"),
            Register("Wave Pattern RAM 9"),
            Register("Wave Pattern RAM A"),
            Register("Wave Pattern RAM B"),
            Register("Wave Pattern RAM C"),
            Register("Wave Pattern RAM D"),
            Register("Wave Pattern RAM E"),
            Register("Wave Pattern RAM F"),
            Register("LCDC"),
            Register("STAT"),
            Register("SCY"),
            Register("SCX"),
            lyRegister,
            Register("LYC"),
            DMARegister(this),
            Register("BGP"),
            Register("OBP0"),
            Register("OBP1"),
            Register("WY"),
            Register("WX")
    )

    val spriteAttributeTable = SpriteAttributeTable()
    val empty0 = object: RAM(0xFF00-0xFEA0) {
        override val name = "Empty0"

        override fun correctAddress(address: Int): Int {
            return address-0xFEA0
        }
    }
    val empty1 = object: RAM(0xFF80-0xFF4C) {
        override val name = "Empty1"

        override fun correctAddress(address: Int): Int {
            return address-0xFF4C
        }
    }
    val internalRAM = object: RAM(8*1024) {
        override val name = "Internal RAM"

        override fun correctAddress(address: Int): Int {
            if(address >= 0xE000) // handle echo of internal RAM
                return address-0xE000
            return address-0xC000
        }
    }
    val highRAM = object: RAM(0xFFFE-0xFF80 +1) {
        override val name = "High RAM"

        override fun correctAddress(address: Int): Int {
            return address-0xFF80
        }
    }
    val videoRAM = object: RAM(8*1024) {
        override val name = "Video RAM"

        override fun correctAddress(address: Int): Int {
            return address-0x8000
        }
    }

    fun map(address: Int): MemoryComponent = when(address.asAddress()) {
        in 0 until 0x8000 -> cartridgeData
        in 0x8000 until 0xA000 -> videoRAM
        in 0xA000 until 0xC000 -> cartridgeData.currentRAMBank
        in 0xC000..0xDFFF, in 0xE000..0xFDFF -> internalRAM
        in 0xFE00 until 0xFEA0 -> spriteAttributeTable
        in 0xFEA0 until 0xFF00 -> empty0
        in 0xFF00 until 0xFF4C -> ioPorts[address-0xFF00]
        in 0xFF4C until 0xFF80 -> empty1
        in 0xFF80 until 0xFFFF -> highRAM
        0xFFFF -> interruptEnableRegister

        else -> error("Invalid address ${Integer.toHexString(address)}")
    }

    override fun write(address: Int, value: Int) {
        map(address).write(address, value)
    }
    override fun read(address: Int) = map(address).read(address)
}

