package org.jglrxavpok.kameboy.ui

import org.lwjgl.glfw.GLFW.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer


class Joystick(val id: Int) {

    val name: String
        get() = glfwGetJoystickName(id)!!
    var hats: ByteBuffer = ByteBuffer.allocate(0)
        internal set
    var axes: FloatBuffer = FloatBuffer.allocate(0)
        internal set
    var buttons: ByteBuffer = ByteBuffer.allocate(0)
        internal set
    var connected: Boolean = false
        internal set
    private var previousHats: ByteBuffer = ByteBuffer.allocate(0)
    private var previousAxes: FloatBuffer = FloatBuffer.allocate(0)
    private var previousButtons: ByteBuffer = ByteBuffer.allocate(0)

    fun button(index: Int): Boolean = buttons[index].toInt() == GLFW_PRESS

    fun axis(index: Int): Float = axes[index]

    fun hat(index: Int): Byte = hats[index]

    fun savePreviousState() {
        if(previousHats.capacity() < hats.capacity())
            previousHats = ByteBuffer.allocate(hats.capacity())
        if(previousAxes.capacity() < axes.capacity())
            previousAxes = FloatBuffer.allocate(axes.capacity())
        if(previousButtons.capacity() < buttons.capacity())
            previousButtons = ByteBuffer.allocate(buttons.capacity())

        for(i in 0 until buttons.capacity())
            previousButtons.put(i, buttons[i])
        for(i in 0 until hats.capacity())
            previousHats.put(i, hats[i])
        for(i in 0 until axes.capacity())
            previousAxes.put(i, axes[i])
    }

    fun findFirstIntersection(): Pair<Int, Component>? {
        val buttonID = (0 until previousButtons.capacity()).firstOrNull { previousButtons[it] != buttons[it] }
        if(buttonID != null) return updateState(buttonID to Component.BUTTON)

       val hatID = (0 until previousHats.capacity()).firstOrNull { previousHats[it] != hats[it] }
       if(hatID != null) return updateState(hatID to Component.HAT)

        val axisID = (0 until previousAxes.capacity()).firstOrNull { previousAxes[it] != axes[it] }
        if(axisID != null) return updateState(axisID to Component.AXIS)

        return null
    }

    private fun updateState(pair: Pair<Int, Component>): Pair<Int, Component> {
        when(pair.second) {
            Component.BUTTON -> previousButtons.put(pair.first, buttons[pair.first])
            Component.AXIS -> previousAxes.put(pair.first, axes[pair.first])
            Component.HAT -> previousHats.put(pair.first, hats[pair.first])
        }
        return pair
    }

    enum class Component {
        HAT, AXIS, BUTTON
    }
}
