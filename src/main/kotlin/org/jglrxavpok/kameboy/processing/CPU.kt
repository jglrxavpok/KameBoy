package org.jglrxavpok.kameboy.processing

import org.jglrxavpok.kameboy.helpful.asSigned8
import org.jglrxavpok.kameboy.helpful.asUnsigned16
import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.memory.*

class CPU(val memory: MemoryComponent, val interruptManager: InterruptManager) {

    var halted = false
    var stopped = false
    private var requestedInterruptChange = false
    private var desiredInterruptState = false
    var stackPointer = Register("Stack Pointer", 0, sizeInBits = 16)
    var programCounter = Register("Program Counter", 0, sizeInBits = 16)
    var A = Register("A", 0, sizeInBits = 8)
    var B = Register("B", 0, sizeInBits = 8)
    var C = Register("C", 0, sizeInBits = 8)
    var D = Register("D", 0, sizeInBits = 8)
    var E = Register("E", 0, sizeInBits = 8)
    var F = Register("F", 0, sizeInBits = 8)
    var H = Register("H", 0, sizeInBits = 8)
    var L = Register("L", 0, sizeInBits = 8)
    var AF = PairedRegisters(A, F)
    var BC = PairedRegisters(B, C)
    var DE = PairedRegisters(D, E)
    var HL = PairedRegisters(H, L)
    var atHL = object: MemoryRegister("(HL)", memory) {
        override val address get()= HL.getValue()
    }

    var flagZ by F.bitVar(7)
    var flagN by F.bitVar(6)
    var flagH by F.bitVar(5)
    var flagC by F.bitVar(4)

    fun reset() {
        // FIXME: GB only for the moment
        A.setValue(0x01)
        F.setValue(0xB0)
        B.setValue(0x00)
        C.setValue(0x13)
        DE.setValue(0x00D8)
        HL.setValue(0x014D)
        stackPointer.setValue(0xFFFE)
        programCounter.setValue(0x0100)

        memory.write(0xFF05, 0x00)   // TIMA
        memory.write(0xFF06, 0x00)   // TMA
        memory.write(0xFF07, 0x00)   // TAC
        memory.write(0xFF10, 0x80)   // NR10
        memory.write(0xFF11, 0xBF)   // NR11
        memory.write(0xFF12, 0xF3)   // NR12
        memory.write(0xFF14, 0xBF)   // NR14
        memory.write(0xFF16, 0x3F)   // NR21
        memory.write(0xFF17, 0x00)   // NR22
        memory.write(0xFF19, 0xBF)   // NR24
        memory.write(0xFF1A, 0x7F)   // NR30
        memory.write(0xFF1B, 0xFF)   // NR31
        memory.write(0xFF1C, 0x9F)   // NR32
        memory.write(0xFF1E, 0xBF)   // NR33
        memory.write(0xFF20, 0xFF)   // NR41
        memory.write(0xFF21, 0x00)   // NR42
        memory.write(0xFF22, 0x00)   // NR43
        memory.write(0xFF23, 0xBF)   // NR30
        memory.write(0xFF24, 0x77)   // NR50
        memory.write(0xFF25, 0xF3)   // NR51
        memory.write(0xFF26, 0xF1)   // NR52 (GB only)
        memory.write(0xFF40, 0x91)   // LCDC
        memory.write(0xFF42, 0x00)   // SCY
        memory.write(0xFF43, 0x00)   // SCX
        memory.write(0xFF45, 0x00)   // LYC
        memory.write(0xFF47, 0xFC)   // BGP
        memory.write(0xFF48, 0xFF)   // OBP0
        memory.write(0xFF49, 0xFF)   // OBP1
        memory.write(0xFF4A, 0x00)   // WY
        memory.write(0xFF4B, 0x00)   // WX
        memory.write(0xFFFF, 0x00) // IE
    }

