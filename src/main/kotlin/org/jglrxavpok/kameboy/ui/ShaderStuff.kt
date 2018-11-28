package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.KameboyMain
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL11.*

fun LoadShader(name: String): Int {
    val versionString = glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)!!.replace(".", "")
    println("Attempting to inject version $versionString into shader $name")
    val id = GL20.glCreateProgram()
    val vertID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
    val fragID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
    fun loadShaderPart(shaderID: Int, sourceID: String) {
        val source = KameboyMain.javaClass.getResourceAsStream("/shaders/$sourceID.glsl").bufferedReader().use { it.readText() }
        GL20.glShaderSource(shaderID, "#version $versionString\n", source)
        GL20.glCompileShader(shaderID)
        if(GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == 0) {
            println("Error while compiling $sourceID:")
            println(GL20.glGetShaderInfoLog(shaderID))
        }
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