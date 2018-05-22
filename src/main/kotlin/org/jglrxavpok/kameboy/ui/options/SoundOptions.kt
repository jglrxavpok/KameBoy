package org.jglrxavpok.kameboy.ui.options

import java.awt.FlowLayout
import javax.swing.*

object SoundOptions : JPanel() {

    val volumeSlider = JSlider()
    val skipAudioButton = JRadioButton("Skip some audio cycles")
    val dontSkipAudioButton = JRadioButton("Don't skip any audio cycles")
    val skipAudioButtons = listOf(skipAudioButton, dontSkipAudioButton)
    val skipAudioButtonGroup = ButtonGroup()
    val skipAudioCycles: Boolean
        get() = skipAudioButtonGroup.isSelected(skipAudioButton.model)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        volumeSlider.paintLabels = true
        volumeSlider.paintTicks = true
        volumeSlider.paintTrack = true

        skipAudioButtonGroup.add(skipAudioButton)
        skipAudioButtonGroup.add(dontSkipAudioButton)
        skipAudioButtonGroup.setSelected(skipAudioButton.model, true) // TODO: save in config file

        skipAudioButton.toolTipText = "Better performance (can drastically improve FPS) but worse sound fidelity"
        dontSkipAudioButton.toolTipText = "Heavily impacts performance but provides a better sound fidelity"

        sub {
            layout = FlowLayout()
            add(JLabel("Volume"))
            add(volumeSlider)
        }

        sub {
            layout = FlowLayout()
            add(JLabel("Sound fidelity"))
            for(button in skipAudioButtons)
                add(button)
        }
    }
}
