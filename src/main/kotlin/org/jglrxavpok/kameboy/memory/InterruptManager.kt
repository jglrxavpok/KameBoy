package org.jglrxavpok.kameboy.memory

class InterruptManager(val memory: MemoryComponent) {

    val IF = MemoryRegister("IF", memory, 0xFF0F)
    var interruptsEnabled = true

    fun fireVBlank() {
        fireInterrupt(0)
    }

    fun fireLCDC() {
        fireInterrupt(1)
    }

    fun fireTimerOverflow() {
        fireInterrupt(2)
    }

    fun fireSerialIOTransferComplete() {
        fireInterrupt(3)
    }

    fun firePinReleased() {
        fireInterrupt(4)
    }

    fun fireInterrupt(interruptIndex: Int) {
        IF.setValue(IF.getValue() or (1 shl interruptIndex))
    }

    fun hasVBlank(): Boolean {
        return IF.getValue() and (1 shl 0) != 0
    }

    fun hasLCDC(): Boolean {
        return IF.getValue() and (1 shl 1) != 0
    }

    fun hasTimerOverflow(): Boolean {
        return IF.getValue() and (1 shl 2) != 0
    }

    fun hasSerial(): Boolean {
        return IF.getValue() and (1 shl 3) != 0
    }

    fun hasPinReleased(): Boolean {
        return IF.getValue() and (1 shl 4) != 0
    }

    fun reset(interruptIndex: Int) {
        val mask = (1 shl interruptIndex).inv()
        IF.setValue(IF.getValue() and mask)
    }
}