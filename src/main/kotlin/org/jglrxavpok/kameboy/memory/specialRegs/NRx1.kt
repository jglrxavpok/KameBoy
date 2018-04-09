package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.sound.Channel

class NRx1(memory: MemoryComponent, val soundChannel: Channel): Register("NR${soundChannel.channelNumber}1", 0) {

    // address: 0xFF01 + soundChannel.channelNumber * 0x10

    override fun write(address: Int, value: Int) {
        super.write(address, value)
        if(soundChannel.channelNumber != 3) {
            soundChannel.loadLengthCounter(64 - (value and 0b11111))
        } else {
            soundChannel.loadLengthCounter(256 - value) // special case for Wave channel
        }
    }
}