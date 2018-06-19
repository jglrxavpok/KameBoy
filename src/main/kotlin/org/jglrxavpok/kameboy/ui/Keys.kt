package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.processing.video.Video
import java.util.*

abstract class Key<T> {
    internal abstract var currentValue: T
    abstract fun convertToString(): String
    abstract fun convertFromString(value: String)
    abstract fun setToDefault()
}

class IntKey: Key<Int>() {
    override var currentValue = 0

    override fun convertToString() = currentValue.toString()

    override fun convertFromString(value: String) {
        currentValue = value.toInt()
    }

    override fun setToDefault() {
        currentValue = 0
    }
}


class FloatKey: Key<Float>() {
    override var currentValue = 0f

    override fun convertToString() = currentValue.toString()

    override fun convertFromString(value: String) {
        currentValue = value.toFloat()
    }

    override fun setToDefault() {
        currentValue = 0f
    }
}

class BooleanKey: Key<Boolean>() {
    override var currentValue = false

    override fun convertToString() = currentValue.toString()

    override fun convertFromString(value: String) {
        currentValue = value.toLowerCase() == "true"
    }

    override fun setToDefault() {
        currentValue = false
    }
}

class EnumKey<T: Enum<T>>(val values: Array<T>, val defaultValue: T = values[0]): Key<T>() {
    override var currentValue: T = defaultValue

    override fun convertToString(): String {
        return currentValue.name
    }

    override fun convertFromString(value: String) {
        currentValue = values.find { it.name.toLowerCase() == value.toLowerCase() } ?: defaultValue
    }

    override fun setToDefault() {
        currentValue = defaultValue
    }

}

open class KeyGroup {

    private fun listProperties() = this.javaClass.declaredFields.filter { Key::class.java.isAssignableFrom(it.type) }.map {
        it.isAccessible = true
        val r = it.get(this@KeyGroup) as Key<*> to it.name
        it.isAccessible = false
        r
    }

    fun load(props: Properties = Config.properties, groupName: String = this.javaClass.simpleName) {
        for((key, name) in listProperties()) {
            val keyName = "$groupName.$name"
            key.setToDefault()
            if(props.containsKey(keyName))
                key.convertFromString(props.getProperty(keyName))
        }
    }

    fun save(props: Properties = Config.properties, groupName: String = this.javaClass.simpleName) {
        for((key, name) in listProperties()) {
            val keyName = "$groupName.$name"
            props.setProperty(keyName, key.convertToString())
        }
    }
}
