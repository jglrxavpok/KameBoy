package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.processing.Sound

class SoundFreqHighRegister(memory: MemoryComponent, name: String, val sound: Sound, address: Int, val soundNumber: Int): MemoryRegister(name, memory, address) {

    val InitialMask = 1 shl 7

    override fun write(address: Int, value: Int) {
        super.write(address, value and 0b1111111)
        if(value and InitialMask != 0) {
            sound.resetSound(soundNumber)
        }
    }
}