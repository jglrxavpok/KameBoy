package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.Register

class SoundRegister(address: Int, orValue: Int = 0xFF): OrOnReadRegister(Integer.toHexString(address), orValue)