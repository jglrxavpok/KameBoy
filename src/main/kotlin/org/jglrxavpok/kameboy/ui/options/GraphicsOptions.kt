package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.processing.video.ColorPalette
import org.jglrxavpok.kameboy.processing.video.Palettes
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

object GraphicsOptions : JPanel() {

    val paletteSelection = JComboBox<ColorPalette>(Palettes)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        sub {
            layout = FlowLayout()
            add(JLabel("DMG Palette: "))
            add(paletteSelection)
        }
    }
}
