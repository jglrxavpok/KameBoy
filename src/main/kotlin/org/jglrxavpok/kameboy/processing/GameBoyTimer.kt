package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.time.SaveStateElement

class GameBoyTimer(val mapper: MemoryMapper) {

    @SaveStateElement
    internal var currentDivCycle = 0.0

    fun step(cycles: Int) {
        // timer runs at a quarter of the speed of machine ticks (the emulator uses clock cycles ie 4*Machine ticks)
        currentDivCycle += cycles /4 /4

        while(currentDivCycle >= 4) {
            currentDivCycle -= 4
            mapper.divRegister.inc()
        }
    }

    fun resetTimerFromDiv() {
        currentDivCycle = 0.0
    }
}
