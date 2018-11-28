package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.KameboyCore
import java.awt.Component
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

object EmulatorControlWindow: JFrame("Control") {

    val resetGame = JButton("Hard reset")
    val insert = JButton("Insert cartridge")
    val ejectCartridge = JButton("Eject cartridge")

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)

        with(content) {
            insert.alignmentX = Component.CENTER_ALIGNMENT
            ejectCartridge.alignmentX = Component.CENTER_ALIGNMENT
            resetGame.alignmentX = Component.CENTER_ALIGNMENT

            add(insert)

            ejectCartridge.isEnabled = false
            add(ejectCartridge)

            resetGame.isEnabled = false
            add(resetGame)

            insert.addActionListener {
                val filechooser = JFileChooser(File(Config[System.lastRomFolder]))
                if(filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    println("hello1")
                    val file = filechooser.selectedFile
                    println("hello2")
                    Config[System.lastRomFolder] = file.parent
                    println("hello3")
                    Config.save()
                    println("hello4")
                    KameboyCore.CoreInstance.loadROM(file)
                }
            }

            ejectCartridge.addActionListener {
                KameboyCore.CoreInstance.ejectCartridge()
            }

            resetGame.addActionListener {
                KameboyCore.CoreInstance.hardReset()
            }
        }

        contentPane.add(content)
        pack()
    }
}