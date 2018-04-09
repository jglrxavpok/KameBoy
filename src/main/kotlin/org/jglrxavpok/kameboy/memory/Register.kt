package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asUnsigned8
import org.jglrxavpok.kameboy.helpful.setBits

open class Register(override val name: String, protected var registerValue: Int = 0x0, val sizeInBits: Int = 8): SingleValueMemoryComponent {

    val maxBit = (1 shl sizeInBits)
    val mask = maxBit-1

    override fun write(address: Int, value: Int) {
        setValue(value)
    }

    override fun setValue(value: Int) {
        this.registerValue = value
        fitValueInBounds()
    }

    override fun getValue(): Int {
        return registerValue
    }

    override fun read(address: Int) = getValue()

    operator fun inc(): Register {
        registerValue++
        fitValueInBounds()
        return this
    }

    operator fun dec(): Register {
        registerValue--
        fitValueInBounds()
        return this
    }

    protected open fun fitValueInBounds() {
        registerValue = getValue() and mask
    }

    operator fun plusAssign(value: Int) {
        this.registerValue += value
        fitValueInBounds()
    }

    operator fun plus(value: Int): Int {
        return this.getValue() + value
    }

    operator fun minus(value: Int): Int {
        return this.getValue() - value
    }

    infix fun shl(count: Int): Int {
        registerValue = getValue() shl count
        fitValueInBounds()
        return registerValue
    }

    infix fun shr(count: Int): Int {
        registerValue = getValue() shr count
        fitValueInBounds()
        return registerValue
    }

    /**
     * Returns the set state of a given bit
     */
    operator fun get(bitIndex: Int): Boolean {
        return (getValue() and (1 shl bitIndex)) != 0
    }

    operator fun set(bitIndex: Int, value: Boolean) {
        this.registerValue = this.getValue().setBits(if(value) 1 else 0, bitIndex..bitIndex)
    }

    fun atPointed(memoryComponent: MemoryComponent) = memoryComponent.read(getValue())

    override fun toString(): String {
        return "Register[name=$name; value=$registerValue]"
    }
}

open class MemoryRegister(override val name: String, val memory: MemoryComponent, open val address: Int = 0): SingleValueMemoryComponent {
    override fun setValue(value: Int) {
        memory.write(this.address, value)
    }

    override fun getValue() = memory.read(this.address)
}

class PairedRegisters(val high: Register, val low: Register): SingleValueMemoryComponent {

    override val name = high.name+low.name+" (Paired)"

    override fun write(address: Int, value: Int) {
        val lowValue = value.asUnsigned8()
        val highValue = (value shr 8).asUnsigned8()
        low.write(address+1, lowValue)
        high.write(address, highValue)
    }

    override fun read(address: Int): Int {
        return (high.read(address).asUnsigned8() shl 8) or low.read(address+1).asUnsigned8()
    }

    override fun setValue(value: Int) {
        write(0, value)
    }

    override fun getValue() = read(0)

    fun atPointed(memoryComponent: MemoryComponent) = memoryComponent.read(getValue())

    operator fun dec(): PairedRegisters {
        setValue(getValue()-1)
        return this
    }

    operator fun inc(): PairedRegisters {
        setValue(getValue()+1)
        return this
    }
}