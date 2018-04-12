package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.SerialIO

class SerialControllerRegister(val serialIO: SerialIO): Register("SC") {

    val transferring by bitVar(7)
}