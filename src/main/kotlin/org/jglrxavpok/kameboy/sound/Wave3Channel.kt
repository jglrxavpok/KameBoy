package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.memory.MemoryMapper

class Wave3Channel(memoryMapper: MemoryMapper): SoundChannel(3, 256, memoryMapper) {

    companion object {
        val WaveTableStart = 0xFF30
        val Volumes = arrayOf(4, 0, 1, 2)
    }
    private var sampleIndex = 0
    val volumeControl get()= Volumes[(nr2.getValue() shr 5) and 0b11]
    override val dacEnabled get()= (nr3.getValue() and (1 shl 7)) != 0

    override fun onOutputClock(timer: Timer) {
        sampleIndex++ // read from the *new* position
        sampleIndex %= 32

        // select low or high nibble depending on sampleIndex (first is high)
        val sample = (memoryMapper.read(WaveTableStart + sampleIndex/2) shr (((sampleIndex+1)%2)*4)) and 0xF
        output(sample shr volumeControl)
    }

    override fun channelStep(cycles: Int) { }

    override fun reset() {
        super.reset()
        sampleIndex = 0
    }
}