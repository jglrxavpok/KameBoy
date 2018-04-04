package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Register

class P1Register(val input: PlayerInput): Register("P1") {

    var selectDirectionKeys = false
    var selectButtonKeys = false

    override fun getValue(): Int {
        val selection = when {
            selectButtonKeys && selectDirectionKeys -> 0x30
            selectButtonKeys -> 0x20
            else -> 0x10
        }
        val pressState = if(selectButtonKeys) input.buttonState else if(selectDirectionKeys) input.directionState else 0xFF
        return (0xC0 or selection) or pressState
    }

    override fun setValue(value: Int) {
        super.setValue(value)
        selectButtonKeys = value and 0x20 != 0
        selectDirectionKeys = value and 0x10 != 0
    }
}