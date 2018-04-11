package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asUnsigned

class RomWrapper(val data: ByteArray): MemoryComponent {

    override val name = "RomWrapper@${hashCode()}"

    override fun write(address: Int, value: Int) {
    }

    override fun read(address: Int) = data[address].asUnsigned()

    override fun toString(): String {
        return name
    }
}