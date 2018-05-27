package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.SerialIO

class SerialControllerRegister(val serialIO: SerialIO): Register("SC") {

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        val clockType = value and 0x1
        if(clockType != 0) { // internal clock
            if(value and (1 shl 7) != 0) {
                serialIO.startTransfer()
            }
        }
    }

}