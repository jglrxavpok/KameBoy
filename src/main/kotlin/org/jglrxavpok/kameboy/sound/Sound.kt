package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.memory.specialRegs.sound.*

class Sound(val memory: MemoryMapper) {

    val channel1 = Square1Channel(memory)
    val channel2 = SquareChannel(memory, 2)
    val channel3 = Wave3Channel(memory)
    val channel4 = Noise4Channel(memory)
    val channels = arrayOf(channel1, channel2, channel3, channel4)

    val channelControl = NRRegister(5, 0, 0x00, this)
    val outputSelect = NRRegister(5, 1, 0x00, this)
    val soundToggle = SoundToggleRegister(this)

    fun channel(index: Int) = channels[index-1]

    fun step(cycles: Int) {
        if(!isOn())
            return
        for(index in 1..4) {
            channel(index).step(cycles)
        }

        // TODO: OUTPUT
    }

    fun isOn(): Boolean {
        return soundToggle.isOn
    }
}