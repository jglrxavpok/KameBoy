package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.memory.MemoryMapper
import org.jglrxavpok.kameboy.processing.CPU
import org.jglrxavpok.kameboy.processing.GameBoyTimer
import org.jglrxavpok.kameboy.processing.video.Video

class Gameboy(val cartridge: Cartridge, val input: PlayerInput, val outputSerial: Boolean = false) {

    val isCGB = cartridge.isForColorGB // TODO maybe add a way to select?
    val inCGBMode = cartridge.isForColorGB
    val mapper = MemoryMapper(this)
    val interruptManager = mapper.interruptManager
    val cpu = CPU(this)
    val video = Video(this)
    val timer = GameBoyTimer(mapper)

    fun step(): Int {
        val clockCycles = cpu.step()
        val speedFactor = mapper.currentSpeedFactor
        val adjustedSpeed = clockCycles/speedFactor
        timer.step(clockCycles)

        // video & sound are not affected by speed change
        cartridge.cartrigeType.tick(adjustedSpeed) // external crystals (eg. MBC3)
        mapper.serialIO.step(adjustedSpeed)
        video.step(adjustedSpeed)
        mapper.sound.step(adjustedSpeed)
        mapper.step(adjustedSpeed)
        return adjustedSpeed
    }

    fun reset() {
        cpu.reset()
    }

}