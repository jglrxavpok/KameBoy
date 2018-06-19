package org.jglrxavpok.kameboy.time

import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.memory.RAM
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent
import org.jglrxavpok.kameboy.processing.video.Video

class SaveState internal constructor(val gameboy: Gameboy) {

    val registerAF: Int = gameboy.cpu.AF.getValue()
    val registerBC: Int = gameboy.cpu.BC.getValue()
    val registerDE: Int = gameboy.cpu.DE.getValue()
    val registerHL: Int = gameboy.cpu.HL.getValue()
    val registerSP: Int = gameboy.cpu.stackPointer.getValue()
    val registerPC: Int = gameboy.cpu.programCounter.getValue()
    val memoryContents = ByteArray(0xFFFF+1)
    val ramBanks: Array<ByteArray>
    val videoMode: Video.VideoMode
    val videoCycle: Int
    val ramIndex: Int
    val cartControllerData: Any
    val currentSpeed: Int
    val internalRamData: Array<ByteArray>

    init {
        for(address in 0x0..0xFFFF) {
            val component = gameboy.mapper.map(address)
            if(component is SingleValueMemoryComponent) {
                memoryContents[address] = component.getValue().toByte()
            } else {
            //    memoryContents[address] = component.read(address).toByte()
            }
        }
        val cartRamBanks = gameboy.cartridge.ramBanks
        ramIndex = gameboy.cartridge.selectedRAMBankIndex
        ramBanks = Array(cartRamBanks.size) { index ->
            val bank = cartRamBanks[index]
            bank.data.copyOf()
        }

        val ppu = gameboy.video
        videoMode = ppu.mode
        videoCycle = ppu.currentClockCycles

        val memory = gameboy.mapper
        val wramMemoryBanks = memory.wramBanks
        internalRamData = copyRam(*wramMemoryBanks, memory.internalRAM, memory.vram0, memory.vram1, memory.highRAM,
                memory.wavePatternRam, memory.backgroundPaletteMemory, memory.spritePaletteMemory, memory.spriteAttributeTable)

        currentSpeed = gameboy.mapper.currentSpeedFactor

        cartControllerData = gameboy.cartridge.cartrigeType.createSaveStateData()
    }

    fun load() {
        val cpu = gameboy.cpu
        val ppu = gameboy.video
        val memory = gameboy.mapper
        cpu.AF.setValue(registerAF)
        cpu.BC.setValue(registerBC)
        cpu.DE.setValue(registerDE)
        cpu.HL.setValue(registerHL)
        cpu.stackPointer.setValue(registerSP)
        cpu.programCounter.setValue(registerPC)

        for(address in 0x0000..0xFFFF) {
            val component = gameboy.mapper.map(address)
            if(component is SingleValueMemoryComponent) {
                component.setValue(memoryContents[address].asUnsigned())
            } else {
               // component.write(address, memoryContents[address].asUnsigned())
            }
        }

        val cartRamBanks = gameboy.cartridge.ramBanks

        ramBanks.forEachIndexed { bankIndex, bytes ->
            for(index in bytes.indices) {
                cartRamBanks[bankIndex].data[index] = bytes[index]
            }
        }
        gameboy.cartridge.selectedRAMBankIndex = ramIndex
        val wramMemoryBanks = gameboy.mapper.wramBanks
        ppu.mode = videoMode
        ppu.currentClockCycles = videoCycle

        gameboy.mapper.currentSpeedFactor = currentSpeed

        loadRam(*wramMemoryBanks, memory.internalRAM, memory.vram0, memory.vram1, memory.highRAM,
                memory.wavePatternRam, memory.backgroundPaletteMemory, memory.spritePaletteMemory, memory.spriteAttributeTable)

        gameboy.cartridge.cartrigeType.loadSaveStateData(cartControllerData)

        // TODO: TIMERS

        memory.spriteAttributeTable.reloadSprites()
    }

    fun copyRam(vararg banks: RAM): Array<ByteArray> {
        val count = banks.size
        return Array(count) { index ->
            val bank = banks[index]
            bank.data.copyOf()
        }
    }

    fun loadRam(vararg banks: RAM) {
        banks.forEachIndexed { ramIndex, ram ->
            for(index in ram.data.indices) {
                ram.data[index] = internalRamData[ramIndex][index]
            }
        }
    }

}

fun CreateSaveState(gameboy: Gameboy) = SaveState(gameboy)