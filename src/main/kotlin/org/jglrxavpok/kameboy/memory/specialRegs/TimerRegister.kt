package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.helpful.asUnsigned16
import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register

class TimerRegister(val memoryMapper: MemoryMapper): Register("TIMA") {

    private val timerControl = MemoryRegister("TAC", memoryMapper, 0xFF07)
    private val timerRunning by timerControl.bitVar(2)

    override fun fitValueInBounds() {
        if(timerRunning) {
            if(registerValue.asUnsigned16() > 0xFF) {
                memoryMapper.interruptManager.fireTimerOverflow()
                registerValue = memoryMapper.read(0xFF06)
            }
        }
        registerValue = registerValue and 0xFF
    }
}