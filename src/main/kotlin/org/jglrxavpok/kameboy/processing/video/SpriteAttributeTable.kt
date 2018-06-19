package org.jglrxavpok.kameboy.processing.video

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.memory.MemoryComponent
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.RAM

class SpriteAttributeTable: RAM(0xFEA0-0xFE00) {

    val startAddress = 0xFE00

    var sprites = Array(40) { index ->
        Sprite(index)
    }
        private set
    override val name = "OAM"

    override fun correctAddress(address: Int) = address-startAddress

    fun reloadSprites() {
        sprites = Array(40) { index ->
            Sprite(index)
        }
    }

    inner class Sprite(index: Int): Comparable<Sprite> {
        val startAddress = this@SpriteAttributeTable.startAddress + index * 4
        val positionY = MemoryRegister("Sprite$index Y", this@SpriteAttributeTable, startAddress)
        val positionX = MemoryRegister("Sprite$index X", this@SpriteAttributeTable, startAddress+1)
        val tileNumber = MemoryRegister("Sprite$index Tile Number", this@SpriteAttributeTable, startAddress+2)
        val attributes = MemoryRegister("Sprite$index Attributes", this@SpriteAttributeTable, startAddress+3)

        val dmgPaletteNumber by attributes.bitVar(4)
        val hMirror by attributes.bitVar(5)
        val vMirror by attributes.bitVar(6)
        val priority by attributes.bitVar(7)

        val visible get()= positionX.getValue() != 0 && positionY.getValue() != 0
        val cgbPaletteNumber get()= attributes.getValue() and 0b111
        val inVram1 by attributes.bitVar(3)

        override fun compareTo(other: Sprite): Int {
            if(!overlaps(other))
                return 0

            val x = positionX.getValue()
            val otherX = other.positionX.getValue()
            // the sprites with the highest priority are sent at the end of the collection during a sort
            // so that they appear above others during rendering
            if(x < otherX)
                return 1
            if(x > otherX)
                return -1
            return -startAddress.compareTo(other.startAddress)
        }

        private fun overlaps(other: Sprite): Boolean {
            val x = positionX.getValue()
            val y = positionY.getValue()

            val otherX = other.positionX.getValue()
            val otherY = other.positionY.getValue()

            if(x+8 < otherX)
                return false
            if(otherX+8 < x)
                return false
            if(y+8 < otherY)
                return false
            if(otherY+8 < y)
                return false
            return true
        }
    }

}