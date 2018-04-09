package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.processing.CPU
import org.jglrxavpok.kameboy.processing.GameBoyTimer
import org.jglrxavpok.kameboy.processing.Video
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class EmulatorCore(val cartridge: Cartridge, val input: PlayerInput, val renderRoutine: EmulatorCore.(IntArray) -> Unit) {
    companion object {
        val CpuClockSpeed = 4194304 // Clock cycles / second
        val VideoVSync = 59.73 // updates per second

        val ClockCyclesPerFrame = CpuClockSpeed / VideoVSync
    }

    val mapper = MemoryMapper(cartridge, input)
    val cpu = CPU(mapper, mapper.interruptManager)
    val video = Video(mapper, mapper.interruptManager)
    val timer = GameBoyTimer(mapper)

    fun frame() {
        var totalClockCycles = 0
        while(totalClockCycles < ClockCyclesPerFrame) {
            val clockCycles = step()
            totalClockCycles += clockCycles
        }
        renderRoutine(video.pixelData)
    }

    fun step(): Int {
        val clockCycles = cpu.step()
        video.step(clockCycles)
        timer.step(clockCycles)
        mapper.sound.step(clockCycles)
        return clockCycles
    }

    fun init() {
        cpu.reset()
        video.drawLogo()
    }

    private lateinit var task: TimerTask

    fun loop() {
        val timer = Timer()
        task = timer.scheduleAtFixedRate(0, 16) {
            frame()
        }
    }

    fun stop() {
        task.cancel()
    }
}