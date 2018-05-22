package org.jglrxavpok.kameboy.ui.options

import org.jglrxavpok.kameboy.network.guest.GuestSession
import org.jglrxavpok.kameboy.network.host.Server
import java.awt.FlowLayout
import javax.swing.*
import kotlin.concurrent.thread

object MultiplayerOptions : JPanel() {

    private val statusLabel = JLabel("Shutdown")
    private val guestStatusLabel = JLabel("Shutdown")
    private val hostAddress = JTextField("localhost:25565")

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val hostPanel = sub("Host") {

        }
        val connectPanel = sub("Join") {

        }
        val watchPanel = sub("Watch") {

        }
        resetHostPanel(hostPanel, connectPanel, watchPanel)
        resetConnectPanel(hostPanel, connectPanel, watchPanel)
        resetWatchPanel(hostPanel, connectPanel, watchPanel)
    }

    private fun resetWatchPanel(hostPanel: JPanel, connectPanel: JPanel, watchPanel: JPanel) {
        // TODO
    }

    private fun resetConnectPanel(hostPanel: JPanel, connectPanel: JPanel, watchPanel: JPanel) {
        connectPanel.removeAll()
        hostPanel.setDeepEnabled(true)
        watchPanel.setDeepEnabled(true)
        guestStatusLabel.text = "Shutdown"

        connectPanel.layout = FlowLayout()
        hostAddress.isEnabled = true
        connectPanel.sub("Status") {
            add(guestStatusLabel)
        }
        val hostAddressPanel = connectPanel.sub("Host address") {
            add(hostAddress)
        }
        connectPanel.sub("Control") {
            val pane = this
            layout = FlowLayout()
            val connectButton = JButton("Connect")
            val disconnectButton = JButton("Disconnect")
            connectButton.addActionListener {
                pane.remove(connectButton)
                hostAddress.setDeepEnabled(false)
                hostPanel.setDeepEnabled(false)
                watchPanel.setDeepEnabled(false)

                connectPanel.remove(hostAddressPanel)
                val addressText = hostAddress.text
                val addressParts = addressText.split(":")
                val ip = addressParts[0]
                val port = addressParts[1].toInt()
                pane.add(disconnectButton)
                thread(name = "Netty Guest Thread") {
                    GuestSession.connect(ip, port) { state ->
                        when(state) {
                            Server.ConnectionStatus.Shutdown -> resetConnectPanel(hostPanel, connectPanel, watchPanel)
                            Server.ConnectionStatus.NoConnection -> {
                                resetConnectPanel(hostPanel, connectPanel, watchPanel)
                                guestStatusLabel.text = "Could not connect"
                            }
                            Server.ConnectionStatus.Running -> guestStatusLabel.text = "Connected"
                            Server.ConnectionStatus.Booting -> guestStatusLabel.text = "Connecting..."
                        }
                        MultiplayerOptions.repaint()
                    }
                }
            }
            disconnectButton.addActionListener {
                GuestSession.disconnect()
            }
            add(connectButton)
        }
    }

    private fun resetHostPanel(hostPanel: JPanel, connectPanel: JPanel, watchPanel: JPanel) {
        hostPanel.removeAll()
        connectPanel.setDeepEnabled(true)
        watchPanel.setDeepEnabled(true)

        hostPanel.layout = FlowLayout()
        val portSelection = JSpinner()
        portSelection.value = 25565
        statusLabel.text = "Shutdown"
        hostPanel.sub("State") {
            add(statusLabel)
        }
        hostPanel.sub("Port") {
            add(portSelection)
        }
        val launchServer = JButton("Launch server")
        hostPanel.add(launchServer)

        launchServer.addActionListener {
            connectPanel.setDeepEnabled(false)
            watchPanel.setDeepEnabled(false)
            hostPanel.removeAll()

            hostPanel.sub("State") {
                add(statusLabel)
            }
            hostPanel.sub("Control") {
                layout = FlowLayout()
                val kickButton = JButton("Kick guest")
                val shutdownButton = JButton("Shutdown server")

                shutdownButton.addActionListener {
                    Server.stop()
                }

                add(kickButton)
                add(shutdownButton)
            }
            thread(name = "Netty Server Thread") {
                Server.start(portSelection.value as Int) { state: Server.ConnectionStatus ->
                    when(state) {
                        Server.ConnectionStatus.Shutdown -> resetHostPanel(hostPanel, connectPanel, watchPanel)
                        Server.ConnectionStatus.Running -> statusLabel.text = "Running"
                        Server.ConnectionStatus.Booting -> statusLabel.text = "Booting"
                        else -> {} // NOP
                    }
                }
            }
        }
    }
}
