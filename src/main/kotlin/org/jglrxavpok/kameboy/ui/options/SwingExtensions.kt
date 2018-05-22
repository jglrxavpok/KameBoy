package org.jglrxavpok.kameboy.ui.options

import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.TitledBorder

fun JComponent.sub(subPanelName: String, initBlock: JPanel.() -> Unit): JPanel {
    val panel = JPanel()
    panel.border = TitledBorder(subPanelName)
    panel.initBlock()
    this.add(panel)
    return panel
}

fun JComponent.setDeepEnabled(enabled: Boolean) {
    isEnabled = enabled
    components.forEach {
        (it as? JComponent)?.setDeepEnabled(enabled)
    }
}