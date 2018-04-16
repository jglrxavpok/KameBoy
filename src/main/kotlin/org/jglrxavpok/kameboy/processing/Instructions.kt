package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.asAddress
import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.memory.MemoryComponent

object Instructions {

    enum class AreaType {
        Ram, Rom, IO, SpecialRegister, VideoRam
    }

    data class InstructionData(val desc: String, val size: Int, val areaType: AreaType)

    fun readInstruction(memory: MemoryComponent, address: Int, areaType: AreaType): InstructionData {
        return when(areaType) {
            AreaType.IO -> InstructionData("I/O "+Integer.toHexString(address).toUpperCase()+" = ${toHexString(memory.read(address), 2)}", 1, areaType)
            AreaType.SpecialRegister -> InstructionData("Special Register "+Integer.toHexString(address).toUpperCase()+" = ${toHexString(memory.read(address), 2)}", 1, areaType)
            AreaType.VideoRam -> InstructionData("VRAM "+Integer.toHexString(address).toUpperCase()+" = ${toHexString(memory.read(address), 2)}", 1, areaType)
            AreaType.Ram, AreaType.Rom -> {
                val (desc, size) = extract(memory, address)
                InstructionData(desc, size, areaType)
            }
        }
    }

    private fun extract(memory: MemoryComponent, address: Int): Pair<String, Int> {
        val opcode = memory.read(address)
        var argIndex = 0
        fun nextByte() = memory.read(address+(argIndex++))

        fun nextAddress(): Int {
            val low = nextByte().asUnsigned8()
            val high = nextByte().asUnsigned8()

            return ((high shl 8) or low).asAddress()
        }

        return when(opcode) {
            0x06 -> Pair("LD B,${toHexString(nextByte(), 2)}", 2)
            0x0E -> Pair("LD C,${toHexString(nextByte(), 2)}", 2)
            0x16 -> Pair("LD D,${toHexString(nextByte(), 2)}", 2)
            0x1E -> Pair("LD E,${toHexString(nextByte(), 2)}", 2)
            0x26 -> Pair("LD H,${toHexString(nextByte(), 2)}", 2)
            0x2E -> Pair("LD L,${toHexString(nextByte(), 2)}", 2)

            0x7F -> Pair("LD A,A", 1)
            0x78 -> Pair("LD A,B", 1)
            0x79 -> Pair("LD A,C", 1)
            0x7A -> Pair("LD A,D", 1)
            0x7B -> Pair("LD A,E", 1)
            0x7C -> Pair("LD A,H", 1)
            0x7D -> Pair("LD A,L", 1)
            0x7E -> Pair("LD A,(HL)", 1)

            0x0A -> Pair("LD A,(BC)", 1)
            0x1A -> Pair("LD A,(DE)", 1)


            0x40 -> Pair("LD B,B", 1)
            0x41 -> Pair("LD B,C", 1)
            0x42 -> Pair("LD B,D", 1)
            0x43 -> Pair("LD B,E", 1)
            0x44 -> Pair("LD B,H", 1)
            0x45 -> Pair("LD B,L", 1)
            0x46 -> Pair("LD B,(HL)", 1)

            0x48 -> Pair("LD C,B", 1)
            0x49 -> Pair("LD C,C", 1)
            0x4A -> Pair("LD C,D", 1)
            0x4B -> Pair("LD C,E", 1)
            0x4C -> Pair("LD C,H", 1)
            0x4D -> Pair("LD C,L", 1)
            0x4E -> Pair("LD C,(HL)", 1)

            0x50 -> Pair("LD D,B", 1)
            0x51 -> Pair("LD D,C", 1)
            0x52 -> Pair("LD D,D", 1)
            0x53 -> Pair("LD D,E", 1)
            0x54 -> Pair("LD D,H", 1)
            0x55 -> Pair("LD D,L", 1)
            0x56 -> Pair("LD D,(HL)", 1)

            0x58 -> Pair("LD E,B", 1)
            0x59 -> Pair("LD E,C", 1)
            0x5A -> Pair("LD E,D", 1)
            0x5B -> Pair("LD E,E", 1)
            0x5C -> Pair("LD E,H", 1)
            0x5D -> Pair("LD E,L", 1)
            0x5E -> Pair("LD E,(HL)", 1)

            0x60 -> Pair("LD H,B", 1)
            0x61 -> Pair("LD H,C", 1)
            0x62 -> Pair("LD H,D", 1)
            0x63 -> Pair("LD H,E", 1)
            0x64 -> Pair("LD H,H", 1)
            0x65 -> Pair("LD H,L", 1)
            0x66 -> Pair("LD H,(HL)", 1)

            0x68 -> Pair("LD L,B", 1)
            0x69 -> Pair("LD L,C", 1)
            0x6A -> Pair("LD L,D", 1)
            0x6B -> Pair("LD L,E", 1)
            0x6C -> Pair("LD L,H", 1)
            0x6D -> Pair("LD L,L", 1)
            0x6E -> Pair("LD L,(HL)", 1)

            0x70 -> Pair("LD (HL),B", 1)
            0x71 -> Pair("LD (HL),C", 1)
            0x72 -> Pair("LD (HL),D", 1)
            0x73 -> Pair("LD (HL),E", 1)
            0x74 -> Pair("LD (HL),H", 1)
            0x75 -> Pair("LD (HL),L", 1)
            0x36 -> Pair("LD (HL),${toHexString(nextByte(), 2)}", 2)

            0xFA -> Pair("LD A,(${toHexString(nextAddress(), 4)})", 3)
            0x3E -> Pair("LD A,${toHexString(nextByte(), 2)}", 2)

            0x47 -> Pair("LD B,A", 1)
            0x4F -> Pair("LD C,A", 1)
            0x57 -> Pair("LD D,A", 1)
            0x5F -> Pair("LD E,A", 1)
            0x67 -> Pair("LD H,A", 1)
            0x6F -> Pair("LD L,A", 1)

            0x02 -> Pair("LD (BC),A", 1)
            0x12 -> Pair("LD (DE),A", 1)
            0x77 -> Pair("LD (HL),A", 1)
            0xEA -> Pair("LD (${toHexString(nextAddress(), 4)}),A", 3)

            0xF2 -> Pair("LD A,(FF00+C)", 1)
            0xE2 -> Pair("LD (FF00+C),A", 1)

            0x3A -> Pair("LD A,(HL-)", 1)
            0x32 -> Pair("LD (HL-),A", 1)

            0x2A -> Pair("LD A,(HL+)", 1)
            0x22 -> Pair("LD (HL+),A", 1)

            0xF0 -> Pair("LD A,(FF00+${toHexString(nextByte(), 2)})", 2)
            0xE0 -> Pair("LD (FF00+${toHexString(nextByte(), 2)}),A", 2)

            0x01 -> Pair("LD BC,(${toHexString(nextAddress(), 4)})", 3)
            0x11 -> Pair("LD DE,(${toHexString(nextAddress(), 4)})", 3)
            0x21 -> Pair("LD HL,(${toHexString(nextAddress(), 4)})", 3)
            0x31 -> Pair("LD SP,(${toHexString(nextAddress(), 4)})", 3)

            0xF9 -> Pair("LD SP,HL", 1)

            0xF8 -> Pair("LD HL,SP+(${nextByte().asSigned8()})", 2)

            0x08 -> Pair("LD (${toHexString(nextAddress(), 4)}),SP", 3)

            0xF5 -> Pair("PUSH AF", 1)
            0xC5 -> Pair("PUSH BC", 1)
            0xD5 -> Pair("PUSH DE", 1)
            0xE5 -> Pair("PUSH HL", 1)

            0xF1 -> Pair("POP AF", 1)
            0xC1 -> Pair("POP BC", 1)
            0xD1 -> Pair("POP DE", 1)
            0xE1 -> Pair("POP HL", 1)

            0x87 -> Pair("ADD A", 1)
            0x80 -> Pair("ADD B", 1)
            0x81 -> Pair("ADD C", 1)
            0x82 -> Pair("ADD D", 1)
            0x83 -> Pair("ADD E", 1)
            0x84 -> Pair("ADD H", 1)
            0x85 -> Pair("ADD L", 1)
            0x86 -> Pair("ADD (HL)", 1)
            0xC6 -> Pair("ADD ${toHexString(nextByte(), 2)}", 2)

            0x8F -> Pair("ADC A", 1)
            0x88 -> Pair("ADC B", 1)
            0x89 -> Pair("ADC C", 1)
            0x8A -> Pair("ADC D", 1)
            0x8C -> Pair("ADC H", 1)
            0x8B -> Pair("ADC E", 1)
            0x8D -> Pair("ADC L", 1)
            0x8E -> Pair("ADC (HL)", 1)
            0xCE -> Pair("ADC ${toHexString(nextByte(), 2)}", 2)

            0x97 -> Pair("SUB A", 1)
            0x90 -> Pair("SUB B", 1)
            0x91 -> Pair("SUB C", 1)
            0x92 -> Pair("SUB D", 1)
            0x93 -> Pair("SUB E", 1)
            0x94 -> Pair("SUB H", 1)
            0x95 -> Pair("SUB L", 1)
            0x96 -> Pair("SUB (HL)", 1)
            0xD6 -> Pair("SUB ${toHexString(nextByte(), 2)}", 2)

            0x9F -> Pair("SBC A", 1)
            0x98 -> Pair("SBC B", 1)
            0x99 -> Pair("SBC C", 1)
            0x9A -> Pair("SBC D", 1)
            0x9B -> Pair("SBC E", 1)
            0x9C -> Pair("SBC H", 1)
            0x9D -> Pair("SBC L", 1)
            0x9E -> Pair("SBC (HL)", 1)
            0xDE -> Pair("SBC ${toHexString(nextByte(), 2)}", 2)

            0xA7 -> Pair("AND A", 1)
            0xA0 -> Pair("AND B", 1)
            0xA1 -> Pair("AND C", 1)
            0xA2 -> Pair("AND D", 1)
            0xA3 -> Pair("AND E", 1)
            0xA4 -> Pair("AND H", 1)
            0xA5 -> Pair("AND L", 1)
            0xA6 -> Pair("AND (HL)", 1)
            0xE6 -> Pair("AND ${toHexString(nextByte(), 2)}", 2)

            0xB7 -> Pair("OR A", 1)
            0xB0 -> Pair("OR B", 1)
            0xB1 -> Pair("OR C", 1)
            0xB2 -> Pair("OR D", 1)
            0xB3 -> Pair("OR E", 1)
            0xB4 -> Pair("OR H", 1)
            0xB5 -> Pair("OR L", 1)
            0xB6 -> Pair("OR (HL)", 1)
            0xF6 -> Pair("OR ${toHexString(nextByte(), 2)}", 2)

            0xAF -> Pair("XOR A", 1)
            0xA8 -> Pair("XOR B", 1)
            0xA9 -> Pair("XOR C", 1)
            0xAA -> Pair("XOR D", 1)
            0xAB -> Pair("XOR E", 1)
            0xAC -> Pair("XOR H", 1)
            0xAD -> Pair("XOR L", 1)
            0xAE -> Pair("XOR (HL)", 1)
            0xEE -> Pair("XOR ${toHexString(nextByte(), 2)}", 2)

            0xBF -> Pair("CP A", 1)
            0xB8 -> Pair("CP B", 1)
            0xB9 -> Pair("CP C", 1)
            0xBA -> Pair("CP D", 1)
            0xBB -> Pair("CP E", 1)
            0xBC -> Pair("CP H", 1)
            0xBD -> Pair("CP L", 1)
            0xBE -> Pair("CP (HL)", 1)
            0xFE -> Pair("CP ${toHexString(nextByte(), 2)}", 2)

            0x3C -> Pair("INC A", 1)
            0x04 -> Pair("INC B", 1)
            0x0C -> Pair("INC C", 1)
            0x14 -> Pair("INC D", 1)
            0x1C -> Pair("INC E", 1)
            0x24 -> Pair("INC H", 1)
            0x2C -> Pair("INC L", 1)
            0x34 -> Pair("INC (HL)", 1)

            0x3D -> Pair("DEC A", 1)
            0x05 -> Pair("DEC B", 1)
            0x0D -> Pair("DEC C", 1)
            0x15 -> Pair("DEC D", 1)
            0x1D -> Pair("DEC E", 1)
            0x25 -> Pair("DEC H", 1)
            0x2D -> Pair("DEC L", 1)
            0x35 -> Pair("DEC (HL)", 1)

            0x09 -> Pair("ADD HL,BC", 1)
            0x19 -> Pair("ADD HL,DE", 1)
            0x29 -> Pair("ADD HL,HL", 1)
            0x39 -> Pair("ADD HL,SP", 1)

            0xE8 -> Pair("ADD SP+(${toHexString(nextByte(), 2)})", 2)

            0x03 -> Pair("INC BC", 1)
            0x13 -> Pair("INC DE", 1)
            0x23 -> Pair("INC HL", 1)
            0x33 -> Pair("INC SP", 1)

            0x0B -> Pair("DEC BC", 1)
            0x1B -> Pair("DEC DE", 1)
            0x2B -> Pair("DEC HL", 1)
            0x3B -> Pair("DEC SP", 1)

            0xCB -> Pair(cbOpcode(nextByte()), 2)

            0x27 -> Pair("DDA", 1)

            0x2F -> Pair("CPL", 1)

            0x3F -> Pair("CCF", 1)

            0x37 -> Pair("SCF", 1)

            0x00 -> Pair("NOP", 1)

            0x76 -> Pair("HALT", 1)

            0x10 -> {
                val nextPart = nextByte()
                when(nextPart) {
                    0x00 -> {
                        Pair("STOP", 2)
                    }
                    else -> Pair("<corrupted stop>", 2)
                }
            }

            0xF3 -> Pair("DI", 1)
            0xFB -> Pair("EI", 1)

            0x07 -> Pair("RLC A", 1)
            0x17 -> Pair("RL A", 1)
            0x0F -> Pair("RRC A", 1)
            0x1F -> Pair("RR A", 1)

            0xC3 -> Pair("JP(${toHexString(nextAddress(), 4)})", 3)
            0xC2 -> Pair("JP NZ(${toHexString(nextAddress(), 4)})", 3)
            0xCA -> Pair("JP Z(${toHexString(nextAddress(), 4)})", 3)
            0xD2 -> Pair("JP NC(${toHexString(nextAddress(), 4)})", 3)
            0xDA -> Pair("JP C(${toHexString(nextAddress(), 4)})", 3)

            0xE9 -> Pair("JP HL", 1)

            0x18 -> Pair("JR(${address+nextByte().asSigned8()})", 2)

            0x20 -> Pair("JR NZ(${address+nextByte().asSigned8()})", 2)
            0x28 -> Pair("JR Z(${address+nextByte().asSigned8()})", 2)
            0x30 -> Pair("JR NC(${address+nextByte().asSigned8()})", 2)
            0x38 -> Pair("JR C(${address+nextByte().asSigned8()})", 2)

            0xCD -> Pair("CALL(${toHexString(nextAddress(), 4)})", 3)
            0xC4 -> Pair("CALL NZ(${toHexString(nextAddress(), 4)})", 3)
            0xCC -> Pair("CALL Z(${toHexString(nextAddress(), 4)})", 3)
            0xD4 -> Pair("CALL NC(${toHexString(nextAddress(), 4)})", 3)
            0xDC -> Pair("CALL C(${toHexString(nextAddress(), 4)})", 3)

            0xC7 -> Pair("RST 00h", 1)
            0xCF -> Pair("RST 08h", 1)
            0xD7 -> Pair("RST 10h", 1)
            0xDF -> Pair("RST 18h", 1)
            0xE7 -> Pair("RST 20h", 1)
            0xEF -> Pair("RST 28h", 1)
            0xF7 -> Pair("RST 30h", 1)
            0xFF -> Pair("RST 38h", 1)

            0xC9 -> Pair("RET", 1)
            0xC0 -> Pair("RET NZ", 1)
            0xC8 -> Pair("RET Z", 1)
            0xD0 -> Pair("RET NC", 1)
            0xD8 -> Pair("RET C", 1)
            0xD9 -> Pair("RETI", 1)
            else -> Pair("ILLEGAL OPCODE", 1)
        }
    }

