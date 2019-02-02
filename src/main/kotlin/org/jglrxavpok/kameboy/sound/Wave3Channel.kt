package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.time.SaveStateElement

class Wave3Channel(memoryMapper: MemoryMapper): SoundChannel(3, 256, memoryMapper) {

    companion object {
        val WaveTableStart = 0xFF30

        @JvmStatic
        val VolumeShifts = arrayOf(4, 0, 1, 2)
    }

    private val patternMemory = IntArray(16)
    @SaveStateElement
    internal var sampleIndex = 0

    override val frequencyMultiplier = 2
    val volumeControl get()= VolumeShifts[(nr2.getValue() shr 5) and 0b11]
    override val dacEnabled by nr3.bitVar(7)

    override fun onOutputClock(timer: Timer) {
        // select low or high nibble depending on sampleIndex (first is high)
        val samples = patternMemory[sampleIndex/2]

        sampleIndex++
        sampleIndex %= 32

        val sample = if(sampleIndex % 2 != 0) samples else (samples shr 4)

        output(((sample and 0xF) shr volumeControl) and 0xF)
    }

    override fun reset() {
        super.reset()
        sampleIndex = 0
    }

    override fun trigger() {
        super.trigger()
        memoryMapper.wavePatternRam.copyTo(patternMemory)
        memoryMapper.wavePatternRam.clear()
    }
}