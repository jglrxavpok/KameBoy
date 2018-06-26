package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.processing.video.PaletteMemory
import java.io.File
import java.io.FileInputStream
import java.util.*

object Config {

    private val settingsFile = File("./settings.cfg")
    internal val properties = Properties()

    private val groups = listOf(Audio, Rendering, System)

    fun load() {
        if(!settingsFile.exists()) {
            settingsFile.createNewFile()
            javaClass.getResourceAsStream("/defaultSettings.cfg").use { input ->
                settingsFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }

        FileInputStream(settingsFile).use {
            properties.load(it)
        }

        groups.forEach { it.load() }
    }

    fun save() {
        val output = settingsFile.outputStream().bufferedWriter()
        groups.forEach { it.save() }
        properties.store(output, "Kameboy settings")
        output.flush()
        output.close()
    }

    operator fun <T> set(key: Key<T>, value: T) {
        key.currentValue = value
    }

    operator fun <T> get(key: Key<T>): T {
        return key.currentValue
    }
}


object Audio: KeyGroup() {
    val volume = IntKey()
    val skipAudioCycles = BooleanKey()
}

object Rendering: KeyGroup() {
    val paletteIndex = IntKey()
    val CGBColorCurve = EnumKey(PaletteMemory.CGBColorCurves.values())
}

object System: KeyGroup() {
    val lastRomFolder = StringKey(defaultValue = ".")
}