package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.time.SaveStateElement

class Square1Channel(memory: MemoryMapper): SquareChannel(memory, 1) {

    val sweepPeriod get()= (nr0.getValue() shr 4) and 0b111
    val sweepShift get()= nr0.getValue() and 0b111
    val decreaseSweep by nr0.bitVar(3)
    val sweepDirection get()= if(decreaseSweep) -1 else 1
    @SaveStateElement
    internal var shadowRegister = 0
    @SaveStateElement
    internal var sweepFlag = false
    @SaveStateElement
    internal var internalTimer = 8

    override fun trigger() {
        super.trigger()
        shadowRegister = frequency
        sweepFlag = sweepPeriod != 0 || sweepShift != 0
        internalTimer = if(sweepPeriod != 0) sweepPeriod else 8
        if(sweepShift != 0) {
            checkOverflow(calculateFrequency())
        }
    }

    private fun checkOverflow(freq: Int) {
        if(freq > 2047) {
            channelEnabled = false
        }
    }

    private fun calculateFrequency(): Int {
        val freq = shadowRegister shr sweepShift
        return shadowRegister + freq * sweepDirection
    }

    override fun clockSweep() {
        super.clockSweep()
        if(sweepFlag && sweepPeriod != 0) {
            if(internalTimer-- <= 0) {
                internalTimer = if(sweepPeriod != 0) sweepPeriod else 8
                val newFreq = calculateFrequency()
                checkOverflow(newFreq)

                if(newFreq <= 2047) {
                    shadowRegister = newFreq
                    frequency = newFreq

                    checkOverflow(calculateFrequency())
                }
            }
        }
    }

}
