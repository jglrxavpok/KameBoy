package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Register

class P1Register(val input: PlayerInput): Register("P1") {

    var selectDirectionKeys = false
    var selectButtonKeys = false

    override fun getValue(): Int {
        val selection = when {
            selectDirectionKeys -> 0x10
            selectButtonKeys -> 0x20
            else -> 0x00
        }
        val pressState = if(selectButtonKeys) input.buttonState else if(selectDirectionKeys) input.directionState else 0xF
        return (0xC0 or selection) or (pressState and 0xF)
    }

    override fun setValue(value: Int) {
        //super.setValue(value)
        selectButtonKeys = value and 0x20 == 0
        selectDirectionKeys = value and 0x10 == 0
    }
}