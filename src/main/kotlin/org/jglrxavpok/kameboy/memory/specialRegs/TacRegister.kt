package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.memory.Register

class TacRegister(val gameboy: Gameboy): Register("TAC") {

    override fun write(address: Int, value: Int) {
        val oldValue = read(address)
        if((oldValue and 0x3) != (value and 0x3)) { // new frequency
            gameboy.timer.resetTimer()
        }
        super.write(address, value)
    }
}