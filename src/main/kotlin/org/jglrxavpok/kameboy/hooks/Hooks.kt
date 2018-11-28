package org.jglrxavpok.kameboy.hooks

import org.jglrxavpok.kameboy.Gameboy
import java.util.*

interface HookListener {
    fun handleEvent(event: HookEvent)
}

interface HookEvent

class Hooks(val owner: Gameboy) {

    val listeners = LinkedList<HookListener>()

    fun fireEvent(event: HookEvent) {
        listeners.forEach { it.handleEvent(event) }
    }

    inline fun <reified T: HookEvent> registerHookHandler(crossinline handle: (T) -> Unit) {
        val listener = object: HookListener {
            override fun handleEvent(event: HookEvent) {
                if(event is T) {
                    handle(event)
                }
            }
        }
        listeners += listener
    }

}