package org.jglrxavpok.kameboy.time

import org.jglrxavpok.kameboy.Gameboy
import kotlin.reflect.KProperty

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SaveStateElement

fun GetSaveStateElements(gameboy: Gameboy): List<Pair<KProperty<*>, Any>> {
    val result = mutableListOf<Pair<KProperty<*>, Any>>()
    AddElements(result, gameboy.cartridge)
    AddElements(result, gameboy.cartridge.cartrigeType)
    AddElements(result, gameboy.cpu)
    AddElements(result, gameboy.timer)
    AddElements(result, gameboy.video)
    AddElements(result, gameboy.mapper)
    AddElements(result, gameboy.mapper.speedRegister)
    AddElements(result, gameboy.mapper.sound)
    AddElements(result, gameboy.mapper.hdma5)
    AddElements(result, gameboy.mapper.interruptManager)
    gameboy.mapper.sound.channels.forEach { channel ->
        AddElements(result, channel)
    }
    return result
}

private inline fun <reified T: Any> AddElements(elements: MutableList<Pair<KProperty<*>, Any>>, component: T) {
    val fields = component::class.members
    fields.filter { it is KProperty }.map { it as KProperty }.filter { field ->
        field.annotations.any { it.annotationClass == SaveStateElement::class }
    }.forEach { elements.add(Pair(it, component)) }
}