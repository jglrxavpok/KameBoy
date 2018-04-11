package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.Register

open class OrOnReadRegister(name: String, val orValue: Int): Register(name) {
    override fun read(address: Int): Int {
        return super.read(address) or orValue
    }
}
