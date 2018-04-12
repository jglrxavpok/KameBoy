package org.jglrxavpok.kameboy.sound

abstract class Channel(val sound: Sound) {

    abstract val channelNumber: Int

    abstract fun onFrameSequencerStep(step: Int)
    open val hasVolumeEnveloppe = true
    private var frameStep = 0

    open fun reset() {
        volume = sound.initialEnveloppeVolume[channelNumber-1]
        frameStep = 0
        frameSequencer.reset()
    }

    open fun step(cycles: Int) {
        frameSequencer.step(cycles)
    }

    val frameSequencer = Timer((512/8* Sound.SecondsToCycles).toInt()) { frameSequencer ->
        when {
            frameStep % 2 == 0 -> {
                clockLengthCounter()
            }
            frameStep == 7 -> {
                clockVolumeEnveloppe()
            }
        }
        onFrameSequencerStep(frameStep)
        frameStep++
        frameStep %= 8
    }

    protected var disabled = true
    private var clockLength = 0
    private var volume = 0

    private fun clockVolumeEnveloppe() {
        if(hasVolumeEnveloppe && sound.enveloppePeriod[channelNumber-1] != 0) {
            val newVolume = volume + if(sound.enveloppeDirection[channelNumber-1]) 1 else -1
            if(newVolume in 0..15) {
                volume = newVolume
            }
            if(volume == 0) {
                disabled = true
                reset()
            }
        }
    }

    fun isSoundOn(): Boolean {
        return !disabled
    }

    fun clockLengthCounter() {
        if(sound.counterSelection[channelNumber-1]) {
            clockLength--
            if(clockLength <= 0) {
                clockLength = 0
                disabled = true
                reset()
            }
        }
    }

    fun loadLengthCounter(data: Int) {
        clockLength = data
    }

    fun output(nibble: Int) {
        //val voltage = dac(nibble)
        sound.mix(nibble, channelNumber)
    }

    private fun dac(nibble: Int): Double {
        return -1.0 + 2.0 * (nibble and 0xF)/15.0
    }
}

class Timer(val defaultPeriod: Int = Int.MAX_VALUE, val outputClock: (Timer) -> Unit) {
    var periodInCycles = defaultPeriod

    private var counter = 0

    fun step(cycles: Int) {
        counter -= cycles
        if(counter <= 0) {
            reset()
            outputClock(this)
        }
    }

    fun reset() {
        counter = periodInCycles
    }
}