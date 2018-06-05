package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.Register

class VramSelect: Register("Vram Select") {

    override fun write(address: Int, value: Int) {
        super.write(address, value and 1)
    }

    override fun read(address: Int): Int {
        return (super.read(address) and 1) or 0b1111_1110
    }
}