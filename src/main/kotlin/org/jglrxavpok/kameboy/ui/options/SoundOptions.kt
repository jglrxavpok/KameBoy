package org.jglrxavpok.kameboy.ui.options

import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider

object SoundOptions : JPanel() {

    val volumeSlider = JSlider()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        volumeSlider.paintLabels = true
        volumeSlider.paintTicks = true
        volumeSlider.paintTrack = true
        sub {
            layout = FlowLayout()
            add(JLabel("Volume"))
            add(volumeSlider)
        }
    }
}
