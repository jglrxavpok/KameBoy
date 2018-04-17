package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*

class Sound(val memory: MemoryMapper) {

    companion object {
        val SecondsToCycles = 1.0/CpuClockSpeed
    }

    private var currentCycleCount = 0
    val channelControl = NRRegister(5, 0, 0x00, this)
    val out1Volume get()= channelControl.getValue() and 0b111
    val out2Volume get()= (channelControl.getValue() shr 4) and 0b111
    val outputInTo1 by channelControl.bitVar(3)
    val outputInTo2 by channelControl.bitVar(7)

    val outputSelect = NRRegister(5, 1, 0x00, this)
    val output4to2 by outputSelect.bitVar(7)
    val output3to2 by outputSelect.bitVar(6)
    val output2to2 by outputSelect.bitVar(5)
    val output1to2 by outputSelect.bitVar(4)
    val output4to1 by outputSelect.bitVar(3)
    val output3to1 by outputSelect.bitVar(2)
    val output2to1 by outputSelect.bitVar(1)
    val output1to1 by outputSelect.bitVar(0)
    val soundToggle = SoundToggleRegister(this)
    val soundToggle4 by soundToggle.bitVar(3)
    val soundToggle3 by soundToggle.bitVar(2)
    val soundToggle2 by soundToggle.bitVar(1)
    val soundToggle1 by soundToggle.bitVar(0)

    val channel1 = Square1(this)
    val channel2 = Square2(this)
    val channel3 = Wave3(this)
    val channel4 = Noise4(this)

    // SOUND 1
    val sound1Sweep = MemoryRegister("NR10", memory, 0xFF10)
    val sweepPeriod get()= (sound1Sweep.getValue() shr 4) and 0b111
    val sweepIncrease by soundToggle.bitVar(3)
    val numberOfSweeps get()= sound1Sweep.getValue() and 0b111
    val channel1Attributes = NRx1(memory, channel1, 0x3F, this)
    val wavePattern1Duty get()= (channel1Attributes.getValue() shr 6) and 0b11
    val sound1LengthRaw get()= channel1Attributes.getValue() and 0b1111
    /**
     * in seconds
     */
    val sound1Length get()= (64.0-sound1LengthRaw)/256.0
    val channel1VolumeEnveloppe = MemoryRegister("NR12", memory, 0xFF12)
    val initialEnveloppeVolume1 get()= (channel1VolumeEnveloppe.getValue() shr 4) and 0b1111
    val enveloppeDirection1 by channel1VolumeEnveloppe.bitVar(3)
    val numberOfEnveloppeSweeps1 get()= channel1VolumeEnveloppe.getValue() and 0b111

    val channel1FrequencyLow = MemoryRegister("NR13", memory, 0xFF13)
    val channel1FrequencyHigh = SoundFreqHighRegister(memory, "NR14", this, 0xFF14, 1)
    val sound1Initial by channel1FrequencyHigh.bitVar(7)
    val counterSelection1 by channel1FrequencyHigh.bitVar(6)
    /**
     * In Hz
     */
    val channel1Frequency: Double get() {
        val x = ((channel1FrequencyHigh.getValue() and 0b111) shl 8) or channel1FrequencyLow.getValue()
        return 131072.0/(2048.0-x)
    }

    // SOUND 2
    val channel2Attributes = NRx1(memory, channel2, 0x3F, this)
    val wavePattern2Duty get()= (channel2Attributes.getValue() shr 6) and 0b11
    val sound2LengthRaw get()= channel2Attributes.getValue() and 0b1111
    /**
     * in seconds
     */
    val sound2Length get()= (64.0-sound2LengthRaw)/256.0

    val channel2VolumeEnveloppe = MemoryRegister("NR22", memory, 0xFF17)
    val initialEnveloppeVolume2 get()= (channel2VolumeEnveloppe.getValue() shr 4) and 0b1111
    val enveloppeDirection2 by channel2VolumeEnveloppe.bitVar(3)
    val numberOfEnveloppeSweeps2 get()= channel2VolumeEnveloppe.getValue() and 0b111

    val channel2FrequencyLow = MemoryRegister("NR23", memory, 0xFF18)
    val channel2FrequencyHigh = SoundFreqHighRegister(memory, "NR24", this, 0xFF19, 2)
    val sound2Initial by channel2FrequencyHigh.bitVar(7)
    val counterSelection2 by channel2FrequencyHigh.bitVar(6)
    /**
     * In Hz
     */
    val channel2Frequency: Double get() {
        val x = ((channel2FrequencyHigh.getValue() and 0b111) shl 8) or channel2FrequencyLow.getValue()
        return 131072.0/(2048.0-x)
    }

