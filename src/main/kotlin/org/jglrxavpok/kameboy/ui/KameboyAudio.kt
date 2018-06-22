package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.sound.Sound
import org.jglrxavpok.kameboy.ui.options.SoundOptions
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class KameboyAudio(var sound: Sound) {

    companion object {
        val nullstr: String? = null
        val nullintarray: IntArray? = null

        val Format = AL_FORMAT_STEREO8
        val SampleRate = 22050
        const val MaxOpenALBufferCount = 3

        val tickDivider = SampleRate.toClockCycles()

        // From Dolphin
        const val MAX_SAMPLES = 1024 * 2  // 128 ms
        const val INDEX_MASK = MAX_SAMPLES * 2 - 1
    }

    private var indexWrite = 0
    private var indexRead = 0
    private var tick = 0
    private var alContext: Long = 0
    private val data = ByteArray(MAX_SAMPLES*2)

    private lateinit var soundThread: Thread

    fun start() {
        sound.play = this::playSample

        val device = alcOpenDevice(nullstr)
        if(device == NULL) {
            error("No default OpenAL device")
        }
        alContext = alcCreateContext(device, nullintarray)
        if(!alcMakeContextCurrent(alContext)) {
            error("Could not make OpenAL context current")
        }

        val capa = ALC.createCapabilities(device)
        AL.createCapabilities(capa)

        soundThread = thread(isDaemon = true, name = "OpenAL Audio Thread", block = this::soundLoop)
    }

    private fun soundLoop() {
        val framesPerBuffer = SampleRate / 20 / MaxOpenALBufferCount
        var countQueuedBuffers = 0
        val alSource = alGenSources()

        val buffers = IntArray(MaxOpenALBufferCount)
        alGenBuffers(buffers)

        var nextBuffer = 0

        val frameData = BufferUtils.createByteBuffer(framesPerBuffer*2)
        var running = true
        while(running) {
            alSourcef(alSource, AL_GAIN, SoundOptions.volumeSlider.value/100f)
            val countProcessedBuffers = alGetSourcei(alSource, AL_BUFFERS_PROCESSED)
            if(countQueuedBuffers == MaxOpenALBufferCount && countProcessedBuffers == 0) {
                try {
                    sleep(2)
                } catch (e: InterruptedException) {
                    running = false
                }
                continue
            }

            if(countProcessedBuffers > 0) {
                // unqueued buffer list is discarded
                MemoryStack.stackPush().use {
                    val unqueuedBuffers = it.callocInt(countProcessedBuffers)
                    alSourceUnqueueBuffers(alSource, unqueuedBuffers)

                    countQueuedBuffers -= countProcessedBuffers
                }
            }

            frameData.position(0)
            frameData.limit(framesPerBuffer*2)
            val framesWritten = getSamples(frameData, framesPerBuffer)
            if(framesWritten == 0)
                continue
            alBufferData(buffers[nextBuffer], Format, frameData, SampleRate)

            alSourceQueueBuffers(alSource, buffers[nextBuffer])

            countQueuedBuffers++
            nextBuffer = (nextBuffer+1) % MaxOpenALBufferCount

            if(alGetSourcei(alSource, AL_SOURCE_STATE) != AL_PLAYING) {
                alSourcePlay(alSource)
            }
        }

        alDeleteBuffers(buffers)
        alDeleteSources(alSource)
        alcDestroyContext(alContext)
    }

    private fun getSamples(target: ByteBuffer, numSamples: Int): Int {
        var currentSample = 0
        val localIndexWrite = indexWrite // save in local memory to avoid concurrency issues
        while (currentSample < numSamples * 2 && ((localIndexWrite - indexRead) and INDEX_MASK) > 2)
        {
            val sampleLeft = data[indexRead and INDEX_MASK]
            target.put(sampleLeft)

            val sampleRight = data[(indexRead+1) and INDEX_MASK]
            target.put(sampleRight)
            indexRead += 2
            currentSample += 2
        }

        val actualSamples = currentSample/2
        // padding
        while(currentSample < numSamples *2) {
            target.put(0)
            target.put(0)
            currentSample += 2
        }

        indexRead = indexRead and INDEX_MASK
        target.position(0)
        target.limit(currentSample)
        return actualSamples
    }

    private fun playSample(left: Int, right: Int) {
        if(tick++ != 0) {
            tick %= tickDivider
            return
        }

        val localIndexRead = indexRead
        if (2 + (indexWrite - localIndexRead and INDEX_MASK) >= MAX_SAMPLES * 2)
            return
        data[indexWrite and INDEX_MASK] = left.toByte()
        data[(indexWrite+1) and INDEX_MASK] = right.toByte()
        indexWrite = (indexWrite+2) and INDEX_MASK
    }

    fun cleanup() {
        soundThread.interrupt()
    }

    fun reloadGBSound(sound: Sound) {
        this.sound.play = {_,_->}

        this.sound = sound
        sound.play = ::playSample
    }

}