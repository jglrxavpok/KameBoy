package org.jglrxavpok.kameboy.time

import org.jglrxavpok.kameboy.Gameboy
import org.jglrxavpok.kameboy.helpful.asUnsigned
import org.jglrxavpok.kameboy.memory.RAM
import org.jglrxavpok.kameboy.memory.RamBank
import org.jglrxavpok.kameboy.memory.SingleValueMemoryComponent
import org.jglrxavpok.kameboy.sound.Timer
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

class SaveState internal constructor(val gameboy: Gameboy) {

    val memoryContents = ByteArray(0xFFFF+1)
    val saveData = mutableMapOf<Pair<KProperty<*>, Any>, Any>()

    init {
        val elements = gameboy.saveStateElements
        for(pair in elements) {
            val elem = pair.first
            val owner = pair.second
            val value = elem.call(owner)!!
            saveData[pair] = saveData(elem, owner, value)
        }
        for(address in 0x0..0xFFFF) {
            val component = gameboy.mapper.map(address)
            if(component is SingleValueMemoryComponent) {
                memoryContents[address] = component.getValue().toByte()
            }
        }
    }

    private fun saveData(property: KProperty<*>, owner: Any, value: Any): Any {
        val type = property.returnType.classifier as? KClass<*> ?: error("Unknown type for $property")
        return when {
            SingleValueMemoryComponent::class.isSuperclassOf(type) -> {
                (value as SingleValueMemoryComponent).getValue()
            }
            RAM::class.isSuperclassOf(type) -> {
                val ram = value as RAM
                ram.data.copyOf()
            }
            Array<RAM>::class.isSuperclassOf(type) -> {
                val array = value as Array<RAM>
                array.map { it.data.copyOf() }
            }
            Array<RamBank>::class.isSuperclassOf(type) -> {
                val array = value as Array<RamBank>
                array.map { it.data.copyOf() }
            }
            Timer::class.isSubclassOf(type) -> {
                val timer = value as Timer
                Pair(timer.period, timer.counter)
            }
            else -> {
                if(property is KMutableProperty) {
                    value
                } else {
                    error("Could not handle given property: $property (Is it immutable?)")
                }
            }

        }
    }

    private fun loadData(property: KProperty<*>, owner: Any, value: Any, propertyValue: Any) {
        val type = property.returnType.classifier as? KClass<*> ?: error("Unknown type for $property")
        when {
            SingleValueMemoryComponent::class.isSuperclassOf(type) -> {
                (propertyValue as SingleValueMemoryComponent).setValue(value as Int)
            }
            RAM::class.isSuperclassOf(type) -> {
                val ram = propertyValue as RAM
                val data = value as ByteArray
                data.forEachIndexed(ram.data::set)
            }
            Array<RAM>::class.isSuperclassOf(type) -> {
                val array = propertyValue as Array<RAM>
                val data = value as List<ByteArray>
                data.forEachIndexed { index, bytes ->
                    bytes.forEachIndexed(array[index].data::set)
                }
            }
            Array<RamBank>::class.isSuperclassOf(type) -> {
                val array = propertyValue as Array<RamBank>
                val data = value as List<ByteArray>
                data.forEachIndexed { index, bytes ->
                    bytes.forEachIndexed(array[index].data::set)
                }
            }
            Timer::class.isSubclassOf(type) -> {
                val timer = propertyValue as Timer
                val (period, counter) = value as Pair<Int, Int>
                timer.period = period
                timer.counter = counter
            }
            else -> {
                if(property is KMutableProperty) {
                    property.setter.call(owner, value)
                } else {
                    error("Could not handle given property: $property (Is it immutable?)")
                }
            }
        }
    }

    fun load() {
        val elements = gameboy.saveStateElements
        for(pair in elements) {
            val elem = pair.first
            val owner = pair.second
            val value = saveData[pair]!!
            val propValue = elem.call(owner)!!
            loadData(elem, owner, value, propValue)
        }

        for(address in 0x0000..0xFFFF) {
            val component = gameboy.mapper.map(address)
            if(component is SingleValueMemoryComponent) {
                component.setValue(memoryContents[address].asUnsigned())
            }
        }

        val memory = gameboy.mapper
        memory.spriteAttributeTable.reloadSprites()
    }

}

fun CreateSaveState(gameboy: Gameboy) = SaveState(gameboy)