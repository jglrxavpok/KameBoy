package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class DivRegister: Register("DIV") {

    override fun write(address: Int, value: Int) {
        setValue(0)
    }
}