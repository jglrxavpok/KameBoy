package org.jglrxavpok.kameboy.memory

interface Rumble {
    fun turnMotorOn()
    fun turnMotorOff()
}

object NoRumble: Rumble {
    override fun turnMotorOn() { }

    override fun turnMotorOff() { }
}

object ControllerRumble: Rumble {
    override fun turnMotorOn() {
        // TODO
    }

    override fun turnMotorOff() {
        // TODO
    }
}