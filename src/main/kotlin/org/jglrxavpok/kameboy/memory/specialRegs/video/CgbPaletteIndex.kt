package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.Register

class CgbPaletteIndex(name: String): Register(name) {

    val IncrementMask = 1 shl 7

    val index get() = getValue() and 0b11111

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        if(value and IncrementMask != 0)
            incrementIndex()
    }

    private fun incrementIndex() {
        val newIndex = (index+1) % 0x40
        setValue(newIndex or (getValue() and IncrementMask))
    }

    override fun read(address: Int): Int {
        return super.read(address)
    }
}