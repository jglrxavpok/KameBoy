package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register

class DivRegister(val mapper: MemoryMapper): Register("DIV") {

    override fun write(address: Int, value: Int) {
        setValue(0)
        mapper.sound.resetFromDiv()
    }
}