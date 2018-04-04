package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.AsciiString
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.fromNibbles

class Cartridge(val rawData: ByteArray): MemoryComponent {
    val scrollingLogo = rawData.sliceArray(0x0104..0x0133)
    val title = AsciiString(rawData.sliceArray(0x0134..0x0142))
    val isForColorGB = rawData[0x0143].asUnsigned() == 0x80
    val licenseCode = fromNibbles(rawData[0x0144], rawData[0x0145])
    val superGameBoyIndicator = rawData[0x0146]
    val cartridgeTypeIndex = rawData[0x0147]
    val romSizeIndex = rawData[0x0148]
    val ramSizeIndex = rawData[0x0149]
    val isJapanese = rawData[0x014A].asUnsigned() == 0
    val oldLicenseCode = rawData[0x14B]
    val maskROMVersion = rawData[0x14C]
    val complementCheck = rawData[0x14D]
    val checksum = fromNibbles(rawData[0x14E], rawData[0x14F])
    val cartrigeType = cartrigeTypeFromIndex(cartridgeTypeIndex)
    val romBankCount get()= when(romSizeIndex.asUnsigned()) {
        0 -> 0
        1 -> 1
        2 -> 2
        3 -> 4
        4 -> 8
        5 -> 16
        6 -> 32
        7 -> 64
        0x52 -> 72
        0x53 -> 80
        0x54 -> 96
        else -> error("Invalid rom size index ${romSizeIndex}")
    }
    val ramBankCount = when(ramSizeIndex.asUnsigned()) {
        0 -> 0
        1, 2 -> 1
        3 -> 4
        4 -> 16
        else -> error("Unknown ram size index $ramSizeIndex")
    }
    val ramBanks = Array(ramBankCount) { index ->
        RamBank("Switchable Ram Bank #$index")
    }
    var selectedRAMBankIndex = 0
    val currentRAMBank get()= ramBanks[selectedRAMBankIndex]
    override val name = "Cartridge (${cartrigeType.name})"

    fun cartrigeTypeFromIndex(index: Byte): MemoryComponent = when(index.asUnsigned()) {
        0 -> ROMOnly(this)
        1, 2, 3 -> MBC1(this)
        else -> error("Cartridge type $index not supported")
    }

    override fun write(address: Int, value: Int) {
        cartrigeType.write(address, value)
    }

    override fun read(address: Int) = cartrigeType.read(address)
}
