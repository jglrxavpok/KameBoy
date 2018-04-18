package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.sound.Sound
import org.jglrxavpok.kameboy.sound.SoundChannel

class NRx1(val soundChannel: SoundChannel, orValue: Int, sound: Sound): NRRegister(soundChannel.channelNumber, 1, orValue, sound) {

    // address: 0xFF01 + soundChannel.channelNumber * 0x10

    override fun write(address: Int, value: Int) {
        if(!sound.isOn() && soundChannel.channelNumber != 4) // NR41 can be written to even when power is off
            return
        super.write(address, value)
        if(channelNumber == 3) {
            soundChannel.loadLength(soundChannel.length - value)
        } else {
            soundChannel.loadLength(soundChannel.length - (value and 0b11111))
        }
    }
}