package org.jglrxavpok.kameboy.ui.options

import org.lwjgl.nuklear.NkContext
import org.lwjgl.system.MemoryStack

abstract class NuklearTab {

    abstract val title: String

    abstract fun renderTab(context: NkContext, stack: MemoryStack)
}