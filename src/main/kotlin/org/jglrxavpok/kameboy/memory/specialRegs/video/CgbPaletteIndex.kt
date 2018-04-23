package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.Register

class CgbPaletteIndex(name: String): Register(name) {

    val IncrementMask = 1 shl 7

    val index get() = getValue() and 0b111111

    fun incrementIndex() {
        val newIndex = (index+1) and 0b111111
        setValue(newIndex or (getValue() and IncrementMask))
    }

}