import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent

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
            step()
        }
        val line = readLine()
        if(line == " ")
            return
        if(line?.startsWith("to:") == true) {
            val requestedPC = line.substring("to:".length).trim().toInt()
            while(cpu.programCounter.getValue() != requestedPC)
                step()
        }
        val count = line?.toIntOrNull()
        loop(count ?: 1)
    }

    private fun step() {
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
        printReg(cpu.atHL)
        println("LCDC: ${Integer.toHexString(memory.read(0xFF40))}")
        println("STAT: ${Integer.toHexString(memory.read(0xFF41))}")
        println("LY: ${Integer.toHexString(memory.read(0xFF4B))}")
        println("Instruction: ${Integer.toHexString(memory.read(cpu.programCounter.getValue()))}")
        println("========")
        emulatorCore.step()
    }

    private fun printReg(register: SingleValueMemoryComponent) {
        println("${register.name} = ${Integer.toHexString(register.getValue())}")
    }
}