    /**
     * Returns clock cycles consumed
     */
    fun step(): Int {
        val shouldChangeInterruptState = requestedInterruptChange
        if(interruptManager.interruptsEnabled) {
            val interruptFlag = memory.read(0xFF0F)
            val interruptEnable = memory.read(0xFFFF)
            if(interruptFlag and interruptEnable != 0) {
                interruptManager.interruptsEnabled = false
                when {
                    !stopped && interruptManager.hasVBlank() -> interrupt(0)
                    !stopped && interruptManager.hasLCDC() -> interrupt(1)
                    !stopped && interruptManager.hasTimerOverflow() -> interrupt(2)
                    !stopped && interruptManager.hasSerial() -> interrupt(3)
                    interruptManager.hasPinReleased() -> {
                        stopped = false
                        interrupt(4)
                    }
                }
            }
        }
        if(halted || stopped)
            return 4
        val position = programCounter.getValue()
        val opcode = nextByte()
        try {
            val clockCycles = execute(opcode)
            if(shouldChangeInterruptState) {
                requestedInterruptChange = false
                interruptManager.interruptsEnabled = desiredInterruptState
            }
            return clockCycles
        } catch (e: Exception) {
            println("Found error in opcode ${Integer.toHexString(opcode)} at PC ${Integer.toHexString(position)}")
            e.printStackTrace()
        }
        return -1
    }

    private fun interrupt(interruptIndex: Int) {
        halted = false
        interruptManager.reset(interruptIndex)
        call(0x40 + interruptIndex * 8)
    }

