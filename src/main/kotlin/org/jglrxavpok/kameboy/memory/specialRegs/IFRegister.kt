package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class IFRegister: Register("IF") {

    override fun setValue(value: Int) {
        super.setValue(value or 0xE0)
    }
}