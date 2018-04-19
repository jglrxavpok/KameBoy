package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.memory.MemoryComponent

class TileDataTable(val video: Video, val memory: MemoryComponent) {

    val currentConfiguration = TableConfiguration.Unsigned

    enum class TableConfiguration(val startAddress: Int) {
        Unsigned(0x8000),
        Signed(0x8800)
    }

}