    private fun execute(opcode: Int): Int {
        return when(opcode) {
            0x06 -> { ld(B, nextByte()); 8}
            0x0E -> { ld(C, nextByte()); 8}
            0x16 -> { ld(D, nextByte()); 8}
            0x1E -> { ld(E, nextByte()); 8}
            0x26 -> { ld(H, nextByte()); 8}
            0x2E -> { ld(L, nextByte()); 8}

            0x7F -> { ld(A, A.getValue()); 4}
            0x78 -> { ld(A, B.getValue()); 4}
            0x79 -> { ld(A, C.getValue()); 4}
            0x7A -> { ld(A, D.getValue()); 4}
            0x7B -> { ld(A, E.getValue()); 4}
            0x7C -> { ld(A, H.getValue()); 4}
            0x7D -> { ld(A, L.getValue()); 4}
            0x7E -> { ld(A, HL.atPointed(memory)); 8}

            0x40 -> { ld(B, B.getValue()); 4}
            0x41 -> { ld(B, C.getValue()); 4}
            0x42 -> { ld(B, D.getValue()); 4}
            0x43 -> { ld(B, E.getValue()); 4}
            0x44 -> { ld(B, H.getValue()); 4}
            0x45 -> { ld(B, L.getValue()); 4}
            0x46 -> { ld(B, HL.atPointed(memory)); 8}

            0x48 -> { ld(C, B.getValue()); 4}
            0x49 -> { ld(C, C.getValue()); 4}
            0x4A -> { ld(C, D.getValue()); 4}
            0x4B -> { ld(C, E.getValue()); 4}
            0x4C -> { ld(C, H.getValue()); 4}
            0x4D -> { ld(C, L.getValue()); 4}
            0x4E -> { ld(C, HL.atPointed(memory)); 8}

            0x50 -> { ld(D, B.getValue()); 4}
            0x51 -> { ld(D, C.getValue()); 4}
            0x52 -> { ld(D, D.getValue()); 4}
            0x53 -> { ld(D, E.getValue()); 4}
            0x54 -> { ld(D, H.getValue()); 4}
            0x55 -> { ld(D, L.getValue()); 4}
            0x56 -> { ld(D, HL.atPointed(memory)); 8}

            0x58 -> { ld(E, B.getValue()); 4}
            0x59 -> { ld(E, C.getValue()); 4}
            0x5A -> { ld(E, D.getValue()); 4}
            0x5B -> { ld(E, E.getValue()); 4}
            0x5C -> { ld(E, H.getValue()); 4}
            0x5D -> { ld(E, L.getValue()); 4}
            0x5E -> { ld(E, HL.atPointed(memory)); 8}

            0x60 -> { ld(H, B.getValue()); 4}
            0x61 -> { ld(H, C.getValue()); 4}
            0x62 -> { ld(H, D.getValue()); 4}
            0x63 -> { ld(H, E.getValue()); 4}
            0x64 -> { ld(H, H.getValue()); 4}
            0x65 -> { ld(H, L.getValue()); 4}
            0x66 -> { ld(H, HL.atPointed(memory)); 8}

            0x68 -> { ld(L, B.getValue()); 4}
            0x69 -> { ld(L, C.getValue()); 4}
            0x6A -> { ld(L, D.getValue()); 4}
            0x6B -> { ld(L, E.getValue()); 4}
            0x6C -> { ld(L, H.getValue()); 4}
            0x6D -> { ld(L, L.getValue()); 4}
            0x6E -> { ld(L, HL.atPointed(memory)); 8}

            0x70 -> { ld_address(HL.getValue(), B.getValue()); 8}
            0x71 -> { ld_address(HL.getValue(), C.getValue()); 8}
            0x72 -> { ld_address(HL.getValue(), D.getValue()); 8}
            0x73 -> { ld_address(HL.getValue(), E.getValue()); 8}
            0x74 -> { ld_address(HL.getValue(), H.getValue()); 8}
            0x75 -> { ld_address(HL.getValue(), L.getValue()); 8}
            0x36 -> { ld_address(HL.getValue(), nextByte()); 12}

            0xFA -> { ld(A, memory.read(nextAddress())); 16}
            0x3E -> { ld(A, nextByte()); 16}

            0x47 -> { ld(A, B.getValue()); 4}
            0x4F -> { ld(A, C.getValue()); 4}
            0x57 -> { ld(A, D.getValue()); 4}
            0x5F -> { ld(A, E.getValue()); 4}
            0x67 -> { ld(A, H.getValue()); 4}
            0x6F -> { ld(A, L.getValue()); 4}

            0x02 -> { ld_address(BC.getValue(), A.getValue()); 8}
            0x12 -> { ld_address(DE.getValue(), A.getValue()); 8}
            0x77 -> { ld_address(HL.getValue(), A.getValue()); 8}
            0xEA -> { ld_address(nextAddress(), A.getValue()); 16}

            0xF2 -> { ld(A, memory.read(C +0xFF00)); 8}
            0xE2 -> { ld_address(C + 0xFF00, A.getValue()); 8}

            0x3A -> { ld(A, HL.atPointed(memory)); HL--; 8}
            0x32 -> { ld_address(HL.getValue(), A.getValue()); HL--; 8}

            0x2A -> { ld(A, HL.atPointed(memory)); HL++; 8}
            0x22 -> { ld_address(HL.getValue(), A.getValue()); HL++; 8}

            0xF0 -> { ld(A, memory.read(0xFF00 + nextByte())); 12}
            0xE0 -> { ld_address(0xFF00 + nextByte(), A.getValue()); 12}

            0x01 -> { ld(BC, nextAddress()); 12}
            0x11 -> { ld(DE, nextAddress()); 12}
            0x21 -> { ld(HL, nextAddress()); 12}
            0x31 -> { ld(stackPointer, nextAddress()); 12}

            0xF9 -> { ld(stackPointer, HL.getValue()); 8}

            0xF8 -> {
                val n = nextByte().asSigned8()
                ld(HL, stackPointer + n)
                flagH = ((programCounter.getValue() and 0x0FFF) + n.asUnsigned16()) > 0x0FFF
                flagC = (programCounter + n.asUnsigned16()) > 0xFFFF
                flagZ = false
                flagN = false
                12
            }

            0x08 -> { ld_address(nextAddress(), stackPointer.getValue()); 20}

            0xF5 -> { push(AF); 16}
            0xC5 -> { push(BC); 16}
            0xD5 -> { push(DE); 16}
            0xE5 -> { push(HL); 16}

            0xF1 -> { AF.setValue(pop()); 12}
            0xC1 -> { BC.setValue(pop()); 12}
            0xD1 -> { DE.setValue(pop()); 12}
            0xE1 -> { HL.setValue(pop()); 12}

            0x87 -> { add(A.getValue()); 4}
            0x80 -> { add(B.getValue()); 4}
            0x81 -> { add(C.getValue()); 4}
            0x82 -> { add(D.getValue()); 4}
            0x83 -> { add(E.getValue()); 4}
            0x84 -> { add(H.getValue()); 4}
            0x85 -> { add(L.getValue()); 4}
            0x86 -> { add(HL.atPointed(memory)); 8}
            0xC6 -> { add(nextByte()); 8}

            0x8F -> { add(A.getValue() + if(flagC) 1 else 0); 4}
            0x88 -> { add(B.getValue() + if(flagC) 1 else 0); 4}
            0x89 -> { add(C.getValue() + if(flagC) 1 else 0); 4}
            0x8A -> { add(D.getValue() + if(flagC) 1 else 0); 4}
            0x8B -> { add(E.getValue() + if(flagC) 1 else 0); 4}
            0x8C -> { add(H.getValue() + if(flagC) 1 else 0); 4}
            0x8D -> { add(L.getValue() + if(flagC) 1 else 0); 4}
            0x8E -> { add(HL.atPointed(memory) + if(flagC) 1 else 0); 8}
            0xCE -> { add(nextByte() + if(flagC) 1 else 0); 8}

            0x97 -> { sub(A.getValue()); 4}
            0x90 -> { sub(B.getValue()); 4}
            0x91 -> { sub(C.getValue()); 4}
            0x92 -> { sub(D.getValue()); 4}
            0x93 -> { sub(E.getValue()); 4}
            0x94 -> { sub(H.getValue()); 4}
            0x95 -> { sub(L.getValue()); 4}
            0x96 -> { sub(HL.atPointed(memory)); 8}
            0xD6 -> { sub(nextByte()); 8}

            0x9F -> { sub(A.getValue() + if(flagC) 1 else 0); 4}
            0x98 -> { sub(B.getValue() + if(flagC) 1 else 0); 4}
            0x99 -> { sub(C.getValue() + if(flagC) 1 else 0); 4}
            0x9A -> { sub(D.getValue() + if(flagC) 1 else 0); 4}
            0x9B -> { sub(E.getValue() + if(flagC) 1 else 0); 4}
            0x9C -> { sub(H.getValue() + if(flagC) 1 else 0); 4}
            0x9D -> { sub(L.getValue() + if(flagC) 1 else 0); 4}
            0x9E -> { sub(HL.atPointed(memory) + if(flagC) 1 else 0); 8}
            0xDE -> { sub(nextByte() + if(flagC) 1 else 0); 8}

            0xA7 -> { and(A.getValue()); 4}
            0xA0 -> { and(B.getValue()); 4}
            0xA1 -> { and(C.getValue()); 4}
            0xA2 -> { and(D.getValue()); 4}
            0xA3 -> { and(E.getValue()); 4}
            0xA4 -> { and(H.getValue()); 4}
            0xA5 -> { and(L.getValue()); 4}
            0xA6 -> { and(HL.atPointed(memory)); 8}
            0xE6 -> { and(nextByte()); 8}

            0xB7 -> { or(A.getValue()); 4}
            0xB0 -> { or(B.getValue()); 4}
            0xB1 -> { or(C.getValue()); 4}
            0xB2 -> { or(D.getValue()); 4}
            0xB3 -> { or(E.getValue()); 4}
            0xB4 -> { or(H.getValue()); 4}
            0xB5 -> { or(L.getValue()); 4}
            0xB6 -> { or(HL.atPointed(memory)); 8}
            0xF6 -> { or(nextByte()); 8}

            0xAF -> { xor(A.getValue()); 4}
            0xA8 -> { xor(B.getValue()); 4}
            0xA9 -> { xor(C.getValue()); 4}
            0xAA -> { xor(D.getValue()); 4}
            0xAB -> { xor(E.getValue()); 4}
            0xAC -> { xor(H.getValue()); 4}
            0xAD -> { xor(L.getValue()); 4}
            0xAE -> { xor(HL.atPointed(memory)); 8}
            0xEE -> { xor(nextByte()); 8}

            0xBF -> { cp(A.getValue()); 4}
            0xB8 -> { cp(B.getValue()); 4}
            0xB9 -> { cp(C.getValue()); 4}
            0xBA -> { cp(D.getValue()); 4}
            0xBB -> { cp(E.getValue()); 4}
            0xBC -> { cp(H.getValue()); 4}
            0xBD -> { cp(L.getValue()); 4}
            0xBE -> { cp(HL.atPointed(memory)); 8}
            0xFE -> { cp(nextByte()); 8}

            0x3C -> { inc(A); 4}
            0x04 -> { inc(B); 4}
            0x0C -> { inc(C); 4}
            0x14 -> { inc(D); 4}
            0x1C -> { inc(E); 4}
            0x24 -> { inc(H); 4}
            0x2C -> { inc(L); 4}
            0x34 -> { inc(atHL); 12}

            0x3D -> { dec(A); 4}
            0x05 -> { dec(B); 4}
            0x0D -> { dec(C); 4}
            0x15 -> { dec(D); 4}
            0x1D -> { dec(E); 4}
            0x25 -> { dec(H); 4}
            0x2D -> { dec(L); 4}
            0x35 -> { dec(atHL); 12}

            0x09 -> { addToRegister(HL, BC.getValue()); 8}
            0x19 -> { addToRegister(HL, DE.getValue()); 8}
            0x29 -> { addToRegister(HL, HL.getValue()); 8}
            0x39 -> { addToRegister(HL, stackPointer.getValue()); 8}

            0xE8 -> {
                val value = nextByte()
                val result = stackPointer + value.asSigned8()
                flagH = ((programCounter.getValue() and 0x0FFF) + value) > 0x0FFF
                flagC = (programCounter + value) > 0xFFFF
                flagZ = false
                flagN = false
                stackPointer.setValue(result)
                16
            }

            0x03 -> { inc16(BC); 8}
            0x13 -> { inc16(DE); 8}
            0x23 -> { inc16(HL); 8}
            0x33 -> { inc16(stackPointer); 8}

            0x0B -> { dec16(BC); 8}
            0x1B -> { dec16(DE); 8}
            0x2B -> { dec16(HL); 8}
            0x3B -> { dec16(stackPointer); 8}

            0xCB -> executeCBOpcode(nextByte())

            0x27 -> {
                flagZ = A.getValue() == 0
                flagH = false
                /* from http://www.felixcloutier.com/x86/DAA.html
                old_AL ← AL;
        old_CF ← CF;
        CF ← 0;
        IF (((AL AND 0FH) > 9) or AF = 1)
                THEN
                    AL ← AL + 6;
                    CF ← old_CF or (Carry from AL ← AL + 6);
                    AF ← 1;
                ELSE
                    AF ← 0;
        FI;
        IF ((old_AL > 99H) or (old_CF = 1))
            THEN
                    AL ← AL + 60H;
                    CF ← 1;
            ELSE
                    CF ← 0;
        FI;
                 */
                flagC = false
                if((A.getValue() and 0xF) > 9) {
                    A.setValue(A + 0x06)
                }
                if((A.getValue() shr 4) and 0xF > 9) {
                    A.setValue(A + 0x60)
                    flagC = true
                }
                4
            }

            0x2F -> {
                A.setValue(A.getValue().inv() and 0xFF)
                4
            }

            0x3F -> {
                flagN = false
                flagH = false
                flagC = !flagC
                4
            }

            0x37 -> {
                flagN = false
                flagH = false
                flagC = true
                4
            }

            0x00 -> 4 // NOP

            0x76 -> {
                halted = true
                4
            }

            0x10 -> {
                val nextPart = nextByte()
                when(nextPart) {
                    0x00 -> { stopped = true; 4}
                    else -> error("Invalid opcode 0x10 ${Integer.toHexString(nextPart)}")
                }
            }

            0xF3 -> {
                requestedInterruptChange = true // need to wait for next instruction to be executed
                desiredInterruptState = false // no longer accept
                4
            }

            0xFB -> {
                requestedInterruptChange = true // need to wait for next instruction to be executed
                desiredInterruptState = true // accept
                4
            }

            0x07 -> { rlc(A); 4}
            0x17 -> { rl(A); 4}
            0x0F -> { rrc(A); 4}
            0x1F -> { rr(A); 4}

            0xC3 -> { jp(nextAddress()); 12}
            0xC2 -> {
                val address = nextAddress()
                if(!flagZ) jp(address)
                12
            }
            0xCA -> {
                val address = nextAddress()
                if(flagZ) jp(address)
                12
            }
            0xD2 -> {
                val address = nextAddress()
                if(!flagC) jp(address)
                12
            }
            0xDA -> {
                val address = nextAddress()
                if(flagC) jp(address)
                12
            }

            0xE9 -> {
                jp(atHL.getValue())
                4
            }

            0x18 -> { jpRelative(nextByte()); 8}

            0x20 -> {
                val address = nextByte()
                if(!flagZ) jpRelative(address)
                8
            }
            0x28 -> {
                val address = nextByte()
                if(flagZ) jpRelative(address)
                8
            }
            0x30 -> {
                val address = nextByte()
                if(!flagC) jpRelative(address)
                8
            }
            0x38 -> {
                val address = nextByte()
                if(flagC) jpRelative(address)
                8
            }

            0xCD -> {
                call(nextAddress())
                12
            }
            0xC4 -> {
                val address = nextAddress()
                if(!flagZ) call(address)
                12
            }
            0xCC -> {
                val address = nextAddress()
                if(flagZ) call(address)
                12
            }
            0xD4 -> {
                val address = nextAddress()
                if(!flagC) call(address)
                12
            }
            0xDC -> {
                val address = nextAddress()
                if(flagC) call(address)
                12
            }

            0xC7 -> { rst(0x00); 32}
            0xCF -> { rst(0x08); 32}
            0xD7 -> { rst(0x10); 32}
            0xDF -> { rst(0x18); 32}
            0xE7 -> { rst(0x20); 32}
            0xEF -> { rst(0x28); 32}
            0xF7 -> { rst(0x30); 32}
            0xFF -> { rst(0x38); 32}

            0xC9 -> { ret(); 8}
            0xC0 -> { if(!flagZ) ret(); 8}
            0xC8 -> { if(flagZ) ret(); 8}
            0xD0 -> { if(!flagC) ret(); 8}
            0xD8 -> { if(flagC) ret(); 8}

            0xD9 -> { ret(); interruptManager.interruptsEnabled = true; 8}

            else -> error("Invalid opcode ${Integer.toHexString(opcode)}")
        }
    }

