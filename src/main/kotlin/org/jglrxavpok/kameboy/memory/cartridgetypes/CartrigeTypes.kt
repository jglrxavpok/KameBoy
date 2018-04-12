package org.jglrxavpok.kameboy.memory.cartridgetypes

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryComponent

abstract class CartridgeType: MemoryComponent {
    abstract fun accepts(address: Int): Boolean
}

class ROMOnly(val cartridge: Cartridge): CartridgeType() {
    private val data = cartridge.rawData

    override val name = "ROM Only"

    override fun write(address: Int, value: Int) { }

    override fun accepts(address: Int): Boolean {
        return address in 0 until 0x8000
    }

    override fun read(address: Int) = data[address].asUnsigned()

    override fun toString(): String {
        return "ROM Only"
    }
}