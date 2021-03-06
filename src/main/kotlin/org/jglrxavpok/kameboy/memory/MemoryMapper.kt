package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.memory.specialRegs.*
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*
import org.jglrxavpok.kameboy.memory.specialRegs.video.CgbPaletteData
import org.jglrxavpok.kameboy.memory.specialRegs.video.CgbPaletteIndex
import org.jglrxavpok.kameboy.memory.specialRegs.video.Hdma5
import org.jglrxavpok.kameboy.memory.specialRegs.video.VramSelect
import org.jglrxavpok.kameboy.processing.video.PaletteMemory
import org.jglrxavpok.kameboy.sound.Sound
import org.jglrxavpok.kameboy.processing.video.SpriteAttributeTable
import org.jglrxavpok.kameboy.processing.video.Video
import org.jglrxavpok.kameboy.time.SaveStateElement

/**
 * TODO: Decouple sound/interrupts/gbc registers from MMU
 */
class MemoryMapper(val gameboy: Gameboy): MemoryComponent {

    override val name = "Memory mapper (internal)"

    private var booting = gameboy.cartridge.hasBootRom

    val interruptManager = InterruptManager(this)
    val sound = Sound(this)

    @SaveStateElement
    val interruptEnableRegister = Register("Interrupt Enable Register")
    @SaveStateElement
    val lyRegister = LYRegister(this)
    @SaveStateElement
    val divRegister = DivRegister()
    @SaveStateElement
    val timerRegister = TimerRegister(this)

    val serialIO = SerialIO(interruptManager, this, gameboy.outputSerial)
    @SaveStateElement
    val serialControlReg = SerialControllerRegister(serialIO)
    @SaveStateElement
    val serialDataReg = SerialDataRegister(serialIO, serialControlReg)

