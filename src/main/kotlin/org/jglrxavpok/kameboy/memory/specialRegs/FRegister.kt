package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class FRegister: Register("F", 0, 8) {

    override fun write(address: Int, value: Int) {
        super.write(address, value and 0b11110000)
    }

    override fun read(address: Int): Int {
        return super.read(address) and 0b11110000
    }
}