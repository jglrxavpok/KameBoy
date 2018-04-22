package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.sound.Sound

open class NRRegister(val channelNumber: Int, val registerIndex: Int, orValue: Int, val sound: Sound): OrOnReadRegister("NR$channelNumber$registerIndex", orValue) {

    companion object {
        val TriggerMask = 1 shl 7
    }

    override fun write(address: Int, value: Int) {
        if(!sound.isOn())
            return
        super.write(address, value and 0b1111_1111)
        if(channelNumber == 5)
            return
        if(registerIndex == 4) {
            val channel = sound.channel(channelNumber)
            if(value and TriggerMask != 0) {
                channel.trigger()
            } else if(channel.channelEnabled) {
                channel.stop()
            }

            // most significant bits of frequency
            channel.frequency = channel.frequency.setBits(value and 0b111, 8..11)
        }

        if (registerIndex == 3) { // least significant bits of frequency
            val channel = sound.channel(channelNumber)
            channel.frequency = channel.frequency.setBits(value, 0..7)
        }
    }
}
