package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.memory.MemoryMapper

open class SquareChannel(memory: MemoryMapper, channelNumber: Int): SoundChannel(channelNumber, 64, memory) {

    companion object {
        val Duty = arrayOf(0b00001000, 0b00001100, 0b00111100, 0b11110111)//arrayOf(0b00000001, 0b10000001, 0b10000111, 0b01111110)
        val High = 0b1111
        val Low = 0b0000
    }
    val dutySelect get()= (nr1.getValue() shr 6) and 0b11
    private var dutyBitSelect = 0
    override val frequencyMultiplier = 4

    override fun onOutputClock(timer: Timer) {
        val dutyBit = Duty[dutySelect] and (1 shl dutyBitSelect) != 0
        dutyBitSelect++
        dutyBitSelect %= 8
        output(if(dutyBit) correctVolume else Low)
    }

    override fun channelStep(cycles: Int) { }

    override fun reset() {
        super.reset()
        //dutyBitSelect = 0
    }
}
