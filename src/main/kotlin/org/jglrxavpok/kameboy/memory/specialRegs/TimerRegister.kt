package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.InterruptManager
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register

class TimerRegister(val memoryMapper: MemoryMapper): Register("TIMA") {

    override fun fitValueInBounds() {
        if(registerValue > 0xFF) {
            memoryMapper.interruptManager.fireTimerOverflow()
            registerValue = memoryMapper.read(0xFF06)
        }
        registerValue = registerValue and 0xFF
    }
}