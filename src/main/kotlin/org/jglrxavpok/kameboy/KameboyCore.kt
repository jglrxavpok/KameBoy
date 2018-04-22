package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.helpful.nullptr
import org.jglrxavpok.kameboy.helpful.setBits
import org.jglrxavpok.kameboy.helpful.toBit
import org.jglrxavpok.kameboy.input.PlayerInput
import org.jglrxavpok.kameboy.memory.Cartridge
import org.jglrxavpok.kameboy.processing.Instructions
import org.jglrxavpok.kameboy.processing.video.Palettes
import org.jglrxavpok.kameboy.ui.*
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import java.awt.FlowLayout
import java.io.File
import java.lang.Thread.yield
import java.nio.ByteBuffer
import javax.swing.*

class KameboyCore(val args: Array<String>): PlayerInput {
    private var window: Long
    private val cartridge = _DEV_cart("Pokemon Red.gb")
    private val core = EmulatorCore(cartridge, this, outputSerial = "-outputserial" in args, renderRoutine = { pixels -> updateTexture(this /* emulator core */, pixels) })
    private var shaderID: Int
    private var textureID: Int
    private var meshID: Int
    private var diffuseTextureUniform: Int
    private val audioSystem: KameboyAudio
    private var paletteIndex = 0
    private val joysticks = Array(10, ::Joystick)

    init {
        val scale = 6
        window = glfwCreateWindow(160*scale, 144*scale, "Kameboy - ${cartridge.title}", nullptr, nullptr)
        glfwSetWindowAspectRatio(window, 160, 144)
        initInput()
        glfwShowWindow(window)

        glfwMakeContextCurrent(window)
        GL.createCapabilities()
        shaderID = loadShader()
        glUseProgram(shaderID)
        diffuseTextureUniform = glGetUniformLocation(shaderID, "diffuse")
        textureID = prepareTexture()
        meshID = prepareRenderMesh()
        audioSystem = KameboyAudio(core.gameboy.mapper.sound)

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
                    GLFW_KEY_F3 -> showMemoryContents()
                    GLFW_KEY_PAGE_UP -> {
                        changePalette(paletteIndex+1)
                    }

                    GLFW_KEY_PAGE_DOWN -> {
                        changePalette(paletteIndex-1)
                    }
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

        glfwSetJoystickCallback { id, event ->
            if(event == GLFW_CONNECTED) {
                println("Joystick $id connected, name is ${glfwGetJoystickName(id)}")
                joysticks[id].connected = true
            } else if(event == GLFW_DISCONNECTED) {
                println("Joystick $id disconnected")
                joysticks[id].connected = false
            }
        }
    }

    private fun changePalette(newIndex: Int) {
        paletteIndex = newIndex
        if(paletteIndex >= Palettes.size)
            paletteIndex = 0
        if(paletteIndex < 0)
            paletteIndex = Palettes.size-1
        core.gameboy.video.dmgPalette = Palettes[paletteIndex]
        println("Now using palette $paletteIndex")
    }

    private fun showMemoryContents() {
        val frame by lazy { JFrame() }
        frame.contentPane.removeAll()
        val linesPanel = JPanel()
        linesPanel.layout = BoxLayout(linesPanel, BoxLayout.Y_AXIS)

        var address = 0
        while(address <= 0xFFFF) {
            val areaType = when(address) {
                0xFFFF, 0xFF50, 0xFF4D -> Instructions.AreaType.SpecialRegister
                in 0x8000 until 0xA000 -> Instructions.AreaType.VideoRam
                in 0xA000 until 0xC000 -> Instructions.AreaType.Ram
                in 0xFF00 until 0xFF4C -> Instructions.AreaType.IO

                else -> Instructions.AreaType.Rom
            }
            val content = Instructions.readInstruction(core.gameboy.mapper, address, areaType)
            linesPanel.add(JLabel(content.desc))
            address += content.size
        }
        frame.contentPane.add(JScrollPane(linesPanel))
        frame.repaint()
        frame.isVisible = true
    }

    private fun isButtonKey(key: Int) = key in arrayOf(GLFW_KEY_Q, GLFW_KEY_W, GLFW_KEY_ENTER, GLFW_KEY_BACKSPACE)
    private fun isDirectionKey(key: Int) = key in arrayOf(GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT)

    override var buttonState = 0xFF
    override var directionState = 0xFF

