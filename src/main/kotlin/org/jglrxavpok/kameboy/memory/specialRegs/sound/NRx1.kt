package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Channel
import org.jglrxavpok.kameboy.sound.Sound

class NRx1(memory: MemoryComponent, val soundChannel: Channel, orValue: Int, sound: Sound): NRRegister(soundChannel.channelNumber, 1, orValue, sound) {

    // address: 0xFF01 + soundChannel.channelNumber * 0x10

    override fun write(address: Int, value: Int) {
        if(!sound.isOn())
            return
        super.write(address, value)
        if(soundChannel.channelNumber != 3) {
            soundChannel.loadLengthCounter(64 - (value and 0b11111))
        } else {
            soundChannel.loadLengthCounter(256 - value) // special case for Wave channel
        }
    }
}