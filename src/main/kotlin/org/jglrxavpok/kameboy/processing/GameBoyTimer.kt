package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.time.SaveStateElement

class GameBoyTimer(val mapper: MemoryMapper) {

    companion object {
        val DivCycleRate = 16384.toClockCycles() // 4194304 (cpu clock speed) /16384 (divider speed)
        val TimerCounterRates = arrayOf(4096.toClockCycles(), 262144.toClockCycles(), 65536.toClockCycles(), 16384.toClockCycles())// 4194304 (cpu clock speed) /x (timer speed)
    }

    @SaveStateElement
    internal var currentDivCycle = 0
    @SaveStateElement
    internal var currentTimerCycle = 0

    fun step(cycles: Int) {
        currentTimerCycle += cycles
        currentDivCycle += cycles

        while(currentDivCycle >= 4) {
            currentDivCycle -= 4
            mapper.divRegister.inc()
        }
    }

    fun resetTimerFromDiv() {
        currentDivCycle = 0
        currentTimerCycle = 0
    }
}
