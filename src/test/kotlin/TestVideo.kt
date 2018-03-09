import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.processing.CPU
import org.jglrxavpok.kameboy.processing.Video
import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities

class TestVideo {

    @Test
    fun test0() {
        val cartridge = Cartridge(rom("Tetris.gb"))
        var frameCount = 0
        var pressedUp = false
        val input = object: PlayerInput {
            override val state: Int
                get() = if(pressedUp) 0x0 else 0xF
        }
        val core = EmulatorCore(cartridge, input) { rgb ->
            frameCount++
            if(frameCount > 60) {
                val result = BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB)
                result.setRGB(0, 0, 256, 256, rgb, 0, 256)
                val frame = JFrame("Video Result")
                frame.add(JLabel(ImageIcon(result)))
                frame.pack()
                frame.setLocationRelativeTo(null)
                frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                frame.isVisible = true
                if(!pressedUp) {
                    pressedUp = true
                    this.mapper.interruptManager.firePinReleased()
                    frameCount = 0
                } else {
                    this.stop()
                }
            }
        }
        core.loop()

        Thread.sleep(50000)
    }

    private fun rom(name: String) = javaClass.getResourceAsStream("/roms/$name").use { it.readBytes() }
}