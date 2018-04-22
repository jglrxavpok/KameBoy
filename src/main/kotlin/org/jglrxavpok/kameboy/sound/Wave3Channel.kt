package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.memory.MemoryMapper

class Wave3Channel(memoryMapper: MemoryMapper): SoundChannel(3, 256, memoryMapper) {

    companion object {
        val WaveTableStart = 0xFF30
        val Volumes = arrayOf(4, 0, 1, 2)
    }

    private val patternMemory = IntArray(16)
    private var sampleIndex = 0

    override val frequencyMultiplier = 2
    val volumeControl get()= Volumes[(nr2.getValue() shr 5) and 0b11]
    override val dacEnabled get()= (nr3.getValue() and (1 shl 7)) != 0

    override fun onOutputClock(timer: Timer) {
        // select low or high nibble depending on sampleIndex (first is high)
        val samples = patternMemory[sampleIndex/2]

        sampleIndex++
        sampleIndex %= 32

        val sample = if(sampleIndex % 2 != 0) samples else samples shr 4

        output(((sample and 0xF) shr volumeControl) and 0xF)
    }

    override fun channelStep(cycles: Int) { }

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