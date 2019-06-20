package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.ui.Audio
import org.jglrxavpok.kameboy.ui.Config
import org.jglrxavpok.kameboy.ui.Rendering
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack
import java.awt.FlowLayout
import javax.swing.*

object SoundOptions : NuklearTab() {
    override val title = "Sound"

    val volumeSlider = JSlider()
    val skipAudioButton = JRadioButton("Skip some audio cycles")
    val dontSkipAudioButton = JRadioButton("Don't skip any audio cycles")
    val skipAudioButtons = listOf(skipAudioButton, dontSkipAudioButton)
    val skipAudioButtonGroup = ButtonGroup()
    val skipAudioCycles: Boolean
        get() = skipAudioButtonGroup.isSelected(skipAudioButton.model)
/*
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

    }
*/
    override fun renderTab(context: NkContext, stack: MemoryStack) {
        if(nk_group_begin(context, "Volume", NK_WINDOW_BORDER or NK_WINDOW_TITLE)) {
            nk_layout_row_dynamic(context, 30f, 1)
            val volumeValue = Config[Audio.volume]
            val newVolumeValue = nk_slide_int(context, 0, volumeValue, 100, 1)
            if(volumeValue != newVolumeValue) {
                Config[Audio.volume] = newVolumeValue
                Config.save()
            }
            nk_group_end(context)
        }

        if(nk_group_begin(context, "Sound fidelity", NK_WINDOW_BORDER or NK_WINDOW_TITLE)) {
            nk_layout_row_dynamic(context, 30f, 1)
            val active = intArrayOf(if(Config[Audio.skipAudioCycles]) 1 else 0)
            if(nk_radio_label(context, "Skip some audio cycles", active)) {
                Config[Audio.skipAudioCycles] = true
            }
            active[0] = 1-active[0]

            nk_layout_row_dynamic(context, 30f, 1)
            if(nk_radio_label(context, "Don't skip any audio cycles", active)) {
                Config[Audio.skipAudioCycles] = false
            }
            // TODO
            nk_group_end(context)
        }
    }
}
