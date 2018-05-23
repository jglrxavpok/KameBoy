package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.SerialIO

class SerialControllerRegister(val serialIO: SerialIO): Register("SC") {

    val transferring by bitVar(7)

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        val clockType = value and 0x1
        //if(clockType == 1) {
            if(value and (1 shl 7) != 0) {
                serialIO.startTransfer()
            }
        //}
        // TODO: handle case in which we are on the receiving end
    }

    override fun read(address: Int): Int {
        return super.read(address)// or 0b01111111
    }
}