    @SaveStateElement
    var currentSpeedFactor: Int = 1
    @SaveStateElement
    val speedRegister = SpeedRegister(this)
    val ioPorts = arrayOf(
            P1Register(gameboy.input),
            serialDataReg,
            serialControlReg,
            UnknownRegister(0xFF03),
            divRegister,
            timerRegister,
            Register("TMA"),
            TacRegister(gameboy),
            UnknownRegister(0xFF08),
            UnknownRegister(0xFF09),
            UnknownRegister(0xFF0A),
            UnknownRegister(0xFF0B),
            UnknownRegister(0xFF0C),
            UnknownRegister(0xFF0D),
            UnknownRegister(0xFF0E),
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

    @SaveStateElement
    val spriteAttributeTable = SpriteAttributeTable()
    @SaveStateElement
    val empty0 = object: RAM(0xFF00-0xFEA0) {
        override val name = "Empty0"

        override fun correctAddress(address: Int): Int {
            return address-0xFEA0
        }
    }
    @SaveStateElement
    val empty1 = object: RAM(0xFF80-0xFF4C) {
        override val name = "Empty1"

        override fun correctAddress(address: Int): Int {
            return address-0xFF4C
        }
    }
    @SaveStateElement
    val internalRAM = object: RAM(8*1024) {
        override val name = "Internal RAM"

        override fun correctAddress(address: Int): Int {
            if(address >= 0xE000) // handle echo of internal RAM
                return address-0xE000
            return address-0xC000
        }
    }
    @SaveStateElement
    val highRAM = object: RAM(0xFFFE-0xFF80 +1) {
        override val name = "High RAM"

        override fun correctAddress(address: Int): Int {
            return address-0xFF80
        }
    }
    @SaveStateElement
    val vram0 = object: RAM(8*1024) {
        override val name = "Video RAM #0"

        override fun correctAddress(address: Int): Int {
            return address-0x8000
        }
    }
    @SaveStateElement
    val vram1 = object: RAM(8*1024) {
        override val name = "Video RAM #1"

        override fun correctAddress(address: Int): Int {
            return address-0x8000
        }
    }
    val bootRegister = object : MemoryComponent {
        override val name = "Boot register"

        override fun write(address: Int, value: Int) {
            if(value and 0x1 == 0x1)
                booting = false
        }

        override fun read(address: Int): Int {
            return 0xFF
        }
    }
    @SaveStateElement
    val wavePatternRam = WavePatternRam(sound)
    @SaveStateElement
    val wramBanks = Array<RAM>(8) { index ->
        object: RAM(0x4000) {
            override val name: String
                get() = "WRAM Bank #$index"

            override fun correctAddress(address: Int): Int {
                if(index == 0)
                    return address-0xC000
                return address-0xD000
            }
        }
    }
    val wramBankSelect = WramBankSelectRegister()
    val infraredRegister = InfraredRegister()

    @SaveStateElement
    val backgroundPaletteMemory = PaletteMemory("Background")
    @SaveStateElement
    val spritePaletteMemory = PaletteMemory("Sprite")

    val backgroundPaletteIndex = CgbPaletteIndex("Background Palette Index")
    @SaveStateElement
    val backgroundPaletteData = CgbPaletteData("Background Palette Data", backgroundPaletteIndex, backgroundPaletteMemory)

    val spritePaletteIndex = CgbPaletteIndex("Sprite Palette Index")
    @SaveStateElement
    val spritePaletteData = CgbPaletteData("Sprite Palette Data", spritePaletteIndex, spritePaletteMemory)

    val vramSelect = VramSelect()

    val hdma1 = Register("HDMA 1")
    val hdma2 = Register("HDMA 2")
    val hdma3 = Register("HDMA 3")
    val hdma4 = Register("HDMA 4")
    val hdma5 = Hdma5(this)

    fun step(cycles: Int) {
        hdma5.step(cycles)
    }

    fun map(address: Int): MemoryComponent {
        if(gameboy.inCGBMode) {
            val comp = when(address.asAddress()) {
                // GBC registers
                0xFF4D -> speedRegister
                0xFF56 -> infraredRegister
                0xFF70 -> wramBankSelect
                0xFF68 -> backgroundPaletteIndex
                0xFF69 -> when(gameboy.video.mode) { Video.VideoMode.Mode3 -> UnaccessibleMemory
                    else -> backgroundPaletteData
                }
                0xFF6A -> spritePaletteIndex
                0xFF6B -> when(gameboy.video.mode) { Video.VideoMode.Mode3 -> UnaccessibleMemory
                    else -> spritePaletteData
                }
                0xFF4F -> vramSelect
                0xFF51 -> hdma1
                0xFF52 -> hdma2
                0xFF53 -> hdma3
                0xFF54 -> hdma4
                0xFF55 -> hdma5
                in 0x8000 until 0xA000 ->
                    if(gameboy.video.mode == Video.VideoMode.Mode3) {
                        UnaccessibleMemory
                    } else {
                        if(vramSelect[0]) vram1 else vram0
                    }
                else -> this
            }
            if(comp != this)
                return comp
        }
        return when(address.asAddress()) {
        // DMG registers
            in 0 until 0x8000 -> {
                if(booting && gameboy.cartridge.hasBootRom && address in 0 until gameboy.cartridge.bootROM!!.size && address !in 0x100..0x1FF) {
                    gameboy.cartridge.bootRomComponent
                } else {
                    gameboy.cartridge
                }
            }
            in 0x8000 until 0xA000 -> {
                if(gameboy.video.mode == Video.VideoMode.Mode3) {
                    UnaccessibleMemory
                } else {
                    vram0
                }
            }
            in 0xA000 until 0xC000 -> {
                if(gameboy.cartridge.cartrigeType.accepts(address)) {
                    gameboy.cartridge.cartrigeType
                } else {
                    gameboy.cartridge.currentRAMBank
                }
            }
            in 0xC000..0xCFFF, in 0xE000..0xEFFF -> {
                if(gameboy.inCGBMode) {
                    wramBanks[0]
                } else {
                    internalRAM
                }
            }
            in 0xD000..0xDFFF, in 0xF000..0xFDFF -> {
                when {
                    gameboy.inCGBMode -> wramBanks[wramBankSelect.getValue()]
                    else -> internalRAM
                }
            }
            in 0xFE00 until 0xFEA0 -> when(gameboy.video.mode) {
                Video.VideoMode.Mode2, Video.VideoMode.Mode3 -> UnaccessibleMemory
                else -> spriteAttributeTable
            }
            in 0xFEA0 until 0xFF00 -> empty0
            in 0xFF30..0xFF3F -> wavePatternRam
            in 0xFF00 until 0xFF4C -> ioPorts[address-0xFF00]
            in 0xFF4C until 0xFF80 -> {
                when {
                    booting && address == 0xFF50 -> bootRegister
                    else -> empty1
                }
            }
            in 0xFF80 until 0xFFFF -> highRAM
            0xFFFF -> interruptEnableRegister

            else -> error("Invalid address ${Integer.toHexString(address)}")
        }
    }

    override fun write(address: Int, value: Int) {
        map(address).write(address, value)
    }
    override fun read(address: Int) = map(address).read(address)
}
