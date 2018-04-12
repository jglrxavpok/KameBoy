package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.SerialIO

class SerialDataRegister(val serialIO: SerialIO, val control: SerialControllerRegister): Register("SB") {

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        serialIO.transfer(value)
    }

    override fun read(address: Int): Int {
        //return super.read(address)
        return 0xFF
    }
}