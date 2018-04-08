package org.jglrxavpok.kameboy.processing

class Serial {
    private var currentCycleCount = 0

    fun step(cycles: Int) {
        currentCycleCount += cycles
    }
}