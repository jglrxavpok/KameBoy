package org.jglrxavpok.kameboy.ui.options

import javax.swing.JComponent
import javax.swing.JPanel

fun JComponent.sub(initBlock: JPanel.() -> Unit): Unit {
    val panel = JPanel()
    panel.initBlock()
    this.add(panel)
}