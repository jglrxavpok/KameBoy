import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.asUnsigned8
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

class TestInternalArithmetics {

    @Test
    fun testConversions() {
        assertEquals(0xFF, (-1).asUnsigned8())
        assertEquals(-1, (0xFF).asSigned8())
        assertEquals(0xFF, (0b11111111.toByte()).asUnsigned())
        assertEquals((-1).toByte(), (-1).asUnsigned8().toByte())
    }

}