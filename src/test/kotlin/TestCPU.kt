import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent
import org.jglrxavpok.kameboy.processing.CPU
import org.junit.Assert.assertEquals
import org.junit.Test

class TestCPU {

    @Test
    fun popAF() {
        val data = ByteArray(1024) { 0xFF.toByte() }

        // CART TYPE
        data[0x147] = 0x00.toByte()
        // ROM SIZE
        data[0x148] = 0x1
        // RAM SIZE
        data[0x149] = 0x0

        // JP 0x0150
        data[0x100] = 0xC3.toByte()
        data[0x101] = 0x50
        data[0x102] = 0x01

        // LD BC, 0x1200
        data[0x150] = 0x01
        data[0x151] = 0x00
        data[0x152] = 0x12

        // PUSH BC
        data[0x153] = 0xC5.toByte()

        // POP AF
        data[0x154] = 0xF1.toByte()

        // PUSH AF
        data[0x155] = 0xF5.toByte()

        // POP DE
        data[0x156] = 0xD1.toByte()

        // LD A,C
        data[0x157] = 0x79.toByte()

        // AND 0xF0
        data[0x158] = 0xE6.toByte()
        data[0x159] = 0xF0.toByte()

        // CP E
        data[0x15A] = 0xBB.toByte()

        val cart = Cartridge(data)
        val input = object: PlayerInput {
            override val buttonState: Int
                get() = 0xFF
            override val directionState: Int
                get() = 0xFF
        }
        val memory = MemoryMapper(cart, input, false)
        val cpu = CPU(memory, memory.interruptManager, cart)
        cpu.reset()
        while(cpu.programCounter.getValue() < 0x15B) {
            cpu.step()
            dumpInfos(cpu, memory)
        }
        assert(cpu.flagZ)
        assertEquals(0x15B, cpu.programCounter.getValue())
    }

    fun dumpInfos(cpu: CPU, mapper: MemoryMapper) {
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
        println("Instruction: ${Integer.toHexString(cpu.programCounter.atPointed(mapper))}")
        println("========")
    }

    private fun printReg(register: SingleValueMemoryComponent) {
        println("${register.name} = ${Integer.toHexString(register.getValue())}")
    }
}