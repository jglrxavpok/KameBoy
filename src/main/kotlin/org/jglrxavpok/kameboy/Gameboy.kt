package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.helpful.Profiler
import org.jglrxavpok.kameboy.hooks.Hooks
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.processing.CPU
import org.jglrxavpok.kameboy.processing.GameBoyTimer
import org.jglrxavpok.kameboy.processing.video.Video
import org.jglrxavpok.kameboy.time.GetSaveStateElements

class Gameboy(val cartridge: Cartridge, val input: PlayerInput, val outputSerial: Boolean = false) {

    val hooks = Hooks(this)
    val isCGB = cartridge.isForColorGB // TODO maybe add a way to select?
    val inCGBMode = cartridge.isForColorGB
    val mapper = MemoryMapper(this)
    val interruptManager = mapper.interruptManager
    val cpu = CPU(this)
    val video = Video(this)
    val timer = GameBoyTimer(mapper)

    val saveStateElements = GetSaveStateElements(this)

    fun step(): Int {
        Profiler.start("CPU")
        val clockCycles = cpu.step()
        val speedFactor = mapper.currentSpeedFactor
        val adjustedSpeed = clockCycles/speedFactor
        Profiler.endStart("Timer")
        timer.step(clockCycles)

        Profiler.endStart("Cart controller")
        cartridge.cartrigeType.tick(adjustedSpeed) // external crystals (eg. MBC3)

        Profiler.endStart("Serial IO")
        mapper.serialIO.step(clockCycles)

        Profiler.endStart("Memory stuff")
        mapper.step(clockCycles)

        // video & sound are not affected by speed change
        Profiler.endStart("PPU")
        video.step(adjustedSpeed)
        Profiler.endStart("APU")
        mapper.sound.step(adjustedSpeed)
        Profiler.end()

        return adjustedSpeed
    }

    fun reset() {
        cpu.reset()
    }

}