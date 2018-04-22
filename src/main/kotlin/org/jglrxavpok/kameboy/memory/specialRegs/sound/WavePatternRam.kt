package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.sound.Sound

class WavePatternRam(val apu: Sound): MemoryComponent {

    private val data = IntArray(16)

    override val name = "Wave Pattern RAM"

    override fun write(address: Int, value: Int) {
        if(apu.channel3.isEnabled())
            return
        data[address-0xFF30] = value
    }

    override fun read(address: Int): Int {
        return data[address-0xFF30]
    }

    fun copyTo(dest: IntArray) {
        for(i in 0..0xF) {
            dest[i] = data[i]
        }
    }

    fun clear() {
        for(i in 0..0xF) {
            data[i] = 0xFF
        }
    }
}