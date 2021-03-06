import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class TestCartridges {

    @Test
    fun testInfo() {
        val cartridge = Cartridge(rom("Tetris.gb"))
        assertEquals("TETRIS", cartridge.title)
        assertEquals("ROM Only", cartridge.cartrigeType.name)
    }

    @Test
    fun correctIOPortsAlignment() {
        val cartridge = Cartridge(rom("Tetris.gb"))
        val gameboy = Gameboy(cartridge, object: PlayerInput {
            override val buttonState: Int
            get() = 0xF
            override val directionState: Int
            get() = 0xF
        }, false)
        val mapper = MemoryMapper(gameboy)
        assertEquals("WX", mapper.map(0xFF4B).name)
        assertEquals("LCDC", mapper.map(0xFF40).name)
        assertEquals("LY", mapper.map(0xFF44).name)
        assertEquals("SCY", mapper.map(0xFF42).name)
        assertEquals("SCX", mapper.map(0xFF43).name)
        assertEquals("NR52", mapper.map(0xFF26).name)
        assertEquals("NR51", mapper.map(0xFF25).name)
    }

    private fun rom(name: String) = javaClass.getResourceAsStream("/roms/$name").use { it.readBytes() }
}