package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.processing.video.ColorPalette
import org.jglrxavpok.kameboy.processing.video.PaletteMemory
import org.jglrxavpok.kameboy.processing.video.Palettes
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.NkVec2
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack
import javax.swing.JComboBox


object GraphicsOptions : NuklearTab() {

    override val title = "Graphics"

    val paletteSelection = JComboBox<ColorPalette>(Palettes)
    private var selectedCurve = Config[Rendering.CGBColorCurve].ordinal

    private val tmpVec = NkVec2.create()

    override fun renderTab(context: NkContext, stack: MemoryStack) {
        if(nk_group_begin(context, "Gameboy DMG Palette", NK_WINDOW_BORDER or NK_WINDOW_TITLE)) {

            nk_group_end(context)
        }

        if(nk_group_begin(context, "Gameboy Color color curve", NK_WINDOW_BORDER or NK_WINDOW_TITLE)) {
            nk_layout_row_dynamic(context, 30f, 1)
            val curves = PaletteMemory.CGBColorCurves.values()
            if (nk_combo_begin_label(context, curves[selectedCurve].name, nk_vec2(nk_widget_width(context), 400f, tmpVec))) {
                nk_layout_row_dynamic(context, 25f, 1)
                for(i in 0 until curves.size) {
                    if (nk_combo_item_label(context, curves[i].name, NK_TEXT_LEFT)) {
                        selectedCurve = i
                        Config[Rendering.CGBColorCurve] = curves[i]
                        Config.save()
                    }
                }
                nk_combo_end(context)
            }

            nk_group_end(context)
        }
    }
/*
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        paletteSelection.selectedIndex = Config[Rendering.paletteIndex]
        paletteSelection.addActionListener {
            Config[Rendering.paletteIndex] = paletteSelection.selectedIndex
            Config.save()
        }

        curveSelection.selectedItem = Config[Rendering.CGBColorCurve]
        curveSelection.addActionListener {
            Config[Rendering.CGBColorCurve] = curveSelection.selectedItem as PaletteMemory.CGBColorCurves
            Config.save()
        }
        sub("Gameboy DMG Palette") {
            layout = FlowLayout()
            add(paletteSelection)
        }
        sub("Gameboy Color color curve") {
            layout = FlowLayout()
            add(curveSelection)
        }
    }*/
}
