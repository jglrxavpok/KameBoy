package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asUnsigned

class RamBank(override val name: String): MemoryComponent {

    private val data = ByteArray(0x2000)
    val startAddress = 0xA000
    var enabled = false

    init {
        data.fill(0xFF.toByte())
    }

    override fun write(address: Int, value: Int) {
        if(enabled)
            data[address - startAddress] = value.toByte()
    }

    override fun read(address: Int) = if(enabled) data[address - startAddress].asUnsigned() else 0xFF
}