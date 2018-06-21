package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent
import org.jglrxavpok.kameboy.processing.CPU
import org.jglrxavpok.kameboy.processing.GameBoyTimer
import org.jglrxavpok.kameboy.processing.video.Video
import org.jglrxavpok.kameboy.time.CreateSaveState
import org.jglrxavpok.kameboy.time.SaveState
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.concurrent.scheduleAtFixedRate

open class EmulatorCore(val cartridge: Cartridge, val input: PlayerInput, val outputSerial: Boolean = false, val renderRoutine: EmulatorCore.(IntArray) -> Unit) {
    companion object {
        val CpuClockSpeed = 4194304 // Clock cycles / second
        val DMGVideoVSync = 59.73 // updates per second
        val CGBVideoVSync = 61.1 // updates per second
    }

    val gameboy = Gameboy(cartridge, input, outputSerial)

    @JvmField
    val clockCyclesPerFrame = CpuClockSpeed / (if(cartridge.isForColorGB) CGBVideoVSync else DMGVideoVSync)

    private var saveStateToLoad: SaveState? = null
    private val saveStates = Array<SaveState?>(11) { null }

    fun frame(catchupSpeed: Double = 1.0) {
        var totalClockCycles = 0
        while(totalClockCycles < clockCyclesPerFrame*catchupSpeed) {
            val clockCycles = step()
            totalClockCycles += clockCycles
        }
        renderRoutine(gameboy.video.pixelData)

        if(saveStateToLoad != null) {
            saveStateToLoad!!.load()
            saveStateToLoad = null
        }
    }

    fun step(): Int {
        return gameboy.step()
    }

    fun init() {
        if(!cartridge.hasBootRom) {
            gameboy.reset()
        } else {
            println("Found BOOT Rom, loading it")
        }
    }

    private lateinit var task: TimerTask

    fun loop() {
        val timer = Timer()
        task = timer.scheduleAtFixedRate(0, 16) {
            frame()
        }
    }

    fun stop() {
        task.cancel()
    }

    fun dumpInfos() {
        println("========")
        if(gameboy.cpu.halted) {
            println("***CPU HALTED***")
        }
        if(gameboy.cpu.stopped) {
            println("***!!CPU STOPPED!!***")
        }
        printReg(gameboy.cpu.AF)
        println("Z: ${gameboy.cpu.flagZ}")
        println("N: ${gameboy.cpu.flagN}")
        println("H: ${gameboy.cpu.flagH}")
        println("C: ${gameboy.cpu.flagC}")
        printReg(gameboy.cpu.BC)
        printReg(gameboy.cpu.DE)
        printReg(gameboy.cpu.HL)
        printReg(gameboy.cpu.stackPointer)
        printReg(gameboy.cpu.programCounter)
        try {
            printReg(gameboy.cpu.atHL)
        } catch (e: Exception) {
            println("(HL) INVALID ADDRESS (${e.message})")
        }
        println("LCDC: ${Integer.toHexString(gameboy.mapper.read(0xFF40))}")
        println("STAT: ${Integer.toHexString(gameboy.mapper.read(0xFF41))}")
        println("LY: ${Integer.toHexString(gameboy.mapper.read(0xFF44))}")
        println("Input: ${Integer.toHexString(gameboy.mapper.read(0xFF00))}")
        println("Instruction: ${Integer.toHexString(gameboy.cpu.programCounter.atPointed(gameboy.mapper))}")
        println("PC (decimal): ${gameboy.cpu.programCounter.getValue()}")
        println("Cartridge info: $cartridge")
        println("IF: ${Integer.toBinaryString(gameboy.mapper.read(0xFF0F))}")
        println("IE: ${Integer.toBinaryString(gameboy.mapper.read(0xFFFF))}")
        println("IME: ${gameboy.cpu.interruptManager.interruptsEnabled}")
        println("========")
    }

    private fun printReg(register: SingleValueMemoryComponent) {
        println("${register.name} = ${Integer.toHexString(register.getValue())}")
    }

    fun showBGMap() {
        val data = IntArray(256*256)
        val video = gameboy.video
        for(row in 0..144) {
            for(x in 0..15) {
                val tileNumber = x+(row/8)*16
                val offset = tileNumber
                val tileAddress = video.tileDataAddress + (if(video.dataSelect) offset else offset.asSigned8())*0x10
                video.drawTileRow(x*8, row, row%8, tileAddress, video.bgPalette, target = data)
            }
        }
        val result = BufferedImage(256,256, BufferedImage.TYPE_INT_ARGB)
        result.setRGB(0, 0, 256, 256, data, 0, 256)
        val frame = JFrame("Video Result")
        frame.add(JLabel(ImageIcon(result)))
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isVisible = true
    }

    fun createSaveState(index: Int) {
        saveStates[index] = CreateSaveState(gameboy)
        println("Save SaveState #$index")
        // TODO: Show message
    }

    fun loadSaveState(index: Int) {
        val state = saveStates[index] ?: return
        println("Loading SaveState #$index")
        // TODO: show message
        saveStateToLoad = state
    }

}