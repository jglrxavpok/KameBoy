package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Register

class P1Register(val input: PlayerInput): Register("P1") {

    var selectDirectionKeys = true
    var selectButtonKeys = true

    override fun read(address: Int): Int {
        val selection = when {
            selectButtonKeys && selectDirectionKeys -> 0x00
            selectDirectionKeys -> 0x10
            selectButtonKeys -> 0x20
            else -> 0x00
        }
        val pressState = when {
            selectButtonKeys && selectDirectionKeys -> 0xCF
            selectButtonKeys -> input.buttonState
            selectDirectionKeys -> input.directionState
            else -> 0xF
        }
        return 0b1100_0000 or (0xC0 or selection) or (pressState and 0xF)
    }

    override fun write(address: Int, value: Int) {
        selectButtonKeys = value and 0x20 == 0
        selectDirectionKeys = value and 0x10 == 0
    }
}