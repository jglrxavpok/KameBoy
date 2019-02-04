package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.helpful.asUnsigned16
import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register

class TimerRegister(val memoryMapper: MemoryMapper): Register("TIMA") {

    private val timerControl = MemoryRegister("TAC", memoryMapper, 0xFF07)
    private val timerRunning by timerControl.bitVar(2)
    private var cycles = 0
    private var resetPending = false

    override fun inc(): Register {
        if(registerValue.asUnsigned16() == 0xFF) {
            registerValue = 0
            resetPending = true
            return this
        }
        return super.inc()
    }

    fun step(cycles: Int) {
        if(!timerRunning)
            return
        this.cycles += cycles
        if(this.cycles >= 4) {
            this.cycles %= 4
            if(resetPending) {
                memoryMapper.interruptManager.fireTimerOverflow()
                registerValue = memoryMapper.read(0xFF06)
                resetPending = false
            }
        }
    }
}