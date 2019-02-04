package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

class WramBankSelectRegister: Register("SVBK", registerValue = 1) {

    override fun write(address: Int, value: Int) {
        super.write(address, value and 0b111)
    }

    override fun read(address: Int): Int {
        return (super.read(address) and 0b111) or 0b1111_1000
    }
}