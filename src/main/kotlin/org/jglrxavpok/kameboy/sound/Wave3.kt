package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.sound.Sound.Companion.SecondsToCycles

/**
 * Wave 3 Channel
 * Based on [gameboy_sound_emulation_by_blargg](https://github.com/Emu-Docs/Emu-Docs/blob/master/Game%20Boy/gameboy_sound_emulation_by_blargg.txt)
 *
 */
class Wave3(sound: Sound): Channel(sound) {

    override val channelNumber = 3

    private var waveBitSelection = 0
    private var isHighNibble = true

    val timer = Timer { timer ->
        val data = sound.memory.read(sound.WavePatternInterval.start + waveBitSelection)
        val currentWaveform = if(isHighNibble) {
            (data shr 4) and 0b1111
        } else {
            data and 0b1111
        }
        output(currentWaveform)
        if(!isHighNibble) {
            isHighNibble = true
            waveBitSelection++
            waveBitSelection %= 32
        } else {
            isHighNibble = false
        }
    }

    override fun onFrameSequencerStep(step: Int) {
    }

    override fun step(cycles: Int) {
        super.step(cycles)
        timer.step(cycles)
    }

    override fun reset() {
        super.reset()
        timer.periodInCycles = (sound.channel3Frequency * Sound.SecondsToCycles).toInt()
        timer.reset()
        disabled = false
        waveBitSelection = 0
        frameSequencer.reset()
    }
}