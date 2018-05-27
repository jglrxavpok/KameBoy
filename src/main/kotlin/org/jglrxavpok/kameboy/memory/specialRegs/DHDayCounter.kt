package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.helpful.asUnsigned16
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.Register

class DHCounterRegister(): Register("RST DH") {

    val timerHalt by bitVar(6)

    fun dlOverflow() {
        if(registerValue and 0x1 == 1) {
            setValue(registerValue.setBits(0, 0..0))
            registerValue = registerValue.setBits(1, 7..7) // set overflow bit
        } else {
            setValue(registerValue.setBits(1, 0..0))
        }
    }

    override fun read(address: Int): Int {
        return super.read(address) or 0b1100_0001
    }
}