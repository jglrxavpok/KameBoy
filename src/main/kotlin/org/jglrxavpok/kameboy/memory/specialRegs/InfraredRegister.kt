package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register

/**
 * TODO: implement IR protocol
 */
class InfraredRegister: Register("RP") {

    val WriteMask = (1 shl 1).inv()

    override fun write(address: Int, value: Int) {
        super.write(address, value and WriteMask)
    }

    override fun read(address: Int): Int {
        return super.read(address) or (1 shl 2) //readData
    }
}