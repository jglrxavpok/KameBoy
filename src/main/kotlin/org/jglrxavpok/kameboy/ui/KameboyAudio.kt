package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.sound.Sound
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine

/**
 * Adapted from https://raw.githubusercontent.com/trekawek/coffee-gb/master/src/main/java/eu/rekawek/coffeegb/gui/AudioSystemSoundOutput.java
 */
class KameboyAudio(val sound: Sound) {
    private val SAMPLE_RATE = 22050

    private val BUFFER_SIZE = 1024

    private val FORMAT = AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, SAMPLE_RATE.toFloat(), 8, 2, 2, SAMPLE_RATE.toFloat(), false)

    private var line: SourceDataLine

    private var buffer: ByteArray

    private var i: Int = 0

    private var tick: Int = 0

    private var divider: Int = 0

    init {
        try {
            line = AudioSystem.getSourceDataLine(FORMAT)
            line.open(FORMAT, BUFFER_SIZE)
        } catch (e: LineUnavailableException) {
            throw RuntimeException(e)
        }

        line.start()
        buffer = ByteArray(line.bufferSize)
        divider = SAMPLE_RATE.toClockCycles()
        sound.play = this::playSample
    }

    private fun playSample(left: Byte, right: Byte) {
        if (tick++ != 0) {
            tick %= divider
            return
        }

        buffer[i++] = (left/8).toByte()
        buffer[i++] = (right/8).toByte()
        if (i > BUFFER_SIZE / 2) {
            line.write(buffer, 0, i)
            i = 0
        }
    }


}