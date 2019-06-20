package org.jglrxavpok.kameboy.ui

import org.jglrxavpok.kameboy.KameboyCore
import org.lwjgl.nuklear.NkRect
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack
import java.io.File

object EmulatorControlWindow: NuklearWindow("Emulator Control") {

    var resetGameEnabled: Boolean = false
    var ejectCartridgeEnabled: Boolean = false
    override val defaultWidth = 300
    override val defaultHeight = 300

    override fun renderWindow(stack: MemoryStack) {
        // Add rows here
        val rowHeight = 50f
        val itemsPerRow = 1
        nk_layout_row_dynamic(context, rowHeight, itemsPerRow)

        if(nk_button_label(context, "Hard reset")) {
            KameboyCore.CoreInstance.hardReset()
        }

        if(nk_button_label(context, "Eject cartridge")) {
            KameboyCore.CoreInstance.ejectCartridge()
        }

        if(nk_button_label(context, "Insert cartridge")) {
            /*val filechooser = JFileChooser(File(Config[System.lastRomFolder]))
            if(filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val file = filechooser.selectedFile
                Config[System.lastRomFolder] = file.parent
                Config.save()
                KameboyCore.CoreInstance.loadROM(file)
            }*/
            val file = File("SOME FILE")
            Config[System.lastRomFolder] = file.parent
            Config.save()
            KameboyCore.CoreInstance.loadROM(file)
        }
    }
}