package org.jglrxavpok.kameboy.memory.specialRegs

import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister
import org.jglrxavpok.kameboy.memory.Register

class LYRegister(val memory: MemoryMapper): Register("LY") {

    private val lyCompare = MemoryRegister("LYC", memory, 0xFF45)
    private val lcdStatus = MemoryRegister("LCDC Status", memory, 0xFF41)
    private var coincidenceInterrupt by lcdStatus.bitVar(6)
    private var coincidenceFlag by lcdStatus.bitVar(2)

    override fun write(address: Int, value: Int) {
        setValue(0)
    }

    override fun setValue(line: Int) {
        super.setValue(line)
        if(line == lyCompare.getValue()) {
            coincidenceFlag = true
            if(coincidenceInterrupt)
                memory.interruptManager.fireLcdStat()
        } else {
            coincidenceFlag = false
        }
    }
}