package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryComponent

object UnaccessibleMemory: MemoryComponent {
    override val name = "Unaccessible Memory"

    override fun write(address: Int, value: Int) { }

    override fun read(address: Int) = 0xFF
}