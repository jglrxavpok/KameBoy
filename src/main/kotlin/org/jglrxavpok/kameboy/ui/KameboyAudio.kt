package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.sound.Sound
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import java.lang.Thread.sleep
import java.lang.Thread.yield
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.thread

class KameboyAudio(val sound: Sound) {

    companion object {
        val nullstr: String? = null
        val nullintarray: IntArray? = null

        val Format = AL_FORMAT_STEREO8
        val SampleRate = 48000
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
        alListener3f(AL_POSITION, 0f, 0f, 0f)
        alListener3f(AL_VELOCITY, 0f, 0f, 0f)

        val orientation = floatArrayOf(0f,0f,1f,0f,1f,0f)
        alListenerfv(AL_ORIENTATION, orientation)

        soundThread = thread(isDaemon = true, name = "OpenAL Audio Thread", block = this::soundLoop)
    }

    private fun soundLoop() {
        val framesPerBuffer = SampleRate / 20 / MaxOpenALBufferCount
        var countQueuedBuffers = 0
        val alSource = alGenSources()
        //alSourcef(alSource, AL_PITCH, 1f)
        alSourcef(alSource, AL_GAIN, 0.125f) // TODO: configurable
        /*alSource3f(alSource, AL_POSITION, 0f, 0f, 0f)
        alSource3f(alSource, AL_VELOCITY, 0f, 0f, 0f)
        alSourcei(alSource, AL_LOOPING, AL_FALSE)*/

        val buffers = IntArray(MaxOpenALBufferCount)
        alGenBuffers(buffers)

        var nextBuffer = 0

        val frameData = BufferUtils.createByteBuffer(framesPerBuffer*2)
        while(true) { // TODO: stoppable
            val countProcessedBuffers = alGetSourcei(alSource, AL_BUFFERS_PROCESSED)
            if(countQueuedBuffers == MaxOpenALBufferCount && countProcessedBuffers == 0) {
                try {
                    sleep(1)
                } catch (e: InterruptedException) {
                    alDeleteSources(alSource)
                    return
                }
                //yield()
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
    }

    private inline fun getSamples(target: ByteBuffer, numSamples: Int): Int {
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
        val s0 = data[(indexRead-2) and INDEX_MASK]
        val s1 = data[(indexRead-1) and INDEX_MASK]
        while(currentSample < numSamples *2) {
            target.put(s0)
            target.put(s1)

            currentSample += 2
        }

        indexRead = indexRead and INDEX_MASK
        target.position(0)
        target.limit(currentSample)
        return actualSamples
    }

    private fun playSample(left: Int, right: Int) {
        // TODO: periodically unqueue all buffers to be in sync?
        if(tick++ != 0) {
            tick %= tickDivider
            return
        }

        val numSamples = 1
        val localIndexRead = indexRead
        if (numSamples * 2 + (indexWrite - localIndexRead and INDEX_MASK) >= MAX_SAMPLES * 2)
            return
        data[indexWrite and INDEX_MASK] = left.toByte()
        data[(indexWrite+1) and INDEX_MASK] = right.toByte()
        indexWrite += numSamples * 2
        indexWrite = indexWrite and INDEX_MASK
    }

    fun cleanup() {
        soundThread.interrupt()
        alcDestroyContext(alContext)
    }

}