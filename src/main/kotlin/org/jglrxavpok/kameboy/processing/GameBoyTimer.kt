package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister

class GameBoyTimer(val mapper: MemoryMapper) {

    companion object {
        val DivCycleRate = 16384.toClockCycles() // 4194304 (cpu clock speed) /16384 (divider speed)
        val TimerCounterRates = arrayOf(4096.toClockCycles(), 262144.toClockCycles(), 65536.toClockCycles(), 16384.toClockCycles())// 4194304 (cpu clock speed) /x (timer speed)
    }

    private var currentDivCycle = 0
    private var currentTimerCycle = 0
    private val timerControl = MemoryRegister("TAC", mapper, 0xFF07)
    private val timerRunning by timerControl.bitVar(2)
    private val clockSelect get()= timerControl.getValue() and 0b11

    fun step(cycles: Int) {
        currentDivCycle += cycles
        if(currentDivCycle >= DivCycleRate) {
            currentDivCycle %= DivCycleRate
            mapper.divRegister.inc()
        }

        if(timerRunning) {
            currentTimerCycle += cycles
            val timerRate = TimerCounterRates[clockSelect]
            if(currentTimerCycle >= timerRate) {
                currentTimerCycle %= timerRate
                mapper.timerRegister.inc()
            }
        }
    }
}
