package org.jglrxavpok.kameboy.memory

import java.io.File

interface Battery {
    fun loadRAM(cartridge: Cartridge)

    fun saveRAM(cartridge: Cartridge)
}

object NoBattery: Battery {
    override fun loadRAM(cartridge: Cartridge) { }
    override fun saveRAM(cartridge: Cartridge) { }
}

class FileBasedBattery(val backingFile: File): Battery {
    override fun loadRAM(cartridge: Cartridge) {
        println("Loading RAM from battery")
        if(backingFile.exists()) {
            val contents = backingFile.readBytes()
            tailrec fun fillRAM(index: Int, offset: Int): Unit = when(index) {
                cartridge.ramBankCount -> Unit
                else -> {
                    val bank = cartridge.ramBanks[index]
                    System.arraycopy(contents, offset, bank.data, 0, 0x2000)
                    fillRAM(index+1, offset+0x2000)
                }
            }
            fillRAM(0, 0)
        }
    }

    override fun saveRAM(cartridge: Cartridge) {
        println("Saving RAM to battery")
        backingFile.createNewFile()
        val out = backingFile.outputStream().buffered()
        out.use {
            for (bank in cartridge.ramBanks) {
                out.write(bank.data)
            }
            out.flush()
        }
    }

}