package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*
import org.jglrxavpok.kameboy.ui.options.SoundOptions

class Sound(val memory: MemoryMapper) {

    val gameboy = memory.gameboy
    val channel1 = Square1Channel(memory)
    val channel2 = SquareChannel(memory, 2)
    val channel3 = Wave3Channel(memory)
    val channel4 = Noise4Channel(memory)
    val channels = arrayOf(channel1, channel2, channel3, channel4)

    val channelControl = NRRegister(5, 0, 0x00, this)
    val outputSelect = NRRegister(5, 1, 0x00, this)
    val soundToggle = SoundToggleRegister(this)

    var play: (Int, Int) -> Unit = {_,_->}

    inline fun channel(index: Int) = channels[index-1]

    init {
        channel1.channelEnabled = true
        channel2.channelEnabled = true
        channel3.channelEnabled = true
        channel4.channelEnabled = true
    }

    fun step(cycles: Int) {
        if(!isOn()) {
            for(i in 0 until cycles) {
                play(0, 0)
            }
            return
        }

        var leftChannel = 0
        var rightChannel = 0
        val outputSelectValue = outputSelect.getValue()
        val channelControlValue = channelControl.getValue()

        fun stepAudio(cycles: Int) {
            for(channelIndex in 0..3) {
                val channel = channels[channelIndex]
                val waveform = channel.step(cycles)
                if(outputSelectValue and (1 shl (channelIndex)) != 0) {
                    leftChannel += waveform
                }
                if(outputSelectValue and (1 shl (channelIndex+4)) != 0) {
                    rightChannel += waveform
                }
            }
            leftChannel /= 4
            rightChannel /= 4
            leftChannel *= (channelControlValue and 0b111)
            rightChannel *= ((channelControlValue shr 4) and 0b111)
        }

        if(SoundOptions.skipAudioCycles) {
            stepAudio(cycles)
            repeat(cycles) {
                play(leftChannel.asSigned8(), rightChannel.asSigned8())
            }
        } else {
            repeat(cycles) {
                leftChannel = 0
                rightChannel = 0
                stepAudio(1)
                play(leftChannel.asSigned8(), rightChannel.asSigned8())
            }
        }
    }

    fun isOn(): Boolean {
        return soundToggle.isOn
    }

    fun resetFromDiv() {
        channels.forEach {
            it.resetFromDiv()
        }
    }

    fun update() {
        // TODO: move timing logic here
    }
}