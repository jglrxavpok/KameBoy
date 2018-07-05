package org.jglrxavpok.kameboy.ui.options

import jdk.nashorn.internal.scripts.JO
import org.jglrxavpok.kameboy.cheats.GamesharkCode
import org.jglrxavpok.kameboy.helpful.toHexStringWithPadding
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

object CheatingOptions : JPanel() {

    val gamesharkCodes = mutableListOf<GamesharkCode>()
    internal val codeTableModel = CheatsTableModel
    internal val codeTable = JTable(codeTableModel)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        sub("Game Shark (WIP)") {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val addButton = JButton("Add")
            codeTable.setDefaultRenderer(Any::class.java, GamesharkCodeRenderer)
            codeTable.getColumn("Active").cellRenderer = CheckBoxRenderer
            codeTable.getColumn("Active").cellEditor = CheckBoxEditor
            codeTable.getColumn("Remove").cellEditor = ButtonEditor
            codeTable.getColumn("Remove").cellRenderer = ButtonRenderer
            add(JScrollPane(codeTable))
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
                        codeTable.updateUI()
                    }
                }
            }
        }
    }
}

object CheckBoxEditor: DefaultCellEditor(JCheckBox())
object ButtonRenderer: JButton(), TableCellRenderer {
    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        text = "Remove"
        return this
    }
}
object ButtonEditor: DefaultCellEditor(JCheckBox()), ActionListener {

    private val button = JButton()
    private var column: Int = -1
    private var row: Int = -1
    init {
        button.isOpaque = true
        button.addActionListener(this)
    }

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        this.row = row
        this.column = column
        return button
    }

    override fun actionPerformed(e: ActionEvent) {
        CheatingOptions.gamesharkCodes.removeAt(row)
        CheatsTableModel.fireTableRowsDeleted(row, row)
    }
}

object CheckBoxRenderer: JCheckBox(), TableCellRenderer {
    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        this.isSelected = value as Boolean
        this.isOpaque = true
        val code = CheatingOptions.gamesharkCodes[row]
        if(!code.isValid)
            background = Color(1f, 0f, 0f)
        else if(code.isActive)
            background = Color(0f, 1f, 0f)
        return this
    }

}

object GamesharkCodeRenderer: DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val comp = when {
            value is JComponent -> value
            else -> JLabel(value.toString())
        }
        comp.isOpaque = true
        val code = CheatingOptions.gamesharkCodes[row]
        if(!code.isValid)
            comp.background = Color(1f, 0f, 0f)
        else if(code.isActive)
            comp.background = Color(0f, 1f, 0f)
        return comp
    }
}

object CheatsTableModel: DefaultTableModel() {
    val codes = CheatingOptions.gamesharkCodes

    override fun getRowCount(): Int {
        return CheatingOptions.gamesharkCodes.size
    }

    override fun getColumnCount(): Int {
        return 5
    }

    private fun code(index: Int) = CheatingOptions.gamesharkCodes[index]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val code = code(rowIndex)
        val removeButton = JButton("Remove")
        return when(columnIndex) {
            0 -> code.code
            1 -> code.memoryAddress.toHexStringWithPadding(charCount = 4)
            2 -> code.newData.toHexStringWithPadding(charCount = 2)
            3 -> code.isActive
            4 -> removeButton
            else -> "<Unknown>"
        }
    }

    override fun setValueAt(aValue: Any?, row: Int, column: Int) {
        val code = code(row)
        when(column) {
            0 -> {
                codes[row] = GamesharkCode(aValue.toString())
                fireTableRowsUpdated(row, row)
            }
            1 -> {
                try {
                    val address = aValue.toString().take(4).toInt(16)
                    codes[row] = code.withAddress(address)
                    fireTableRowsUpdated(row, row)
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(CheatingOptions, "Invalid hexadecimal value for address: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            2 -> {
                try {
                    val newData = aValue.toString().take(2).toInt(16)
                    codes[row] = code.withData(newData)
                    fireTableRowsUpdated(row, row)
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(CheatingOptions, "Invalid hexadecimal value for data: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            3 -> {
                if(aValue is Boolean) {
                    codes[row].isActive = aValue
                    fireTableRowsUpdated(row, row)
                }
            }
        }
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return when(column) {
            4 -> true
            else -> true
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        if(columnIndex == 3)
            return java.lang.Boolean.TYPE
        return super.getColumnClass(columnIndex)
    }

    override fun getColumnName(column: Int): String {
        return when(column) {
            0 -> "Code"
            1 -> "Memory address"
            2 -> "Data"
            3 -> "Active"
            4 -> "Remove"
            else -> "Unknown"
        }
    }
}