package org.jglrxavpok.kameboy

import org.jglrxavpok.kameboy.ui.FontRenderer

data class Message(val text: String, var duration: Float)

class MessageSystem {
    private val messages = mutableListOf<Message>()

    fun message(text: String, duration: Float = 1f) {
        messages.add(0, Message(text, duration))
        println(text)
    }

    fun step(delta: Float) {
        messages.forEach { it.duration -= delta }
        messages.removeIf { it.duration < 0f }
    }

    fun drawMessages() {
        val textScale = 2f
        val startX = 5f
        val startY = 5f
        if(messages.size == 1) {
            FontRenderer.drawString(messages[0].text, startX, startY, r = 1f, g = 0.1f, b = 0.1f, scale = textScale)
        } else {
            messages.forEachIndexed { index, message ->
                val txt = message.text
                val yOffset = index * textScale * 16f
                FontRenderer.drawString("(${index+1}) $txt", startX, startY+yOffset, r = 1f, g = 0.1f, b = 0.1f, scale = textScale)
            }
        }
    }
}