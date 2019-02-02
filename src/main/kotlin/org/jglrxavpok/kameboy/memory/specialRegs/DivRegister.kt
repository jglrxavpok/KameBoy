package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register

class DivRegister(val mapper: MemoryMapper): Register("DIV", registerValue = 0, sizeInBits = 16) {

    private val timerControl = MemoryRegister("TAC", mapper, 0xFF07)
    private val timerRunning by timerControl.bitVar(2)
    private val clockSelect get()= timerControl.getValue() and 0b11

    override fun write(address: Int, value: Int) {
        setValue(0)
        mapper.sound.resetFromDiv()
        mapper.gameboy.timer.resetTimerFromDiv()
    }

    override fun inc(): Register {
        val prevFullValue = registerValue
        val prevValue = prevFullValue shr 8
        super.inc()
        val newValue = registerValue shr 8
        var fallingEdgeMask =
            if(mapper.currentSpeedFactor != 1) { // double speed mode
                1 shl 6
            } else {
                1 shl 5
            }
        if(prevValue and fallingEdgeMask != 0 && newValue and fallingEdgeMask == 0) { // falling edge of bit 5 or 6
            mapper.sound.update()
        }


        if(timerRunning) {
            val clockBit = when(clockSelect) {
                0 -> 9
                1 -> 3
                2 -> 5
                3 -> 7
                else -> 9 // should never happen
            }
            fallingEdgeMask = 1 shl clockBit
            if(prevFullValue and fallingEdgeMask != 0 && registerValue and fallingEdgeMask == 0) {
                mapper.timerRegister.inc()
            }
        }

        return this
    }

    override fun read(address: Int): Int {
        return super.read(address) shr 8 // MSB of counter
    }
}