package org.jglrxavpok.kameboy.memory.specialRegs.sound

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.RAM
import org.jglrxavpok.kameboy.sound.Sound

class WavePatternRam(val apu: Sound): RAM(16) {
    override fun correctAddress(address: Int): Int {
        return address-0xFF30
    }

    override val name = "Wave Pattern RAM"

    override fun write(address: Int, value: Int) {
        if(apu.channel3.isEnabled())
            return
        super.write(address, value)
    }

    fun copyTo(dest: IntArray) {
        for(i in 0..0xF) {
            dest[i] = data[i].asUnsigned()
        }
    }

    fun clear() {
        for(i in 0..0xF) {
            data[i] = (0xFF).toByte()
        }
    }
}