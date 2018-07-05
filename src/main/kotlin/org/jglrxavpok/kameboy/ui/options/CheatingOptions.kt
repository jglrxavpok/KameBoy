package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.cheats.GamesharkCode
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*

object CheatingOptions : JPanel() {

    val gamesharkCodes = mutableListOf<GamesharkCode>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        sub("Game Shark (WIP)") {
            layout = FlowLayout()
            val addButton = JButton("Add")
            val codeList = JList<GamesharkCode>(object : AbstractListModel<GamesharkCode>() {
                override fun getElementAt(index: Int)= gamesharkCodes[index]
                override fun getSize() = gamesharkCodes.size
            })
            codeList.preferredSize.setSize(100,200)
            codeList.cellRenderer = GamesharkCodeRenderer
            add(codeList)
            add(addButton)

            addButton.addActionListener {
                val code = JOptionPane.showInputDialog(this, "Please enter your code")
                if(code != null) {
                    val gamesharkCode = GamesharkCode(code)
                    if(!gamesharkCode.isValid)
                        JOptionPane.showMessageDialog(this, "Invalid GameShark code!", "Error", JOptionPane.ERROR_MESSAGE)
                    else {
                        gamesharkCodes.add(gamesharkCode)
                        CheatingOptions.repaint()
                        codeList.repaint()
                        codeList.updateUI()
                    }
                }
            }
        }
    }
}

object GamesharkCodeRenderer: ListCellRenderer<GamesharkCode> {
    override fun getListCellRendererComponent(list: JList<out GamesharkCode>, code: GamesharkCode, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        val comp = JPanel()
        val layout = GridLayout(1,3)
        comp.layout = layout

        comp.add(JLabel(code.code))
        comp.add(JLabel(Integer.toHexString(code.memoryAddress)))
        comp.add(JLabel(Integer.toHexString(code.newData)))

        return comp
    }

}
