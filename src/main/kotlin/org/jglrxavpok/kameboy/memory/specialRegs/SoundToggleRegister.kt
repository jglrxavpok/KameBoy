package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Sound

class SoundToggleRegister(val sound: Sound): Register("NR52") {

    override fun write(address: Int, value: Int) {
        super.write(address, value and 0b10000000)
    }

    override fun read(address: Int): Int {
        val allOf = super.read(address) and 0b10000000
        var result = allOf
        if(sound.isSoundOn(1)) {
            result = result or (1 shl 0)
        }
        if(sound.isSoundOn(2)) {
            result = result or (1 shl 1)
        }
        if(sound.isSoundOn(3)) {
            result = result or (1 shl 2)
        }
        if(sound.isSoundOn(4)) {
            result = result or (1 shl 3)
        }
        return result
    }
}