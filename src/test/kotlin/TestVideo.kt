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

class TestVideo {

    @Test
    fun test0() {
        val cartridge = Cartridge(rom("Tetris.gb"))
        val mapper = MemoryMapper(cartridge)
        val cpu = CPU(mapper, mapper.interruptManager)
        val video = Video(mapper, mapper.interruptManager)
        cpu.reset()
        repeat(1000*1000) {
            cpu.step()
        }
        repeat(150) {
            video.scanLine()
        }

        val result = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val rgb = video.pixelData
        result.setRGB(0, 0, 256, 256, rgb, 0, 256)

        val frame = JFrame("Video Result")
        frame.add(JLabel(ImageIcon(result)))
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isVisible = true
        Thread.sleep(500)
    }

    private fun rom(name: String) = javaClass.getResourceAsStream("/roms/$name").use { it.readBytes() }
}