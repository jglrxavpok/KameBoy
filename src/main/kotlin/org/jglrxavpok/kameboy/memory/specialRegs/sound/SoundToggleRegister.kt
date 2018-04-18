package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Sound

class SoundToggleRegister(val sound: Sound): Register("NR52") {

    var isOn: Boolean = true

    override fun write(address: Int, value: Int) {
        val valueToWrite = value and (1 shl 7)
        if(valueToWrite == 0) { // power off APU
            if(isOn) {
                for(addr in 0xFF10..0xFF25) { // NR10-NR51
                    if(addr != 0xFF20) // NR41
                        sound.memory.write(addr, 0x00)
                }

                for(number in 1..4) {
                    sound.channel(number).stop()
                }
            }
        }
        // the order is important, write 0x00 **then** turn off
        super.write(address, valueToWrite)
        if(!isOn && valueToWrite != 0) { // turning on
            for(number in 1..4)
                sound.channel(number).reset()
        }
        isOn = valueToWrite != 0
    }

    override fun read(address: Int): Int {
        val allOf = if(isOn) 1 shl 7 else 0
        var result = allOf
        for(index in 1..4) {
            val channel = sound.channel(index)
            if(channel.channelEnabled) {
                result = result or (1 shl (index-1))
            }
        }
        println("READ FROM NR52: ${Integer.toBinaryString(result)}")
        return result or 0x70
    }
}