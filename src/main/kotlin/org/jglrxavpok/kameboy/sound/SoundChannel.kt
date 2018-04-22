package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.helpful.toClockCycles
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.memory.MemoryRegister

abstract class SoundChannel(val channelNumber: Int, val length: Int, val memoryMapper: MemoryMapper) {

    val nr0 = MemoryRegister("NR${channelNumber}0", memoryMapper, addressNR(0))
    val nr1 = MemoryRegister("NR${channelNumber}1", memoryMapper, addressNR(1))
    val nr2 = MemoryRegister("NR${channelNumber}2", memoryMapper, addressNR(2))
    val nr3 = MemoryRegister("NR${channelNumber}3", memoryMapper, addressNR(3))
    val nr4 = MemoryRegister("NR${channelNumber}4", memoryMapper, addressNR(4))

    var shouldClockLength by nr4.bitVar(6)

    var channelEnabled = false
    open val dacEnabled get()= (nr2.getValue() shr 3) != 0
    protected abstract val frequencyMultiplier: Int
    open var frequency: Int = 0
        set(value) {
            field = value
            timer.period = ((2048-value)*frequencyMultiplier)//.toClockCycles()
            if(channelNumber == 3)
                timer.period /= 2
        }
    private var lengthCounter = length
    private var output: Int = 0
    protected var volume: Int = 0
    private val initialVolume get() = (nr2.getValue() shr 4) and 0xF
    private val increaseVolume by nr2.bitVar(3)
    private val enveloppePeriod get()= nr2.getValue() and 0b111
    private val volumeDirection get()= if(increaseVolume) 1 else -1
    private var frameSequencerStep = 0
    val frameSequencer = Timer(512.toClockCycles()) {
        when {
            frameSequencerStep % 2 == 0 -> clockLength()
            frameSequencerStep == 7 -> clockVolume()
            frameSequencerStep % 4 == 2 -> clockSweep()
        }
        frameSequencerStep++
        frameSequencerStep %= 8
    }

    val timer = Timer(0) {
        onOutputClock(this)
    }
    val correctVolume get()= if(enveloppePeriod == 0) initialVolume else volume

    abstract fun onOutputClock(timer: Timer)

    open fun trigger() {
        channelEnabled = dacEnabled
        if(lengthCounter <= 0)
            lengthCounter = length
        timer.reset()
        volume = initialVolume
        output = 0
    }

    fun clockLength() {
        if(shouldClockLength) {
            if(lengthCounter > 0) {
                lengthCounter--
                if(lengthCounter == 0) {
                    channelEnabled = false
                    //      println("LENGTH 0 in $channelNumber")
                }
            }
        } else {
            lengthCounter = 0
        }
    }

    private var volumeCounter = 0

    fun clockVolume() {
        if(volumeCounter++ >= enveloppePeriod) {
            volumeCounter = 0
            if(enveloppePeriod != 0) {
                val newVolume = volume + volumeDirection
                if(newVolume in 0..15) {
                    volume = newVolume
                }
            }
        }
    }

    fun loadLength(newLength: Int) {
        lengthCounter = newLength
        if(newLength == 0)
            lengthCounter = length

   //     println("LOADED LENGTH in $channelNumber: $lengthCounter")
    }

    abstract fun channelStep(cycles: Int)
    open fun clockSweep() { }

    fun step(cycles: Int): Int {
        frameSequencer.step(cycles)
        timer.step(cycles)
        channelStep(cycles)
        if(!dacEnabled)
            channelEnabled = false
        if(!channelEnabled)
            return 0x0
        return output
    }

    fun stop() {
        channelEnabled = false
    }

    protected fun output(nibble: Int) {
        output = nibble and 0xF
    }

    fun addressNR(index: Int) = 0xFF10+(channelNumber-1)*0x5+index
    fun isEnabled() = dacEnabled && channelEnabled

    open fun reset() {
        frameSequencerStep = 0
        output = 0
    }
}