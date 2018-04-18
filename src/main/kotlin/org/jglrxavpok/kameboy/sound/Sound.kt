package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*

class Sound(val memory: MemoryMapper) {

    val channel1 = Square1Channel(memory)
    val channel2 = SquareChannel(memory, 2)
    val channel3 = Wave3Channel(memory)
    val channel4 = Noise4Channel(memory)
    val channels = arrayOf(channel1, channel2, channel3, channel4)

    val channelControl = NRRegister(5, 0, 0x00, this)
    val outputSelect = NRRegister(5, 1, 0x00, this)
    val soundToggle = SoundToggleRegister(this)

    var play: (Byte, Byte) -> Unit = {_,_->}

    fun channel(index: Int) = channels[index-1]

    fun step(cycles: Int) {
        if(!isOn()) {
            repeat(cycles) {
                play(0, 0)
            }
            return
        }
        repeat(cycles) {
            var leftChannel = 0
            var rightChannel = 0
            for(index in 1..4) {
                val channel = channel(index)
                val waveform = channel(index).step(1)
                if(outputSelect.getValue() and (1 shl (channel.channelNumber-1)) != 0) {
                    leftChannel += waveform
                }
                if(outputSelect.getValue() and (1 shl (channel.channelNumber-1+4)) != 0) {
                    rightChannel += waveform
                }
            }
            leftChannel /= 4
            rightChannel /= 4
            leftChannel *= (channelControl.getValue() and 0b111)
            rightChannel *= ((channelControl.getValue() shr 4) and 0b111)

            play(leftChannel.toByte(), rightChannel.toByte())
        }
    }

    fun isOn(): Boolean {
        return soundToggle.isOn
    }
}