    private fun ret() {
        programCounter.setValue(pop())
    }

    private fun rst(address: Int) {
        call(address)
    }

    private fun call(address: Int) {
        val lowValue = programCounter.getValue() and 0xFF
        val highValue = (programCounter.getValue() shr 8) and 0xFF
        memory.write(stackPointer.getValue(), highValue)
        memory.write(stackPointer.getValue()+1, lowValue)
        stackPointer--
        stackPointer--

        jp(address)
    }

    private fun jpRelative(unsignedOffset: Int) {
        val offset = unsignedOffset.asSigned8()
        jp(programCounter + offset)
    }

    private fun jp(address: Int) {
        programCounter.setValue(address)
    }

    private fun rr(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        val newC = value and 0x1 != 0
        flagN = false
        flagH = false
        val result = ((value shr 1) and 0xFF) or (if(flagC) (1 shl 7) else 0)
        flagZ = result == 0
        register.setValue(result)
        flagC = newC
    }

    private fun rrc(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        val newC = value and 0x1 != 0
        flagN = false
        flagH = false
        val result = ((value shr 1) and 0xFF) or (if(newC) (1 shl 7) else 0)
        flagZ = result == 0
        register.setValue(result)
        flagC = newC
    }

    private fun rl(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        val newC = value and 0b10000000 != 0
        flagN = false
        flagH = false
        val result = ((value shl 1) and 0xFF) or (if(flagC) 1 else 0)
        flagZ = result == 0
        register.setValue(result)
        flagC = newC
    }

