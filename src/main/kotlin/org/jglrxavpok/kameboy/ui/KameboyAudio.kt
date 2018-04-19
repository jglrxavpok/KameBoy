package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.sound.Sound
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import java.lang.Thread.yield
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class KameboyAudio(val sound: Sound) {

    companion object {
        val nullstr: String? = null
        val nullintarray: IntArray? = null

        val Format = AL_FORMAT_STEREO8
        val SampleRate = 48000
        val BufferSize = 2048

        val tickDivider = (SampleRate).toClockCycles()
    }

    private val alSource: Int
    private var index = 0
    private var tick = 0
    private val internalBuffer = ByteArray(BufferSize)
    private val bufferPool = SoundBufferPool()

    init {
        sound.play = this::playSample

        // TODO: move to start method
        // TODO: cleanup

        val device = alcOpenDevice(nullstr)
        if(device == NULL) {
            error("No default OpenAL device")
        }
        val context = alcCreateContext(device, nullintarray)
        if(!alcMakeContextCurrent(context)) {
            error("Could not make OpenAL context current")
        }

        val capa = ALC.createCapabilities(device)
        AL.createCapabilities(capa)
        alListener3f(AL_POSITION, 0f, 0f, 0f)
        alListener3f(AL_VELOCITY, 0f, 0f, 0f)

        val orientation = floatArrayOf(0f,0f,1f,0f,1f,0f)
        alListenerfv(AL_ORIENTATION, orientation)

        alSource = alGenSources()
        alSourcef(alSource, AL_PITCH, 1f)
        alSourcef(alSource, AL_GAIN, 0.25f) // TODO: configurable
        alSource3f(alSource, AL_POSITION, 0f, 0f, 0f)
        alSource3f(alSource, AL_VELOCITY, 0f, 0f, 0f)
        alSourcei(alSource, AL_LOOPING, AL_FALSE)

       /* thread {
            while(true) { // TODO: add a way to stop

                yield()
            }
        }*/
    }

    private fun playSample(left: Byte, right: Byte) {
        if(tick++ != 0) {
            tick %= tickDivider
            return
        }
        internalBuffer[index++] = (left/2).toByte()
        internalBuffer[index++] = (right/2).toByte()

        if(index > BufferSize/2) {
            val processedBuffers = alGetSourcei(alSource, AL_BUFFERS_PROCESSED)
            repeat(processedBuffers) {
                val bufferID = alSourceUnqueueBuffers(alSource)
                bufferPool.free(bufferID)
            }

            alSourceQueueBuffers(alSource, soundBuffer())
            index = 0

            if(alGetSourcei(alSource, AL_SOURCE_STATE) != AL_PLAYING) {
                alSourcePlay(alSource)
            }
        }
    }

    private fun soundBuffer(): Int {
        val buffer = bufferPool.getOrNull() ?: newBuffer()
        val data = BufferUtils.createByteBuffer(index)
        for(i in 0 until index)
            data.put(internalBuffer[i])
        data.rewind()
        alBufferData(buffer, Format, data, SampleRate)

        return buffer
    }

    private fun newBuffer(): Int {
        return alGenBuffers()
    }


}