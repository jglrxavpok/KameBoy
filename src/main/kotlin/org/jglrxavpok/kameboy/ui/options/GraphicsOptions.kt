package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.processing.video.ColorPalette
import org.jglrxavpok.kameboy.processing.video.PaletteMemory
import org.jglrxavpok.kameboy.processing.video.Palettes
import org.jglrxavpok.kameboy.ui.Audio
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

object GraphicsOptions : JPanel() {

    val paletteSelection = JComboBox<ColorPalette>(Palettes)
    val curveSelection = JComboBox<PaletteMemory.CGBColorCurves>(PaletteMemory.CGBColorCurves.values())

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
    }
}
