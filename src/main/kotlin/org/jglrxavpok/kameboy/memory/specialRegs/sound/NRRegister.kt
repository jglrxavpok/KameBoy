package org.jglrxavpok.kameboy.memory.specialRegs.sound

open class NRRegister(channelNumber: Int, registerIndex: Int, orValue: Int): OrOnReadRegister("NR$channelNumber$registerIndex", orValue) {

}
