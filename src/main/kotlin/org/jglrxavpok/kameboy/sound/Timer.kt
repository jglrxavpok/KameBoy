package org.jglrxavpok.kameboy.sound

import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.helpful.toClockCycles

class Timer(var period: Int, val outputClock: Timer.() -> Unit) {

    internal var counter = 0

    fun step(cycles: Int) {
        counter -= cycles
        if(counter <= 0) {
            counter += period
            outputClock()
        }
    }

    fun reset() {
        counter = period
    }
}