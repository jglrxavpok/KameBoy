package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister

class GameBoyTimer(val mapper: MemoryMapper) {

    companion object {
        val DivCycleRate = 4194304/4 / 16384 // 4194304 (cpu clock speed) /16384 (divider speed)
        val TimerCounterRates = arrayOf(4194304/4 / 4096, 4194304/4 / 262144, 4194304/4 / 65536, 4194304/4 / 16384)// 4194304 (cpu clock speed) /x (timer speed)
    }

    private var currentDivCycle = 0
    private var currentTimerCycle = 0
    private val timerControl = MemoryRegister("TAC", mapper, 0xFF07)
    private val timerRunning by timerControl.bitVar(2)
    private val clockSelect get()= timerControl.getValue() and 0b11

    private var prevTimerRate = 0

    fun step(cycles: Int) {
        currentDivCycle += cycles/4
        if(currentDivCycle >= DivCycleRate) {
            currentDivCycle %= DivCycleRate
            mapper.divRegister.inc()
        }

        if(timerRunning) {
            currentTimerCycle += cycles/4
            val timerRate = TimerCounterRates[clockSelect]

            if(clockSelect != prevTimerRate) {
                prevTimerRate = clockSelect
                currentTimerCycle = 0
            }
            if(currentTimerCycle >= timerRate) {
                currentTimerCycle %= timerRate
                mapper.timerRegister.inc()
            }
        }
    }
}
