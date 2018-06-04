package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryComponent

class UnknownRegister(val address: Int): MemoryComponent {
    override val name = "Unknown ${Integer.toHexString(address)}"

    override fun write(address: Int, value: Int) { } // NOP

    override fun read(address: Int): Int {
        return 0xFF
    }

}