package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.helpful.asUnsigned16
import org.jglrxavpok.kameboy.memory.Register

class DLCounterRegister(val high: DHCounterRegister): Register("RST DL") {
    override fun fitValueInBounds() {
        if(registerValue.asUnsigned16() > 0xFF) {
            registerValue %= 0x100
            high.dlOverflow()
        }
        registerValue = registerValue and 0xFF
    }
}