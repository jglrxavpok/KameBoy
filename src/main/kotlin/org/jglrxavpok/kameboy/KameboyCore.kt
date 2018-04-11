package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.helpful.nullptr
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_BGRA
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class KameboyCore(val args: Array<String>): PlayerInput {
    private var window: Long
    private val cartridge = Cartridge(_DEV_rom("LoZ Link's Awakening.gb"))
    private val core = EmulatorCore(cartridge, this, { pixels -> updateTexture(this /* emulator core */, pixels) })
    private var shaderID: Int
    private var textureID: Int
    private var meshID: Int
    private var diffuseTextureUniform: Int
    private var scrollUniform: Int

    init {
        window = glfwCreateWindow(160*4, 144*4, "Kameboy - ${cartridge.title}", nullptr, nullptr)
        initInput()
        glfwShowWindow(window)

        glfwMakeContextCurrent(window)
        GL.createCapabilities()
        shaderID = loadShader()
        glUseProgram(shaderID)
        diffuseTextureUniform = glGetUniformLocation(shaderID, "diffuse")
        scrollUniform = glGetUniformLocation(shaderID, "scroll")
        textureID = prepareTexture()
        meshID = prepareRenderMesh()

        runEmulator()
        cleanup()
    }

    private fun prepareRenderMesh(): Int {
        val vaoID = glGenVertexArrays()
        val vertexBufferID = glGenBuffers()

        val indexBufferID = glGenBuffers()
        glBindVertexArray(vaoID)

        glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferID)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*4, 0)

        val vertexBuffer = BufferUtils.createFloatBuffer(4*(3+2))

        vertexBuffer.put(floatArrayOf(0f, 0f))

        vertexBuffer.put(floatArrayOf(1f, 0f))

        vertexBuffer.put(floatArrayOf(1f, 1f))

        vertexBuffer.put(floatArrayOf(0f, 1f))

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBufferID)
        val indexBuffer = BufferUtils.createIntBuffer(6)
        indexBuffer.put(0).put(1).put(2)
        indexBuffer.put(2).put(3).put(0)

        vertexBuffer.flip()
        indexBuffer.flip()
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW)
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW)

        return vaoID
    }

    private fun prepareTexture(): Int {
        val id = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 256, 256, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        return id
    }

    private fun loadShader(): Int {
        val id = glCreateProgram()
        val vertID = glCreateShader(GL_VERTEX_SHADER)
        val fragID = glCreateShader(GL_FRAGMENT_SHADER)
        fun loadShaderPart(shaderID: Int, sourceID: String) {
            val source = KameboyMain.javaClass.getResourceAsStream("/shaders/$sourceID.glsl").bufferedReader().use { it.readText() }
            glShaderSource(shaderID, source)
            if(glGetShaderi(shaderID, GL_LINK_STATUS) == 0) {
                println(glGetShaderInfoLog(shaderID))
            }
            glCompileShader(shaderID)
        }

        loadShaderPart(vertID, "blit.vert")
        loadShaderPart(fragID, "blit.frag")

        glAttachShader(id, vertID)
        glAttachShader(id, fragID)
        glLinkProgram(id)
        if(glGetProgrami(id, GL_LINK_STATUS) == 0) {
            println(glGetProgramInfoLog(id))
        }
        glDeleteShader(vertID)
        glDeleteShader(fragID)
        return id
    }

    private fun initInput() {
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if(action == GLFW_RELEASE) {
                when(key) {
                    GLFW_KEY_F1 -> core.dumpInfos()
                    GLFW_KEY_F2 -> core.showBGMap()
                }
            }
            val bit = when(key) {
                GLFW_KEY_RIGHT, GLFW_KEY_Q -> 0
                GLFW_KEY_LEFT, GLFW_KEY_W -> 1
                GLFW_KEY_UP, GLFW_KEY_BACKSPACE -> 2
                GLFW_KEY_DOWN, GLFW_KEY_ENTER -> 3
                else -> return@glfwSetKeyCallback
            }
            val released = when(action) {
                GLFW_PRESS -> 0
                GLFW_RELEASE -> 1
                else -> return@glfwSetKeyCallback
            }
            when {
                isButtonKey(key) -> buttonState = buttonState.setBits(released, bit..bit)
                isDirectionKey(key) -> directionState = directionState.setBits(released, bit..bit)
            }
        }
    }

    private fun isButtonKey(key: Int) = key in arrayOf(GLFW_KEY_Q, GLFW_KEY_W, GLFW_KEY_ENTER, GLFW_KEY_BACKSPACE)
    private fun isDirectionKey(key: Int) = key in arrayOf(GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT)

    override var buttonState = 0xFF
    override var directionState = 0xFF

    private fun _DEV_rom(name: String) = KameboyCore::class.java.getResourceAsStream("/roms/$name").buffered().use { it.readBytes() }

    private fun cleanup() {
        glDeleteProgram(shaderID)
        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun runEmulator() {
        core.init()
        val windowWPointer = IntArray(1)
        val windowHPointer = IntArray(1)
        glfwSwapInterval(1)

        while(!glfwWindowShouldClose(window)) {
            glfwGetWindowSize(window, windowWPointer, windowHPointer)
            glfwPollEvents()
            glClearColor(0f, .8f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glViewport(0, 0, windowWPointer[0], windowHPointer[0])

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, textureID)
            glUseProgram(shaderID)
            glUniform1i(diffuseTextureUniform, 0)
            glUniform2f(scrollUniform, core.video.scrollX.getValue().toFloat(), core.video.scrollY.getValue().toFloat())

            glBindVertexArray(meshID)
          /*  glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID)*/
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

            core.frame()
            glfwSwapBuffers(window)
        }
    }

    fun updateTexture(core: EmulatorCore, videoData: IntArray) {
        val data by lazy { IntArray(256*256) }
        glBindTexture(GL_TEXTURE_2D, textureID)
        for((index, color) in videoData.withIndex()) {
            val correctFormatColor = color and 0xFFFFFF
            data[index] = correctFormatColor
        }
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, data)
    }
}
