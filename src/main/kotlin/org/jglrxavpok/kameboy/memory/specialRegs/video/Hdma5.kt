package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.processing.video.Video
import org.jglrxavpok.kameboy.time.SaveStateElement

class Hdma5(val memory: MemoryMapper) : Register("HDMA 5") {

    @SaveStateElement
    internal var source = 0
    @SaveStateElement
    internal var destination = 0
    @SaveStateElement
    internal var length = 0
    @SaveStateElement
    internal var index = 0
    @SaveStateElement
    internal var inHBlank = false
    @SaveStateElement
    internal var hBlankTransfer = false

    override fun write(address: Int, value: Int) {
        val immediate = value and (1 shl 7) == 0
        if(!immediate) {
            hBlankTransfer = true
        } else {
            if(hBlankTransfer) {
                hBlankTransfer = false // terminate
                return
            }
        }
        index = 0
        length = ((value and 0b0111_1111) +1) * 0x10
        val sourceHigh = memory.hdma1.getValue()
        val sourceLow = memory.hdma2.getValue() and 0b1111_0000

        val destinationHigh = memory.hdma3.getValue() and 0b0001_1111
        val destinationLow = memory.hdma4.getValue() and 0b1111_0000
        source = (sourceHigh shl 8) + sourceLow
        destination = (destinationHigh shl 8) + destinationLow + 0x8000

        if(immediate) {
            index = 0
            // Copy all at once
            repeat(length/0x10) {
                transferBlock()
            }
        }
    }

    private fun transferBlock() {
        repeat(0x10) {
            memory.write(destination+index, memory.read(source+index))
            index++
        }
    }

    fun step(cycles: Int) {
        if(!hBlankTransfer)
            return
        if(memory.gameboy.video.mode == Video.VideoMode.HBlank) {
            if(!inHBlank) {
                transferBlock()
                inHBlank = true
            }
        } else {
            inHBlank = false
        }
    }

    override fun read(address: Int): Int {
        if(!hBlankTransfer)
            return 0xFF
        return (((length-index)/0x10 -1) and 0b0111_1111)
    }
}