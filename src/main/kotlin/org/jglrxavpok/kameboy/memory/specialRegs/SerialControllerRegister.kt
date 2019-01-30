package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.SerialIO
import org.jglrxavpok.kameboy.network.host.Server

class SerialControllerRegister(val serialIO: SerialIO): Register("SC", registerValue = 0x7E) {

    override fun write(address: Int, value: Int) {
        val previousValue = getValue()
        super.write(address, value)

        // todo: handle when guest tries to set itself into master mode
        val clockType = /*read(address)*/value and 0x1
        if(clockType != 0) { // internal clock
            if(previousValue and (1 shl 7) == 0 && value and (1 shl 7) != 0) {
                serialIO.startTransfer()
            }
        }
    }

    override fun read(address: Int): Int {
        //val clockTypeBit = if(Server.isRunning()) 1 else 0
        return super.read(address) or 0x7E//.setBits(clockTypeBit, 0..0)
    }
}