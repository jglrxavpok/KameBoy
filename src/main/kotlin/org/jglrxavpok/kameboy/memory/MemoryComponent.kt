package org.jglrxavpok.kameboy.memory

import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.helpful.setBits
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

interface MemoryComponent {

    val name: String
    fun write(address: Int, value: Int)
    fun read(address: Int): Int
}

abstract class RAM(val size: Int): MemoryComponent {
    internal val data = ByteArray(size)

    override fun write(address: Int, value: Int) {
        data[correctAddress(address)] = value.toByte()
    }

    fun writeNoSideEffects(address: Int, value: Int) {
        data[correctAddress(address)] = value.toByte()
    }

    override fun read(address: Int): Int {
        return data[correctAddress(address)].asUnsigned()
    }

    abstract fun correctAddress(address: Int): Int
}

interface SingleValueMemoryComponent: MemoryComponent {

    fun setValue(value: Int)
    fun getValue(): Int

    override fun write(address: Int, value: Int) {
        setValue(value)
    }

    override fun read(address: Int) = getValue()

    fun bitVar(i: Int): BitProperty {
        return BitProperty(this, i)
    }

    class BitProperty(val memory: SingleValueMemoryComponent, val bitIndex: Int) {

        var value: Boolean
            get() = (memory.getValue() and (1 shl bitIndex)) != 0
            set(va) { memory.setValue(memory.getValue().setBits(if(va) 1 else 0, bitIndex..bitIndex))}

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = value
        operator fun setValue(thisRef: Any?, property: KProperty<*>, newVal: Boolean) {
            value = newVal
        }
    }
}