    private fun rlc(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        val newC = value and 0b10000000 != 0
        flagN = false
        flagH = false
        val result = ((value shl 1) and 0xFF) or (if(newC) 1 else 0)
        flagZ = result == 0
        register.setValue(result)
        flagC = newC
    }

    private fun executeCBOpcode(opcode: Int): Int {
        return when(opcode) {
            0x37 -> { swap(A); 8}
            0x30 -> { swap(B); 8}
            0x31 -> { swap(C); 8}
            0x32 -> { swap(D); 8}
            0x33 -> { swap(E); 8}
            0x34 -> { swap(H); 8}
            0x35 -> { swap(L); 8}
            0x36 -> { swap(atHL); 16}

            0x07 -> { rlc(A); 8}
            0x00 -> { rlc(B); 8}
            0x01 -> { rlc(C); 8}
            0x02 -> { rlc(D); 8}
            0x03 -> { rlc(E); 8}
            0x04 -> { rlc(H); 8}
            0x05 -> { rlc(L); 8}
            0x06 -> { rlc(atHL); 16}

            0x17 -> { rl(A); 8}
            0x10 -> { rl(B); 8}
            0x11 -> { rl(C); 8}
            0x12 -> { rl(D); 8}
            0x13 -> { rl(E); 8}
            0x14 -> { rl(H); 8}
            0x15 -> { rl(L); 8}
            0x16 -> { rl(atHL); 16}

            0x0F -> { rrc(A); 8}
            0x08 -> { rrc(B); 8}
            0x09 -> { rrc(C); 8}
            0x0A -> { rrc(D); 8}
            0x0B -> { rrc(E); 8}
            0x0C -> { rrc(H); 8}
            0x0D -> { rrc(L); 8}
            0x0E -> { rrc(atHL); 16}

            0x1F -> { rr(A); 8}
            0x18 -> { rr(B); 8}
            0x19 -> { rr(C); 8}
            0x1A -> { rr(D); 8}
            0x1B -> { rr(E); 8}
            0x1C -> { rr(H); 8}
            0x1D -> { rr(L); 8}
            0x1E -> { rr(atHL); 16}

            0x27 -> { sla(A); 8}
            0x20 -> { sla(B); 8}
            0x21 -> { sla(C); 8}
            0x22 -> { sla(D); 8}
            0x23 -> { sla(E); 8}
            0x24 -> { sla(H); 8}
            0x25 -> { sla(L); 8}
            0x26 -> { sla(atHL); 16}

            0x2F -> { sra(A); 8}
            0x28 -> { sra(B); 8}
            0x29 -> { sra(C); 8}
            0x2A -> { sra(D); 8}
            0x2B -> { sra(E); 8}
            0x2C -> { sra(H); 8}
            0x2D -> { sra(L); 8}
            0x2E -> { sra(atHL); 16}

            0x3F -> { srl(A); 8}
            0x38 -> { srl(B); 8}
            0x39 -> { srl(C); 8}
            0x3A -> { srl(D); 8}
            0x3B -> { srl(E); 8}
            0x3C -> { srl(H); 8}
            0x3D -> { srl(L); 8}
            0x3E -> { srl(atHL); 16}

            in 0x40..0x7F -> bit(opcode)
            in 0xC0..0xFF -> set(opcode)
            in 0x80..0xBF -> res(opcode)

            else -> error("Invalid CB opcode ${Integer.toHexString(opcode)}")
        }
    }

