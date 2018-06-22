package org.jglrxavpok.kameboy.ui.options

import javafx.application.Application as FxApplication
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.WindowConstants


object OptionsWindow: JFrame("Kameboy Options") {

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        val tabs = JTabbedPane()
        tabs.addTab("Graphics", GraphicsOptions)
        tabs.addTab("Input", InputOptions)
        tabs.addTab("Multiplayer", MultiplayerOptions)
        tabs.addTab("Sound", SoundOptions)
        add(tabs)
        pack()
    }
}