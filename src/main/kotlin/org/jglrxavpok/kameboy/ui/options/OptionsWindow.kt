package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.ui.NuklearWindow
import org.lwjgl.BufferUtils
import org.lwjgl.nuklear.NkVec2
import org.lwjgl.system.MemoryStack
import org.lwjgl.nuklear.Nuklear.*

object OptionsWindow: NuklearWindow("Kameboy Options") {
    private lateinit var nullVec: NkVec2
    override val defaultWidth = 400
    override val defaultHeight = 400

    private val roundingBuffer = BufferUtils.createFloatBuffer(1)
    private val tabs = listOf<NuklearTab>(
            GraphicsOptions,
            InputOptions,
            //MultiplayerOptions
            SoundOptions//,
            /*CheatingOptions*/
    )

    private var selectedTab: NuklearTab = GraphicsOptions

    override fun init() {
        super.init()
        nullVec = NkVec2.create()
        nullVec.set(0f, 0f)
    }

    override fun renderWindow(stack: MemoryStack) {
        roundingBuffer.put(0, context.style().button().rounding())
        roundingBuffer.rewind()
        nk_style_push_vec2(context, context.style().window().spacing(), nullVec)
        nk_style_push_float(context, roundingBuffer, 0f)

        nk_layout_row_begin(context, NK_STATIC, 30f, tabs.size)
        for(i in 0 until tabs.size) {
            val tab = tabs[i]
            if (nk_tab (context, tab.title, selectedTab == tab)) {
                selectedTab = tab
            }
        }
        nk_style_pop_float(context)
        nk_style_pop_vec2(context)

        nk_layout_row_dynamic(context, 200f, 1)
        selectedTab .renderTab(context, stack)
    }
}