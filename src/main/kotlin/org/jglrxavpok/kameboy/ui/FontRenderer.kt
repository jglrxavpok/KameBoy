package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.KameboyCore
import org.jglrxavpok.kameboy.KameboyMain
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30
import javax.imageio.ImageIO

object FontRenderer {

    private var shaderID: Int = -1
    private var fontAtlas: Int = -1
    private var characterMesh: Int = -1
    private var minUVUniform: Int = -1
    private var maxUVUniform: Int = -1
    private var charPosUniform: Int = -1
    private var screenSizeUniform: Int = -1
    private var colorUniform: Int = -1
    private var scaleUniform: Int = -1

    fun init() {
        shaderID = LoadShader("text")
        glUseProgram(shaderID)
        fontAtlas = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontAtlas)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        val atlas = ImageIO.read(javaClass.getResourceAsStream("/images/font.png"))
        val pixelData = BufferUtils.createIntBuffer(4 * atlas.width * atlas.height)
        val rgb = atlas.getRGB(0, 0, atlas.width, atlas.height, null, 0, atlas.width)
        for(color in rgb) {
            val alpha = (color shr 24) and 0xFF
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            val finalColor = alpha or (blue shl 8) or (green shl 16) or (red shl 24)
            pixelData.put(finalColor)
        }
        pixelData.flip()
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 256, 1536, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelData)

        characterMesh = createCharacterMesh()
        minUVUniform = glGetUniformLocation(shaderID, "minUV")
        maxUVUniform = glGetUniformLocation(shaderID, "maxUV")
        charPosUniform = glGetUniformLocation(shaderID, "offset")
        screenSizeUniform = glGetUniformLocation(shaderID, "screenSize")
        colorUniform = glGetUniformLocation(shaderID, "textColor")
        scaleUniform = glGetUniformLocation(shaderID, "scale")
    }

    private fun createCharacterMesh(): Int {
        val vaoID = GL30.glGenVertexArrays()
        val vertexBufferID = GL15.glGenBuffers()

        val indexBufferID = GL15.glGenBuffers()
        GL30.glBindVertexArray(vaoID)

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferID)
        GL20.glEnableVertexAttribArray(0)
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0)

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
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_DYNAMIC_DRAW)
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_DYNAMIC_DRAW)
        return vaoID
    }

    fun drawString(text: String, x: Float, y: Float, r: Float = 1f, g: Float = 1f, b: Float = 1f, scale: Float = 1f) {
        glUseProgram(shaderID)
        GL30.glBindVertexArray(characterMesh)
        val (screenWidth, screenHeight) = KameboyCore.CoreInstance.getScreenSize()
        glUniform2f(screenSizeUniform, screenWidth.toFloat(), screenHeight.toFloat())
        glUniform3f(colorUniform, r, g, b)
        glUniform1f(scaleUniform, scale)

        GL11.glBindTexture(GL_TEXTURE_2D, fontAtlas)

        val spacing = -4f * scale
        for((position, c) in text.withIndex()) {
            val xoffset = (16f + spacing) * scale * position
            val index = if(c == ' ') 1535 else c.toInt()
            val xIndex = index % 16
            val yIndex = index / 16
            val minU = xIndex*16f/ 256f
            val maxU = (xIndex+1)*16f/ 256f
            val minV = yIndex*16f/ 1536f
            val maxV = (yIndex+1)*16f/ 1536f
            glUniform2f(minUVUniform, minU, minV)
            glUniform2f(maxUVUniform, maxU, maxV)
            glUniform2f(charPosUniform, x+xoffset, y)

            GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0)
        }
    }
}