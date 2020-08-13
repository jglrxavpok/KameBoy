package org.jglrxavpok.kameboy

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.lang.Thread.sleep

object MinestomRenderer {

    private var vertexBuffer: Int = 0
    private var indexBuffer: Int = 0
    private var diffuseTextureUniform: Int = 0
    private var shader: Int = 0
    private var texture: Int = 0
    private var mesh: Int = 0
    private var init = false

    fun renderEmulatorContentsToTexture() {
        if(!init) {
            if(KameboyCore.isInitialized()) {
                val core = KameboyCore.CoreInstance
                shader = core.loadShader()
                GL20.glUseProgram(shader)
                diffuseTextureUniform = GL20.glGetUniformLocation(shader, "diffuse")
                texture = core.prepareTexture()


                val vertexBufferID = GL20.glGenBuffers()

                val indexBufferID = GL20.glGenBuffers()
                GL20.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferID)
                GL20.glEnableVertexAttribArray(0)
                GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 2 * 4, 0)

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
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL20.GL_DYNAMIC_DRAW)
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL20.GL_DYNAMIC_DRAW)

                this.vertexBuffer = vertexBufferID
                this.indexBuffer = indexBufferID
                init = true

                println("Initialized Minestom rendering")
            }
        }

        if(!init)
            return
        val core = KameboyCore.CoreInstance
        GL11.glClearColor(1f, 0f, 0f, 1f)
        glClear(GL11.GL_COLOR_BUFFER_BIT)

        val videoData = if(core.core is NoGameCore) {
            core.noGameImage
        } else {
            core.core.gameboy.video.pixelData
        }
        updateTexture(videoData)

        GL20.glUseProgram(shader)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texture)
        GL20.glUniform1i(diffuseTextureUniform, 0)

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

        GL20.glUseProgram(0)
    }

    private fun updateTexture(videoData: IntArray) {
        val data by lazy { IntArray(256*256) }
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texture)
        for((index, color) in videoData.withIndex()) {
            val correctFormatColor = color and 0xFFFFFF
            data[index] = correctFormatColor // or 0xFF
        }
        GL20.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 256, 256, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, data)
    }

}
