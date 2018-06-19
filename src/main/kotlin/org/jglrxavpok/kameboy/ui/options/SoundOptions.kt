package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.ui.Audio
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering
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
        volumeSlider.value = Config[Audio.volume]
        volumeSlider.paintLabels = true
        volumeSlider.paintTicks = true
        volumeSlider.paintTrack = true
        volumeSlider.addChangeListener {
            Config[Audio.volume] = volumeSlider.value
            Config.save()
        }

        skipAudioButtonGroup.add(skipAudioButton)
        skipAudioButtonGroup.add(dontSkipAudioButton)
        if(Config[Audio.skipAudioCycles])
            skipAudioButtonGroup.setSelected(skipAudioButton.model, true)
        else
            skipAudioButtonGroup.setSelected(dontSkipAudioButton.model, true)

        dontSkipAudioButton.addActionListener {
            Config[Audio.skipAudioCycles] = false
            Config.save()
        }

        skipAudioButton.addActionListener {
            Config[Audio.skipAudioCycles] = true
            Config.save()
        }

        skipAudioButton.toolTipText = "Better performance (can drastically improve FPS) but worse sound fidelity"
        dontSkipAudioButton.toolTipText = "Heavily impacts performance but provides a better sound fidelity"

        sub("Volume") {
            layout = FlowLayout()
            add(volumeSlider)
        }

        sub("Sound fidelity") {
            layout = FlowLayout()
            for(button in skipAudioButtons)
                add(button)
        }
    }
}