    val registerList = listOf(B, C, D, E, H, L, atHL, A)

    private fun res(opcode: Int): Int {
        val register = registerList[(opcode-0x80) % registerList.size]
        val bit = (opcode-0x80) / registerList.size
        val value = register.getValue()
        val result = value and (1 shl bit).inv()
        register.setValue(result)
        return if(register == atHL) 16 else 8
    }

    private fun set(opcode: Int): Int {
        val register = registerList[(opcode-0xC0) % registerList.size]
        val bit = (opcode-0xC0) / registerList.size
        val value = register.getValue()
        val result = value or (1 shl bit)
        register.setValue(result)
        return if(register == atHL) 16 else 8
    }

    private fun bit(opcode: Int): Int {
        val register = registerList[(opcode-0x40) % registerList.size]
        val bit = (opcode-0x40) / registerList.size
        val value = register.getValue()
        flagZ = value and (1 shl bit) == 0
        flagH = true
        flagN = false
        return if(register == atHL) 16 else 8
    }

    private fun srl(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        flagC = value and 0x1 != 0
        val result = ((value shr 1) and 0xFF)
        flagZ = result == 0
        flagN = false
        flagH = false
        register.setValue(result)
    }

    private fun sra(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        val msb = value and 0b10000000
        flagC = value and 0x1 != 0
        val result = ((value shr 1) and 0xFF) or msb
        flagZ = result == 0
        flagN = false
        flagH = false
        register.setValue(result)
    }

