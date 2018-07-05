package org.jglrxavpok.kameboy.hooks

import org.jglrxavpok.kameboy.Gameboy

interface HookListener {
    fun handleEvent(event: HookEvent)
}

interface HookEvent

class Hooks(val owner: Gameboy) {

    val listeners = mutableListOf<HookListener>()

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