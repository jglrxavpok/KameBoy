import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun main(args: Array<String>) {
    val cartridge = Cartridge(rom("Tetris.gb"))
    val core = EmulatorCore(cartridge) {}
    StepByStepExecution(core).loop()
}

private fun rom(name: String) = StepByStepExecution::class.java.getResourceAsStream("/roms/$name").use { it.readBytes() }

class StepByStepExecution(val emulatorCore: EmulatorCore) {

    val cpu = emulatorCore.cpu
    val memory = emulatorCore.mapper

    init {
        emulatorCore.cpu.reset()
    }

    tailrec fun loop(stepcount: Int = 1) {
        repeat(stepcount) {
            step(mute = false)
        }
        val line = readLine()
        if(line == " ")
            return
        if(line != null) {
            val rgb = when {
                "pic" in line -> {
                    emulatorCore.video.pixelData
                }

                "bgmap" in line -> {
                    val data = IntArray(16*32 * 16*32)
                    val video = emulatorCore.video
                    for(row in 0..(15*16)) {
                        for(x in 0..15) {
                            val tileNumber = x+(row/8)*16
                            val offset = tileNumber * 0x10
                            val tileAddress = video.tileDataAddress + offset
                            video.drawTileRow(x*8, row, tileAddress, video.bgPaletteData, target = data)
                        }
                    }
                    data
                }

                else -> null
            }

            if(rgb != null) {
                val result = BufferedImage(256,256, BufferedImage.TYPE_INT_ARGB)
                result.setRGB(0, 0, 256, 256, rgb, 0, 256)
                val frame = JFrame("Video Result")
                frame.add(JLabel(ImageIcon(result)))
                frame.pack()
                frame.setLocationRelativeTo(null)
                frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                frame.isVisible = true
            }
        }
        if(line?.startsWith("to:") == true) {
            val requestedPC = line.substring("to:".length).trim().toInt()
            while(cpu.programCounter.getValue() != requestedPC)
                step(mute = true)
            println("Arrived!")
        }
        val count = line?.toIntOrNull()
        loop(count ?: 1)
    }

    private fun step(mute: Boolean) {
        if(!mute) {
            println("========")
            printReg(cpu.AF)
            println("Z: ${cpu.flagZ}")
            println("N: ${cpu.flagN}")
            println("H: ${cpu.flagH}")
            println("C: ${cpu.flagC}")
            printReg(cpu.BC)
            printReg(cpu.DE)
            printReg(cpu.HL)
            printReg(cpu.stackPointer)
            printReg(cpu.programCounter)
            try {
                printReg(cpu.atHL)
            } catch (e: Exception) {
                println("(HL) INVALID ADDRESS (${e.message})")
            }
            println("LCDC: ${Integer.toHexString(memory.read(0xFF40))}")
            println("STAT: ${Integer.toHexString(memory.read(0xFF41))}")
            println("LY: ${Integer.toHexString(memory.read(0xFF44))}")
            println("Input: ${Integer.toHexString(memory.read(0xFF00))}")
            println("Instruction: ${Integer.toHexString(memory.read(cpu.programCounter.getValue()))}")
            println("========")
        }
        emulatorCore.step()
    }

    private fun printReg(register: SingleValueMemoryComponent) {
        println("${register.name} = ${Integer.toHexString(register.getValue())}")
    }
}