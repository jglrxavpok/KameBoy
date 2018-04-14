import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun main(args: Array<String>) {
    val cartridge = Cartridge(rom("Kirby's Dream Land.gb"))
    val input = object: PlayerInput {
        override val buttonState = 0xF
        override val directionState = 0xF
    }
    val core = EmulatorCore(cartridge, input) {}
    StepByStepExecution(core).loop()
}

private fun rom(name: String) = StepByStepExecution::class.java.getResourceAsStream("/roms/$name").use { it.readBytes() }

class StepByStepExecution(val emulatorCore: EmulatorCore) {

    val cpu = emulatorCore.cpu
    val memory = emulatorCore.mapper

    init {
        emulatorCore.init()
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
                            video.drawTileRow(x*8, row, row%8, tileAddress, video.bgPaletteData, target = data)
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
            val data = line.substring("to:".length).trim()
            if("x" in data) {
                val params = data.split("x")
                val times = params[1].toInt()
                val requestedPC = params[0].toInt()
                repeat(times) {
                    while(cpu.programCounter.getValue() != requestedPC)
                        step(mute = true)
                    step(mute = false)
                }
                println("Arrived!")
            } else {
                val requestedPC = data.toInt()
                while(cpu.programCounter.getValue() != requestedPC)
                    step(mute = true)
                println("Arrived!")
            }
        }
        if(line?.startsWith("ly:") == true) {
            val data = line.substring("to:".length).trim()
            val requestedLY = data.toInt()
            while(memory.read(0xFF44) != requestedLY)
                step(mute = true)
            println("Arrived!")
        }
        val count = line?.toIntOrNull()
        loop(count ?: 1)
    }

    private fun step(mute: Boolean) {
        if(!mute) {
            emulatorCore.dumpInfos()
        }
        emulatorCore.step()
    }
}