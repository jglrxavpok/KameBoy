package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.specialRegs.*
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*
import org.jglrxavpok.kameboy.sound.Sound
import org.jglrxavpok.kameboy.processing.video.SpriteAttributeTable

/**
 * TODO: Decouple sound/interrupts/gbc registers from MMU
 */
class MemoryMapper(val cartridgeData: Cartridge, val input: PlayerInput, val outputSerial: Boolean = false): MemoryComponent {

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
    val serialIO = SerialIO(interruptManager, this, outputSerial)
    val serialControlReg = SerialControllerRegister(serialIO)
    val serialDataReg = SerialDataRegister(serialIO, serialControlReg)

    var currentSpeedFactor: Int = 1
    val speedRegister = SpeedRegister(this)
    val ioPorts = arrayOf(
            P1Register(input),
            serialDataReg,
            serialControlReg,
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
            IFRegister(),
            NRRegister(1,0, 0x80, sound),
            NRx1(sound.channel1, 0x3F, sound),
            NRRegister(1, 2, 0x00, sound),
            NRRegister(1, 3, 0xFF, sound),
            NRRegister(1, 4, 0xBF, sound),
            SoundRegister(0xFF15, sound),
            NRx1(sound.channel2, 0x3F, sound),
            NRRegister(2, 2, 0x00, sound),
            NRRegister(2, 3, 0xFF, sound),
            NRRegister(2, 4, 0xBF, sound),
            NRRegister(3, 0, 0x7F, sound),
            NRx1(sound.channel3, 0xFF, sound),
            NRRegister(3, 2, 0x9F, sound),
            NRRegister(3, 3, 0xFF, sound),
            NRRegister(3, 4, 0xBF, sound),
            SoundRegister(0xFF1F, sound),
            NRx1(sound.channel4, 0xFF, sound),
            NRRegister(4, 2, 0x00, sound),
            NRRegister(4, 3, 0x00, sound),
            NRRegister(4, 4, 0xBF, sound),
            sound.channelControl,
            sound.outputSelect,
            sound.soundToggle,
            SoundRegister(0xFF27, sound),
            SoundRegister(0xFF28, sound),
            SoundRegister(0xFF29, sound),
            SoundRegister(0xFF2A, sound),
            SoundRegister(0xFF2B, sound),
            SoundRegister(0xFF2C, sound),
            SoundRegister(0xFF2D, sound),
            SoundRegister(0xFF2E, sound),
            SoundRegister(0xFF2F, sound),
            MemoryRegister("Wave Pattern RAM 0", this, 0xFF30),
            MemoryRegister("Wave Pattern RAM 1", this, 0xFF31),
            MemoryRegister("Wave Pattern RAM 2", this, 0xFF32),
            MemoryRegister("Wave Pattern RAM 3", this, 0xFF33),
            MemoryRegister("Wave Pattern RAM 4", this, 0xFF34),
            MemoryRegister("Wave Pattern RAM 5", this, 0xFF35),
            MemoryRegister("Wave Pattern RAM 6", this, 0xFF36),
            MemoryRegister("Wave Pattern RAM 7", this, 0xFF37),
            MemoryRegister("Wave Pattern RAM 8", this, 0xFF38),
            MemoryRegister("Wave Pattern RAM 9", this, 0xFF39),
            MemoryRegister("Wave Pattern RAM A", this, 0xFF3A),
            MemoryRegister("Wave Pattern RAM B", this, 0xFF3B),
            MemoryRegister("Wave Pattern RAM C", this, 0xFF3C),
            MemoryRegister("Wave Pattern RAM D", this, 0xFF3D),
            MemoryRegister("Wave Pattern RAM E", this, 0xFF3E),
            MemoryRegister("Wave Pattern RAM F", this, 0xFF3F),
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
    val bootRegister = OrOnReadRegister("BOOT register", 0xFF)
    val wavePatternRam = WavePatternRam(sound)

    fun map(address: Int): MemoryComponent = when(address.asAddress()) {
        // GBC registers
        0xFF4D -> speedRegister
        // DMG registers
        in 0 until 0x8000 -> {
            if(cartridgeData.hasBootRom && address in 0..0xFF && read(0xFF50) != 0x1) {
                cartridgeData.bootRomComponent
            } else {
                if(address == 0xFF50) {
                    bootRegister
                } else {
                    cartridgeData
                }
            }
        }
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
        in 0xFF30..0xFF3F -> wavePatternRam
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