    private fun sla(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        flagC = value and 0b10000000 != 0
        val result = (value shl 1) and 0xFF
        flagZ = result == 0
        flagN = false
        flagH = false
        register.setValue(result)
    }

    private fun swap(register: SingleValueMemoryComponent) {
        val value = register.getValue()
        flagN = false
        flagC = false
        flagH = false
        flagZ = value == 0
        val lower = value and 0xF
        val upper = (value shr 4) and 0xF
        val newValue = upper or (lower shl 4)
        register.setValue(newValue)
    }

    private fun dec16(register: SingleValueMemoryComponent) {
        val value = register.getValue()-1
        register.setValue(value)
    }

    private fun inc16(register: SingleValueMemoryComponent) {
        val value = register.getValue()+1
        register.setValue(value)
    }

    private fun addToRegister(register: SingleValueMemoryComponent, value: Int) {
        flagN = true
        val result = register.getValue() + value
        flagC = result > 0xFFFF
        flagH = ((register.getValue() and 0x0FFF) + (value and 0x0FFF)) > 0x0FFF
        register.setValue(result and 0xFFFF)
    }

    private fun dec(register: SingleValueMemoryComponent) {
        register.setValue(register.getValue()-1)
        flagZ = register.getValue() == 0
        flagH = (register.getValue() and 0xF) == 0xF
        flagN = true
    }

