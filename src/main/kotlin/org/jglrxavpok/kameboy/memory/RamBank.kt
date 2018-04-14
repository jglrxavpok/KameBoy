package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asUnsigned

class RamBank(override val name: String): MemoryComponent {

    val data = ByteArray(0x2000)
    val startAddress = 0xA000

    init {
        data.fill(0xFF.toByte())
    }

    override fun write(address: Int, value: Int) {
        data[address - startAddress] = value.toByte()
    }

    override fun read(address: Int) = data[address - startAddress].asUnsigned()
}