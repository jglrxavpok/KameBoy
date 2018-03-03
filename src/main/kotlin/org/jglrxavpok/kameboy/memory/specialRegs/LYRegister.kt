package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class LYRegister: Register("LY") {

    override fun write(address: Int, value: Int) {
        setValue(0)
    }
}