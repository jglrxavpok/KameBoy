package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.AsciiString
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.fromNibbles
import org.jglrxavpok.kameboy.memory.cartridgetypes.*
import org.jglrxavpok.kameboy.time.SaveStateElement
import java.io.File

open class Cartridge(val rawData: ByteArray, val bootROM: ByteArray? = null, val saveFile: File? = null): MemoryComponent {
    val hasBootRom = bootROM != null
    val bootRomComponent = RomWrapper(bootROM ?: ByteArray(0))
    open val scrollingLogo = rawData.sliceArray(0x0104..0x0133)
    open val title = AsciiString(rawData.sliceArray(0x0134..0x0142))
    open val isOnlyForColorGB = rawData[0x0143].asUnsigned() == 0xC0
    open val isForColorGB = rawData[0x0143].asUnsigned() == 0x80 || isOnlyForColorGB
    open val licenseCode = fromNibbles(rawData[0x0144], rawData[0x0145])
    open val superGameBoyIndicator = rawData[0x0146]
    open val cartridgeTypeIndex = rawData[0x0147]
    open val romSizeIndex = rawData[0x0148]
    open val ramSizeIndex = rawData[0x0149]
    open val isJapanese = rawData[0x014A].asUnsigned() == 0
    open val oldLicenseCode = rawData[0x14B]

    open val usesSGBFunctions = oldLicenseCode.asUnsigned() == 0x33 && superGameBoyIndicator.asUnsigned() == 0x03

    open val maskROMVersion = rawData[0x14C]
    open val complementCheck = rawData[0x14D]
    open val checksum = fromNibbles(rawData[0x14E], rawData[0x14F])
    val romBankCount get()= when(romSizeIndex.asUnsigned()) {
     /*   0 -> 0
        1 -> 1
        2 -> 2
        3 -> 4
        4 -> 8
        5 -> 16
        6 -> 32
        7 -> 64*/
        0 -> 2
        1 -> 4
        2 -> 8
        3 -> 16
        4 -> 32
        5 -> 64
        6 -> 128
        7 -> 256
        8 -> 512
        0x52 -> 72
        0x53 -> 80
        0x54 -> 96
        else -> error("Invalid rom size index $romSizeIndex")
    }
    val ramBankCount get()= when(ramSizeIndex.asUnsigned()) {
        0 -> 0
        1, 2 -> 1
        3 -> 4
        4 -> 16
        else -> error("Unknown ram size index $ramSizeIndex")
    }

    @SaveStateElement
    val ramBanks = Array(ramBankCount) { index ->
        RamBank("Switchable Ram Bank #$index")
    }
    @SaveStateElement
    var selectedRAMBankIndex = 0
    val currentRAMBank get()= ramBanks[selectedRAMBankIndex % ramBanks.size]

    val cartrigeType = cartrigeTypeFromIndex(cartridgeTypeIndex)

    override val name = "Cartridge (${cartrigeType.name})"

    fun cartrigeTypeFromIndex(index: Byte): CartridgeType = when(index.asUnsigned()) {
        0 -> ROMOnly(this)
        1, 2 -> MBC1(this, NoBattery)
        3 -> MBC1(this, getSaveFileBattery())

        5 -> MBC2(this, NoBattery)
        6 -> MBC2(this, getSaveFileBattery())


        0x0F -> MBC3(this, getSaveFileBattery())
        0x10 -> MBC3(this, getSaveFileBattery())
        0x11 -> MBC3(this, NoBattery)
        0x12 -> MBC3(this, NoBattery)
        0x13 -> MBC3(this, getSaveFileBattery())

        0x19 -> MBC5(this, NoBattery, NoRumble)
        0x1A -> MBC5(this, NoBattery, NoRumble)
        0x1B -> MBC5(this, getSaveFileBattery(), NoRumble)
        0x1C -> MBC5(this, NoBattery, ControllerRumble)
        0x1D -> MBC5(this, NoBattery, ControllerRumble)
        0x1E -> MBC5(this, getSaveFileBattery(), ControllerRumble)

        else -> error("Cartridge type $index not supported")
    }

    private fun getSaveFileBattery(): FileBasedBattery {
        val file = saveFile ?: File("$title.sav")
        return FileBasedBattery(file)
    }

    override fun write(address: Int, value: Int) {
        cartrigeType.write(address, value)
    }

    override fun read(address: Int) = cartrigeType.read(address)

    override fun toString(): String {
        return "Cartridge[\n" +
                "Title: '$title'\n" +
                "Cartridge type infos: $cartrigeType\n" +
                "only for Color: $isOnlyForColorGB\n" +
                "compatible with Gameboy Color: $isForColorGB\n"+
                "]"
    }
}