    private fun _DEV_cart(name: String): Cartridge {
        val saveFolder = File("./saves/")
        if(!saveFolder.exists())
            saveFolder.mkdirs()
        return Cartridge(_DEV_rom(name), _DEV_BOOT_ROM(), File(saveFolder, name.takeWhile { it != '.' }+".sav"))
    }
    private fun _DEV_rom(name: String) = KameboyCore::class.java.getResourceAsStream("/roms/$name").buffered().use { it.readBytes() }

    private fun _DEV_BOOT_ROM(): ByteArray? {
        val bootRomFile = File("DMG_ROM.bin")
        if(!bootRomFile.exists())
            return null
        return bootRomFile.readBytes()
    }

    private fun cleanup() {
        audioSystem.cleanup()
        glDeleteProgram(shaderID)
        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun runEmulator() {
        core.init()
        audioSystem.start()
        val windowWPointer = IntArray(1)
        val windowHPointer = IntArray(1)
        glfwSwapInterval(1)

        var time = glfwGetTime()
        var frames = 0
        var totalTime = 0.0

        while(!glfwWindowShouldClose(window)) {
            glfwGetWindowSize(window, windowWPointer, windowHPointer)
            pollEvents()
            glClearColor(0f, .8f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)
            glViewport(0, 0, windowWPointer[0], windowHPointer[0])

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, textureID)
            glUseProgram(shaderID)
            glUniform1i(diffuseTextureUniform, 0)

            glBindVertexArray(meshID)
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

            core.frame()
            glfwSwapBuffers(window)

            val newTime = glfwGetTime()
            val deltaTime = newTime-time
            time = newTime
            frames++
            totalTime += deltaTime
            if(totalTime >= 1f) {
                println("$frames fps")
                frames = 0
                totalTime %= 1f
            }
        }
    }

    private fun handleButtonChange(button: Int, state: Boolean) {
        /*
        GLFW_KEY_RIGHT, GLFW_KEY_Q -> 0
GLFW_KEY_LEFT, GLFW_KEY_W -> 1
GLFW_KEY_UP, GLFW_KEY_BACKSPACE -> 2
GLFW_KEY_DOWN, GLFW_KEY_ENTER -> 3
         */
        when(button) {
            XBoxA, XBoxX -> {
                buttonState = buttonState.setBits(1-state.toBit(), 0..0)
            }
            XBoxB, XBoxY -> {
                buttonState = buttonState.setBits(1-state.toBit(), 1..1)
            }
            XBoxSelect -> {
                buttonState = buttonState.setBits(1-state.toBit(), 2..2)
            }
            XBoxStart -> {
                buttonState = buttonState.setBits(1-state.toBit(), 3..3)
            }
        }
    }

    private fun handleAxisChange(axis: Int, axisValue: Float) {
        // XBOX 360 only for now
        when(axis) {
            XBoxLeftX -> {
                if(axisValue >= 0.25f) {
                    directionState = directionState.setBits(0, 0..0)
                    directionState = directionState.setBits(1, 1..1)
                } else if(axisValue <= -0.25f) {
                    directionState = directionState.setBits(1, 0..0)
                    directionState = directionState.setBits(0, 1..1)
                } else {
                    directionState = directionState.setBits(0b11, 0..1)
                }
            }
            XBoxLeftY -> {
                if(axisValue >= 0.25f) {
                    directionState = directionState.setBits(1, 2..2)
                    directionState = directionState.setBits(0, 3..3)
                } else if(axisValue <= -0.25f) {
                    directionState = directionState.setBits(0, 2..2)
                    directionState = directionState.setBits(1, 3..3)
                } else {
                    directionState = directionState.setBits(0b11, 2..3)
                }
            }
        }
    }

    private fun pollEvents() {
        glfwPollEvents()
        joysticks.filter { glfwJoystickPresent(it.id) }
                .forEach {
                    it.savePreviousState()
                    it.connected = true
                    it.buttons = glfwGetJoystickButtons(it.id)!!
                    it.axes = glfwGetJoystickAxes(it.id)!!
                    it.hats = glfwGetJoystickHats(it.id)!!

                    tailrec fun checkJoystickChanges() {
                        val change = it.findFirstIntersection()
                        if(change != null) {
                            when(change.second) {
                                Joystick.Component.AXIS -> {
                                    val axis = change.first
                                    val axisValue = it.axis(axis)

                                    handleAxisChange(axis, axisValue)
                                }

                                Joystick.Component.BUTTON -> {
                                    val button = change.first
                                    val buttonState = it.button(button)
                                    handleButtonChange(button, buttonState)
                                }
                            }
                            checkJoystickChanges()
                        }
                    }
                    checkJoystickChanges()
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
