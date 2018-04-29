package org.jglrxavpok.kameboy

import org.lwjgl.glfw.GLFW
import javax.swing.UIManager

object KameboyMain {

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Throwable) {
            // shhhh
        }
        when {
            GLFW.glfwInit() -> { launchEmulator(args) }
            else -> error("Could not init GLFW!")
        }
    }

    private fun launchEmulator(args: Array<String>) {
        KameboyCore(args)
    }
}