package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.sound.Sound.Companion.SecondsToCycles

/**
 * Square 2 Channel
 * Based on [gameboy_sound_emulation_by_blargg](https://github.com/Emu-Docs/Emu-Docs/blob/master/Game%20Boy/gameboy_sound_emulation_by_blargg.txt)
 *
 */
class Square2(sound: Sound): Channel(sound) {

    companion object {
        val WaveDuty = arrayOf(
                0b00000001,
                0b10000001,
                0b10000111,
                0b01111110
        )
        val High = 0b1111
        val Low = 0b0000
    }
    override val channelNumber = 2

    private var waveBitSelection = 0
    val timer = Timer { timer ->
        val currentWaveform = WaveDuty[sound.wavePattern1Duty]
        val isHigh = currentWaveform and (1 shl waveBitSelection)
        if(isHigh != 0) {
            output(High)
        } else {
            output(Low)
        }
        waveBitSelection++
        waveBitSelection %= 8
    }

    override fun onFrameSequencerStep(step: Int) {
    }

    override fun step(cycles: Int) {
        super.step(cycles)
        timer.step(cycles)
    }

    override fun reset() {
        super.reset()
        timer.periodInCycles = (sound.channel2Frequency * Sound.SecondsToCycles).toInt()
        timer.reset()
        disabled = false
        frameSequencer.reset()
    }
}