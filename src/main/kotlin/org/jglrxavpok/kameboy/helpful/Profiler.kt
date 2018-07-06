package org.jglrxavpok.kameboy.helpful

import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.EmulatorCore.Companion.CpuClockSpeed

object Profiler {

    const val defaultSection = "Misc."
    private var currentSection: String = defaultSection
    private var nanoTime: Long = 0
    private val storedTimings = hashMapOf<String, Double>()

    fun start(section: String) {
        currentSection = section
        nanoTime = System.nanoTime()
    }

    fun end() {
        val sectionTime = System.nanoTime() - nanoTime
        store(currentSection, sectionTime/1000.0)
        currentSection = defaultSection
    }

    private fun store(section: String, time: Double) {
        if(section !in storedTimings) {
            storedTimings[section] = 0.0
        }
        storedTimings[section] = storedTimings[section]!! + time
    }

    fun beginFrame() {
        for(section in storedTimings.keys) {
            storedTimings[section] = 0.0
        }
    }

    fun endStart(section: String) {
        end()
        start(section)
    }

    fun dump() {
        println("=== Profiler Results Start ===")
        var totalTime: Double = 0.0
        for(section in storedTimings.keys) {
            val timing = storedTimings[section]!!
            totalTime += timing
            println("$section: ${timing/1000.0} ms")
        }
        println("\tTotal time (average): ${totalTime/1000.0} ms")
        println("=== Profiler Results End ===")
    }
}