    private fun inc(register: SingleValueMemoryComponent) {
        val result = register.getValue() + 1
        flagZ = (result and 0xFF) == 0
        flagH = register.getValue() and 0xF == 0xF
        flagN = false
        register.setValue(result)
    }

    private fun xor(value: Int) {
        val result = A.getValue() xor value
        A.setValue(result)
        flagZ = result == 0
        flagN = false
        flagC = false
        flagH = false
    }

    private fun or(value: Int) {
        val result = A.getValue() or value
        A.setValue(result)
        flagZ = result == 0
        flagN = false
        flagC = false
        flagH = false
    }

    private fun and(value: Int) {
        val result = A.getValue() and value
        A.setValue(result)
        flagZ = result == 0
        flagN = false
        flagC = false
        flagH = true
    }

    private fun sub(value: Int) {
        val result = A - value
        cp(value)
        A.setValue(result)
    }

    private fun cp(value: Int) {
        flagZ = A.getValue() == value
        flagH = (A.getValue() and 0xF) < (value and 0xF)
        flagN = true
        flagC = A.getValue() < value
    }

    private fun add(value: Int) {
        val result = A + value
        flagZ = (result and 0xFF) == 0
        flagH = ((A.getValue() and 0x0F)+(value and 0x0F)) and 0x10 == 0x10
        flagC = result > 0xFF
        flagN = false
        A.setValue(result)
    }

    private fun pop(): Int {
        val high = memory.read(stackPointer.getValue())
        val low = memory.read(stackPointer.getValue()+1)
        stackPointer++
        stackPointer++
        return (high shl 8) or low
    }

    private fun push(register: PairedRegisters) {
        memory.write(stackPointer.getValue(), register.high.getValue())
        memory.write(stackPointer.getValue()+1, register.low.getValue())
        stackPointer--
        stackPointer--
    }

    private fun ld_address(address: Int, value: Int) = memory.write(address, value)

    private fun ld(register: SingleValueMemoryComponent, value: Int) = register.setValue(value)

    fun nextAddress(): Int {
        val low = nextByte().asUnsigned8()
        val high = nextByte().asUnsigned8()

        return (high shl 8) or low
    }

    fun nextByte(): Int {
        val pc = programCounter.getValue()
        programCounter++
        return memory.read(pc)
    }
}