package org.jglrxavpok.kameboy

import org.lwjgl.glfw.GLFW

object KameboyMain {

    @JvmStatic
    fun main(args: Array<String>) {
        when {
            GLFW.glfwInit() -> { launchEmulator(args) }
            else -> error("Could not init GLFW!")
        }
    }

    private fun launchEmulator(args: Array<String>) {
        KameboyCore(args)
    }
}