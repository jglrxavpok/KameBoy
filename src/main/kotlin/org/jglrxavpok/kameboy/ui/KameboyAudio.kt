package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.sound.Sound
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.system.MemoryUtil.*

class KameboyAudio(val sound: Sound) {

    companion object {
        val nullstr: String? = null
        val nullintarray: IntArray? = null

        val Format = AL_FORMAT_STEREO8
        val SampleRate = 22050
        val BufferSize = 1024

        val tickDivider = SampleRate.toClockCycles()
    }

    private var alSource = -1
    private var index = 0
    private var tick = 0
    private val internalBuffer = ByteArray(BufferSize)
    private val bufferPool = SoundBufferPool()
    private var alContext: Long = 0

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

        alSource = alGenSources()
        alSourcef(alSource, AL_PITCH, 1f)
        alSourcef(alSource, AL_GAIN, 0.125f) // TODO: configurable
        alSource3f(alSource, AL_POSITION, 0f, 0f, 0f)
        alSource3f(alSource, AL_VELOCITY, 0f, 0f, 0f)
        alSourcei(alSource, AL_LOOPING, AL_FALSE)
    }

    private fun playSample(left: Int, right: Int) {

        // TODO: periodically unqueue all buffers to be in sync
        if(tick++ != 0) {
            tick %= tickDivider
            return
        }
        internalBuffer[index++] = (left).toByte()
        internalBuffer[index++] = (right).toByte()

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
        // TODO: read from end
        // take a look at https://github.com/dolphin-emu/dolphin/tree/master/Source/Core/AudioCommon
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

    fun cleanup() {
        alDeleteSources(alSource)
        alcDestroyContext(alContext)
    }

}