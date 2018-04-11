package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.specialRegs.*
import org.jglrxavpok.kameboy.memory.specialRegs.sound.NRRegister
import org.jglrxavpok.kameboy.memory.specialRegs.sound.SoundRegister
import org.jglrxavpok.kameboy.sound.Sound
import org.jglrxavpok.kameboy.processing.SpriteAttributeTable
class MemoryMapper(val cartridgeData: Cartridge, val input: PlayerInput): MemoryComponent {

    companion object {
        val ExecutionEntryPoint = 0x100
        val NintendoLogo = 0x104
    }

    override val name = "Memory mapper (internal)"

    val interruptManager = InterruptManager(this)
    val sound = Sound(this)

    val interruptEnableRegister = Register("Interrupt Enable Register")
    val lyRegister = LYRegister(this)
    val divRegister = DivRegister()
    val timerRegister = TimerRegister(this)
    val ioPorts = arrayOf(
            P1Register(input),
            Register("SB"),
            Register("SC"),
            Register("Unknown FF03"),
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
            NRRegister(1,0, 0x80),
            sound.channel1Attributes,
            NRRegister(1, 2, 0x00),
            NRRegister(1, 3, 0xFF),
            NRRegister(1, 4, 0xBF),
            SoundRegister(0xFF15),
            sound.channel2Attributes,
            NRRegister(2, 2, 0x00),
            NRRegister(2, 3, 0xFF),
            NRRegister(2, 4, 0xBF),
            NRRegister(3, 0, 0x7F),
            sound.channel3SoundLengthRaw,
            NRRegister(3, 2, 0x9F),
            NRRegister(3, 3, 0xFF),
            NRRegister(3, 4, 0xBF),
            SoundRegister(0xFF1F),
            sound.channel4SoundLengthReg,
            NRRegister(4, 2, 0x00),
            NRRegister(4, 3, 0x00),
            NRRegister(4, 4, 0xBF),
            sound.channelControl,
            sound.outputSelect,
            sound.soundToggle,
            SoundRegister(0xFF27),
            SoundRegister(0xFF28),
            SoundRegister(0xFF29),
            SoundRegister(0xFF2A),
            SoundRegister(0xFF2B),
            SoundRegister(0xFF2C),
            SoundRegister(0xFF2D),
            SoundRegister(0xFF2E),
            SoundRegister(0xFF2F),
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
        in 0xA000 until 0xC000 -> {
            if(cartridgeData.cartrigeType.accepts(address)) {
                cartridgeData.cartrigeType
            } else {
                cartridgeData.currentRAMBank
            }
        }
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

