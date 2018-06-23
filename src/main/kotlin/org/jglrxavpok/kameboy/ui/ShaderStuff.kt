package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.KameboyMain
import org.lwjgl.opengl.GL20

fun LoadShader(name: String): Int {
    val id = GL20.glCreateProgram()
    val vertID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
    val fragID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
    fun loadShaderPart(shaderID: Int, sourceID: String) {
        val source = KameboyMain.javaClass.getResourceAsStream("/shaders/$sourceID.glsl").bufferedReader().use { it.readText() }
        GL20.glShaderSource(shaderID, source)
        if(GL20.glGetShaderi(shaderID, GL20.GL_LINK_STATUS) == 0) {
            println(GL20.glGetShaderInfoLog(shaderID))
        }
        GL20.glCompileShader(shaderID)
    }

    loadShaderPart(vertID, "$name.vert")
    loadShaderPart(fragID, "$name.frag")

    GL20.glAttachShader(id, vertID)
    GL20.glAttachShader(id, fragID)
    GL20.glLinkProgram(id)
    if(GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == 0) {
        println(GL20.glGetProgramInfoLog(id))
    }
    GL20.glDeleteShader(vertID)
    GL20.glDeleteShader(fragID)
    return id

}