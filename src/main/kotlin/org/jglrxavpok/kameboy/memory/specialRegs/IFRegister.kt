package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class IFRegister: Register("IF") {

    override fun read(address: Int): Int {
        return super.read(address) or 0xE0
    }
}