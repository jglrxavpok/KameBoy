package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.time.SaveStateElement

class SpeedRegister(val memoryMapper: MemoryMapper): Register("GBC Speed") {

    @SaveStateElement
    internal var prepareSpeedSwitch = 0
    val shouldPrepareSwitch get()= prepareSpeedSwitch == 1

    override fun read(address: Int): Int {
        return ((memoryMapper.currentSpeedFactor-1) shl 7) or prepareSpeedSwitch
    }

    override fun write(address: Int, value: Int) {
        prepareSpeedSwitch = value and 0x1
    }
}