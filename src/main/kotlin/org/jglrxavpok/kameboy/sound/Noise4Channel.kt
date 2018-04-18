package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.sound.SquareChannel.Companion.High

class Noise4Channel(memoryMapper: MemoryMapper): SoundChannel(4, 64, memoryMapper) {

    companion object {
        val DivisorToPeriod = arrayOf(8, 16, 32, 48, 64, 80, 96, 112)
    }

    val widthMode7 by nr4.bitVar(3)
    private var lfsr = 0x7FFF

    override var frequency: Int
        get() = ((1.0/timer.period)*CpuClockSpeed).toInt()
        set(value) {}

    override fun trigger() {
        super.trigger()
        timer.period = DivisorToPeriod[nr3.getValue() and 0b111] shl ((nr3.getValue() shr 4) and 0xF)
        timer.reset()
        lfsr = 0x7FFF
    }

    override fun onOutputClock(timer: Timer) {
        val xorLow = (lfsr and 0x1) xor (lfsr and 0x2)
        lfsr = lfsr shr 1
        lfsr = lfsr.setBits(xorLow , 15..15)
        if(widthMode7) {
            lfsr = lfsr.setBits(xorLow, 6..6)
        }

        output((1 - (lfsr and 1)) * High)
    }

    override fun channelStep(cycles: Int) { }
}