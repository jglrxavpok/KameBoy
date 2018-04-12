package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.sound.Sound.Companion.SecondsToCycles

/**
 * Noise 4 Channel
 * Based on [gameboy_sound_emulation_by_blargg](https://github.com/Emu-Docs/Emu-Docs/blob/master/Game%20Boy/gameboy_sound_emulation_by_blargg.txt)
 *
 */
class Noise4(sound: Sound): Channel(sound) {

    companion object {
        val Divisors = arrayOf(8,16,32,48,64,80,96,112)
        val High = 0b1111
    }
    override val channelNumber = 4

    private var register = 0

    val timer = Timer { timer ->
        val xorResult = (register and 0b1) xor ((register shr 1) and 0b1)
        register = register shr 1
        register = register.setBits(xorResult, 7..7)
        if(sound.counterWidth) {
            register = register.setBits(xorResult, 6..6)
        }
        output((register and 0b1) * High)
    }

    override fun onFrameSequencerStep(step: Int) {
    }

    override fun step(cycles: Int) {
        super.step(cycles)
        timer.step(cycles)
    }

    override fun reset() {
        super.reset()
        // FIXME: << <what value> ?
        timer.periodInCycles = (Divisors[sound.divingRatioOfFreq] shl 1).toInt()
        timer.reset()
        disabled = false
        frameSequencer.reset()
    }
}