    // SOUND 3
    val channel2ToggleReg = MemoryRegister("NR30", memory, 0xFF1A)
    val channel2Toggle by channel2ToggleReg.bitVar(7)
    val channel3SoundLengthRaw = NRx1(memory, channel3, 0xFF, this)
    val channel3SoundLength get()= (256.0-channel3SoundLengthRaw.getValue())/256.0
    val channel3OutputLevelReg = MemoryRegister("NR32", memory, 0xFF1C)
    val channel3OutputLevel get()= (channel3OutputLevelReg.getValue() shr 5) and 0b11

    val channel3FrequencyLow = MemoryRegister("NR33", memory, 0xFF1D)
    val channel3FrequencyHigh = SoundFreqHighRegister(memory, "NR34", this, 0xFF1E, 3)
    val sound3Initial by channel3FrequencyHigh.bitVar(7)
    val counterSelection3 by channel3FrequencyHigh.bitVar(6)
    /**
     * In Hz
     */
    val channel3Frequency: Double get() {
        val x = ((channel3FrequencyHigh.getValue() and 0b111) shl 8) or channel3FrequencyLow.getValue()
        return 65536.0/(2048.0-x)
    }

    val WavePatternInterval = 0xFF30..0xFF3F

    // SOUND 4
    val channel4SoundLengthReg = NRx1(memory, channel4, 0xFF, this)
    val channel4SoundLength get()= (64.0-(channel4SoundLengthReg.getValue() and 0b11111)) / 256.0
    val channel4VolumeEnveloppe = MemoryRegister("NR42", memory, 0xFF21)
    val initialEnveloppeVolume4 get()= (channel4VolumeEnveloppe.getValue() shr 4) and 0b1111
    val enveloppeDirection4 by channel4VolumeEnveloppe.bitVar(3)
    val numberOfEnveloppeSweeps4 get()= channel4VolumeEnveloppe.getValue() and 0b111

    val channel4PolynomialCounter = MemoryRegister("NR43", memory, 0xFF22)
    val shiftClockFrequency get()= (channel4PolynomialCounter.getValue() shr 4) and 0b1111
    val counterWidth by channel4PolynomialCounter.bitVar(3)
    val divingRatioOfFreq get()= channel4PolynomialCounter.getValue() and 0b111

    val channel4CounterAndInitialReg = SoundFreqHighRegister(memory, "NR44", this, 0xFF23, 4)
    val sound4Initial by channel4CounterAndInitialReg.bitVar(7)
    val counterSelection4 by channel4CounterAndInitialReg.bitVar(6)
    val counterSelection get()= arrayOf(counterSelection1, counterSelection2, counterSelection3, counterSelection4)
    val enveloppePeriod get()= arrayOf(numberOfEnveloppeSweeps1, numberOfEnveloppeSweeps2, -1, numberOfEnveloppeSweeps4)
    val enveloppeDirection get()= arrayOf(enveloppeDirection1, enveloppeDirection2, false, enveloppeDirection4)
    val initialEnveloppeVolume get()= arrayOf(initialEnveloppeVolume1, initialEnveloppeVolume2, -1, initialEnveloppeVolume4)

    var output: (Int, Int) -> Unit = {_,_->}

    private var leftChannel = 0
    private var rightChannel = 0

    fun step(cycles: Int) {
        currentCycleCount += cycles
        if(soundToggle.isOn) {
            if(channel1.isSoundOn())
                channel1.step(cycles)
            if(channel2.isSoundOn())
                channel2.step(cycles)
            if(channel3.isSoundOn())
                channel3.step(cycles)
            if(channel4.isSoundOn())
                channel4.step(cycles)
            outputSound()
        }
    }

    private fun outputSound() {
        // between -8.0V and +8.0V
        leftChannel /= 4
        rightChannel /= 4
        val leftVolume = (out1Volume+1) * leftChannel
        val rightVolume = (out2Volume+1) * rightChannel
        output(leftVolume, rightVolume)
    }

    fun resetSound(soundNumber: Int) {
        when(soundNumber) {
            1 -> channel1.reset()
            2 -> channel2.reset()
            3 -> channel3.reset()
            4 -> channel4.reset()
            else -> error("Wrong sound channel number $soundNumber")
        }
    }

    fun isSoundOn(soundNumber: Int): Boolean {
        return when(soundNumber) {
            1 -> channel1.isSoundOn()
            2 -> channel2.isSoundOn()
            3 -> channel3.isSoundOn()
            4 -> channel4.isSoundOn()
            else -> error("Wrong sound channel number $soundNumber")
        }
    }

    fun mix(volume: Int, channelNumber: Int) {
        if(shouldGoTo(channelNumber, ChannelPosition.Left)) {
            leftChannel += volume
        }
        if(shouldGoTo(channelNumber, ChannelPosition.Right)) {
            rightChannel += volume
        }
    }

    private fun shouldGoTo(channelNumber: Int, position: ChannelPosition): Boolean {
        val bit = (channelNumber-1) + (position.ordinal*4)
        return outputSelect.getValue() and bit != 0
    }

    fun isOn(): Boolean {
        return soundToggle.isOn
    }

    enum class ChannelPosition {
        Left, Right
    }
}