    private fun cbOpcode(opcode: Int): String {
        return when(opcode) {
            0x37 -> "SWAP A"
            0x30 -> "SWAP B"
            0x31 -> "SWAP C"
            0x32 -> "SWAP D"
            0x33 -> "SWAP E"
            0x34 -> "SWAP H"
            0x35 -> "SWAP L"
            0x36 -> "SWAP (HL)"

            0x07 -> "RLC A"
            0x00 -> "RLC B"
            0x01 -> "RLC C"
            0x02 -> "RLC D"
            0x03 -> "RLC E"
            0x04 -> "RLC H"
            0x05 -> "RLC L"
            0x06 -> "RLC (HL)"

            0x17 -> "RL A"
            0x10 -> "RL B"
            0x11 -> "RL C"
            0x12 -> "RL D"
            0x13 -> "RL E"
            0x14 -> "RL H"
            0x15 -> "RL L"
            0x16 -> "RL (HL)"

            0x0F -> "RRC A"
            0x08 -> "RRC B"
            0x09 -> "RRC C"
            0x0A -> "RRC D"
            0x0B -> "RRC E"
            0x0C -> "RRC H"
            0x0D -> "RRC L"
            0x0E -> "RRC (HL)"

            0x1F -> "RR A"
            0x18 -> "RR B"
            0x19 -> "RR C"
            0x1A -> "RR D"
            0x1B -> "RR E"
            0x1C -> "RR H"
            0x1D -> "RR L"
            0x1E -> "RR (HL)"

            0x27 -> "SLA A"
            0x20 -> "SLA B"
            0x21 -> "SLA C"
            0x22 -> "SLA D"
            0x23 -> "SLA E"
            0x24 -> "SLA H"
            0x25 -> "SLA L"
            0x26 -> "SLA (HL)"

            0x2F -> "SRA A"
            0x28 -> "SRA B"
            0x29 -> "SRA C"
            0x2A -> "SRA D"
            0x2B -> "SRA E"
            0x2C -> "SRA H"
            0x2D -> "SRA L"
            0x2E -> "SRA (HL)"

            0x3F -> "SRL A"
            0x38 -> "SRL B"
            0x39 -> "SRL C"
            0x3A -> "SRL D"
            0x3B -> "SRL E"
            0x3C -> "SRL H"
            0x3D -> "SRL L"
            0x3E -> "SRL (HL)"

            in 0x40..0x7F -> bit(opcode)
            in 0xC0..0xFF -> set(opcode)
            in 0x80..0xBF -> res(opcode)

            else -> error("Invalid CB opcode ${Integer.toHexString(opcode)}")
        }
    }

    private val registerList = listOf("B", "C", "D", "E", "H", "L", "atHL", "A")

    private fun res(opcode: Int): String {
        val register = registerList[(opcode-0x80) % registerList.size]
        val bit = (opcode-0x80) / registerList.size
        return "RES $register,$bit"
    }

    private fun set(opcode: Int): String {
        val register = registerList[(opcode-0xC0) % registerList.size]
        val bit = (opcode-0xC0) / registerList.size
        return "SET $register,$bit"
    }

    private fun bit(opcode: Int): String {
        val register = registerList[(opcode-0x40) % registerList.size]
        val bit = (opcode-0x40) / registerList.size
        return "BIT $register,$bit"
    }

    private fun toHexString(value: Int, minDigitCount: Int): String {
        return Integer.toHexString(value).toUpperCase().padStart(minDigitCount, '0')
    }
}