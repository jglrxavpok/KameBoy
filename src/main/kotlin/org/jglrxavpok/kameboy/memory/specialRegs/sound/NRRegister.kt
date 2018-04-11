package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.sound.Sound

open class NRRegister(channelNumber: Int, registerIndex: Int, orValue: Int, val sound: Sound): OrOnReadRegister("NR$channelNumber$registerIndex", orValue) {

    override fun write(address: Int, value: Int) {
        if(sound.isOn())
            super.write(address, value)
    }
}
