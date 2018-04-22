package org.jglrxavpok.kameboy.memory.specialRegs.video

import org.jglrxavpok.kameboy.memory.Register
import org.jglrxavpok.kameboy.processing.video.PaletteMemory

class CgbPaletteData(name: String, val paletteIndex: CgbPaletteIndex, val paletteMemory: PaletteMemory): Register(name) {

    override fun write(address: Int, value: Int) {
        paletteMemory.write(paletteIndex.index, value)
        if(paletteIndex.getValue() and paletteIndex.IncrementMask != 0)
            paletteIndex.incrementIndex()
    }

    override fun read(address: Int): Int {
        return paletteMemory.read(paletteIndex.index)
    }
}