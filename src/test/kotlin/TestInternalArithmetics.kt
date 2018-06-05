import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.junit.Assert.assertEquals
import org.junit.Test

class TestInternalArithmetics {

    @Test
    fun testConversions() {
        assertEquals(0xFF, (-1).asUnsigned8())
        assertEquals(-1, (0xFF).asSigned8())
        assertEquals(0xFF, (0b11111111.toByte()).asUnsigned())
        assertEquals((-1).toByte(), (-1).asUnsigned8().toByte())
    }

}