package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Sound

class SoundRegister(address: Int, val sound: Sound, orValue: Int = 0xFF): OrOnReadRegister(Integer.toHexString(address), orValue) {

    override fun write(address: Int, value: Int) {
        if(sound.isOn())
            super.write(address, value)
    }
}