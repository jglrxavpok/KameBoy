package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Sound

class SoundToggleRegister(val sound: Sound): Register("NR52") {

    var isOn: Boolean = true

    override fun write(address: Int, value: Int) {
        val valueToWrite = value and 0b10000000
        if(valueToWrite == 0) { // power off APU
            for(addr in 0xFF10..0xFF25) { // NR10-NR51
                sound.memory.write(addr, 0x00)
            }
        }
        // the order is important, write 0x00 **then** turn off
        super.write(address, valueToWrite)
        isOn = valueToWrite != 0
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
        return result or 0x70
    }
}