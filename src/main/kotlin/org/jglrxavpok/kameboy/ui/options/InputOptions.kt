package org.jglrxavpok.kameboy.ui.options

import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack

object InputOptions : NuklearTab() {
    override val title = "Input"

    override fun renderTab(context: NkContext, stack: MemoryStack) {
        nk_label(context, "WIP", NK_TEXT_ALIGN_LEFT)
    }


}
