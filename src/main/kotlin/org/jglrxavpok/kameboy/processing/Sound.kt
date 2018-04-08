package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.specialRegs.SoundFreqHighRegister
import org.jglrxavpok.kameboy.memory.specialRegs.SoundToggleRegister

class Sound(val memory: MemoryMapper) {

    companion object {
        val SecondsToCycles = 1.0/4194304.0
    }

    private var currentCycleCount = 0
    val channelControl = MemoryRegister("NR50", memory, 0xFF24)
    val out1Volume get()= channelControl.getValue() and 0b111
    val out2Volume get()= (channelControl.getValue() shr 4) and 0b111
    val outputInTo1 by channelControl.bitVar(3)
    val outputInTo2 by channelControl.bitVar(7)

    val outputSelect = MemoryRegister("NR51", memory, 0xFF25)
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

    // SOUND 1
    val sound1Sweep = MemoryRegister("NR10", memory, 0xFF10)
    val sweepTime get()= (sound1Sweep.getValue() shr 4) and 0b111
    val sweepIncrease by soundToggle.bitVar(3)
    val numberOfSweeps get()= sound1Sweep.getValue() and 0b111
    val channel1Attributes = MemoryRegister("NR11", memory, 0xFF11)
    val wavePattern1Duty get()= (channel1Attributes.getValue() shr 6) and 0b11
    val sound1LengthRaw get()= channel1Attributes.getValue() and 0b1111
    /**
     * in seconds
     */
    val sound1Length get()= (64.0-sound1LengthRaw)/256.0
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
    val channel2Attributes = MemoryRegister("NR21", memory, 0xFF16)
    val wavePattern2Duty get()= (channel2Attributes.getValue() shr 6) and 0b11
    val sound2LengthRaw get()= channel2Attributes.getValue() and 0b1111
    /**
     * in seconds
     */
    val sound2Length get()= (64.0-sound2LengthRaw)/256.0

    val channel2VolumeEnveloppe = MemoryRegister("NR22", memory, 0xFF17)
    val initialEnveloppeVolume2 = (channel2VolumeEnveloppe.getValue() shr 4) and 0b1111
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
    val channel3SoundLengthRaw = MemoryRegister("NR31", memory, 0xFF1B)
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
    val channel4SoundLengthReg = MemoryRegister("NR41", memory, 0xFF20)
    val channel4SoundLength get()= (64.0-(channel4SoundLengthReg.getValue() and 0b11111)) / 256.0
    val channel4VolumeEnveloppe = MemoryRegister("NR42", memory, 0xFF21)
    val initialEnveloppeVolume4 = (channel4VolumeEnveloppe.getValue() shr 4) and 0b1111
    val enveloppeDirection4 by channel2VolumeEnveloppe.bitVar(3)
    val numberOfEnveloppeSweeps4 get()= channel4VolumeEnveloppe.getValue() and 0b111

    val channel4PolynomialCounter = MemoryRegister("NR43", memory, 0xFF22)
    val shiftClockFrequency = (channel4PolynomialCounter.getValue() shr 4) and 0b1111
    val counterWidth by channel4PolynomialCounter.bitVar(3)
    val divingRatioOfFreq get()= channel4PolynomialCounter.getValue() and 0b111

    val channel4CounterAndInitialReg = SoundFreqHighRegister(memory, "NR44", this, 0xFF23, 4)
    val sound4Initial by channel3FrequencyHigh.bitVar(7)
    val counterSelection4 by channel3FrequencyHigh.bitVar(6)

    fun step(cycles: Int) {
        currentCycleCount += cycles
    }

    fun resetSound(soundNumber: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun isSoundOn(soundNumber: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}