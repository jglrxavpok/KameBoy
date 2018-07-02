package org.jglrxavpok.kameboy.ui

import org.lwjgl.glfw.GLFW

const val XBoxA = 0
const val XBoxB = 1
const val XBoxX = 2
const val XBoxY = 3
const val XBoxLeftBumper = 4
const val XBoxRightBumper = 5
const val XBoxSelect = 6
const val XBoxStart = 7

const val XBoxLeftX = 0
const val XBoxLeftY = 1
const val XBoxRightX = 2
const val XBoxRightY = 3
const val XBoxLeftTrigger = 4
const val XBoxRightTrigger = 5

const val GbPressBit = 0
const val GbReleaseBit = 1

interface GameboyControls {

    fun pressA()
    fun pressB()
    fun releaseA()
    fun releaseB()

    fun pressStart()
    fun releaseStart()
    fun pressSelect()
    fun releaseSelect()

    fun pressLeft()
    fun releaseLeft()
    fun pressUp()
    fun releaseUp()
    fun pressDown()
    fun releaseDown()
    fun pressRight()
    fun releaseRight()
}

interface ControllerMapping {
    fun handleButtonChange(button: Int, buttonState: Boolean, gbControls: GameboyControls)
    fun handleAxisChange(axis: Int, axisValue: Float, gbControls: GameboyControls)
}

object DefaultXboxMapping: ControllerMapping {
    override fun handleButtonChange(button: Int, pressed: Boolean, gbControls: GameboyControls) {
        when(button) {
            XBoxA, XBoxX -> {
                if(pressed)
                    gbControls.pressA()
                else
                    gbControls.releaseA()
            }
            XBoxB, XBoxY -> {
                if(pressed)
                    gbControls.pressB()
                else
                    gbControls.releaseB()
            }
            XBoxSelect -> {
                if(pressed)
                    gbControls.pressSelect()
                else
                    gbControls.releaseSelect()
            }
            XBoxStart -> {
                if(pressed)
                    gbControls.pressStart()
                else
                    gbControls.releaseStart()
            }
        }

    }

    override fun handleAxisChange(axis: Int, axisValue: Float, gbControls: GameboyControls) {
        // XBOX 360 only for now
        when(axis) {
            XBoxLeftX -> {
                when {
                    axisValue >= 0.25f -> {
                        gbControls.pressRight()
                        gbControls.releaseLeft()
                    }
                    axisValue <= -0.25f -> {
                        gbControls.pressLeft()
                        gbControls.releaseRight()
                    }
                    else -> {
                        gbControls.releaseLeft()
                        gbControls.releaseRight()
                    }
                }
            }
            XBoxLeftY -> {
                when {
                    axisValue >= 0.25f -> {
                        gbControls.pressDown()
                        gbControls.releaseUp()
                    }
                    axisValue <= -0.25f -> {
                        gbControls.pressUp()
                        gbControls.releaseDown()
                    }
                    else -> {
                        gbControls.releaseDown()
                        gbControls.releaseUp()
                    }
                }
            }
        }
    }
}

const val JoyConDown = 0
const val JoyConRight = 1
const val JoyConLeft = 2
const val JoyConUp = 3
const val JoyConSL = 4
const val JoyConSR = 5
const val JoyConRorL = 14
const val JoyConZRorZL = 15
const val JoyConAxisUpAsButton = 16
const val JoyConAxisDownAsButton = 18
const val JoyConAxisLeftAsButton = 19
const val JoyConAxisRightAsButton = 17
const val JoyConStickClick = 11

const val JoyConPlus = 9
const val JoyConHome = 12

object JoyConMapping: ControllerMapping {
    override fun handleButtonChange(button: Int, pressed: Boolean, gbControls: GameboyControls) {
        when(button) {
            JoyConDown -> if(pressed) gbControls.pressB() else gbControls.releaseB()
            JoyConRight -> if(pressed) gbControls.pressA() else gbControls.releaseA()
            JoyConLeft, JoyConSL -> if(pressed) gbControls.pressSelect() else gbControls.releaseSelect()
            JoyConUp, JoyConSR -> if(pressed) gbControls.pressStart() else gbControls.releaseStart()
            JoyConAxisUpAsButton -> if(pressed) gbControls.pressUp() else gbControls.releaseUp()
            JoyConAxisLeftAsButton -> if(pressed) gbControls.pressLeft() else gbControls.releaseLeft()
            JoyConAxisRightAsButton -> if(pressed) gbControls.pressRight() else gbControls.releaseRight()
            JoyConAxisDownAsButton -> if(pressed) gbControls.pressDown() else gbControls.releaseDown()
        }
    }

    override fun handleAxisChange(axis: Int, axisValue: Float, gbControls: GameboyControls) {
        //println(">> axis $axis -> $axisValue")
        // Apparently, when connected over Bluetooth, the stick is considered as a set of buttons
    }

}

fun GetControllerMapping(joystick: Joystick): ControllerMapping {
    // TODO: custom controls
    return when {
        "Joy-Con" in joystick.name
                || GLFW.glfwGetJoystickGUID(joystick.id) == "030000007e0500000620000000000000"
                || GLFW.glfwGetJoystickGUID(joystick.id) == "030000007e0500000720000000000000"
        -> JoyConMapping
        else -> DefaultXboxMapping
    }
}