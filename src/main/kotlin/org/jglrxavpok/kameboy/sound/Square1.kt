package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.helpful.toClockCycles

/**
 * Square 1 Channel
 * Based on [gameboy_sound_emulation_by_blargg](https://github.com/Emu-Docs/Emu-Docs/blob/master/Game%20Boy/gameboy_sound_emulation_by_blargg.txt)
 *
 */
class Square1(sound: Sound): Channel(sound) {

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
    override val channelNumber = 1

    var sweepShadowRegister: Int = 0
    var sweepFlag = false

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
        when {
            (step+2) % 4 == 0 -> {
                clockSweep()
            }
        }
    }

    private fun clockSweep() {
        if(sweepFlag && sound.sweepPeriod != 0 && sound.numberOfSweeps != 0) {
            calculateFrequency()
            val newFreq = sweepShadowRegister
            checkOverflow()
            if(newFreq <= 2047 && sound.numberOfSweeps != 0) {
                sound.channel1FrequencyLow.setValue(newFreq and 0xFF)
                val previousRegValue = sound.channel1FrequencyHigh.getValue()
                val valueToWrite = previousRegValue.setBits((newFreq shr 8) and 0b111, 0..2)
                sound.channel1FrequencyHigh.setValue(valueToWrite)
                calculateFrequency()
                checkOverflow()
            }
        }
    }

    private fun checkOverflow() {
        if(sweepShadowRegister > 2047) {
            disabled = true
        }
    }

    private fun calculateFrequency() {
        val freq = (sweepShadowRegister shr sound.numberOfSweeps) * if(sound.sweepIncrease) 1 else -1
        sweepShadowRegister += freq
    }

    override fun step(cycles: Int) {
        super.step(cycles)
        timer.step(cycles)
    }

    override fun reset() {
        super.reset()
        timer.periodInCycles = sound.channel1Frequency.toInt().toClockCycles()
        timer.reset()
        disabled = false
        sweepShadowRegister = sound.channel1Frequency.toInt()
        frameSequencer.reset()
        //sweepFlag = sound.sweepPeriod != 0 || sound.numberOfSweeps != 0
       /* if(sound.numberOfSweeps != 0) {
            calculateFrequency()
            checkOverflow()
        }*/
    }
}