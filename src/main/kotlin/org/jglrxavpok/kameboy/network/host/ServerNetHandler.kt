package org.jglrxavpok.kameboy.network.host

import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.network.AbstractPacket
import org.jglrxavpok.kameboy.network.INetworkHandler
import org.jglrxavpok.kameboy.network.NetworkSide


class ServerNetHandler(override val core: EmulatorCore) : INetworkHandler {
    override fun exception(cause: Throwable) { }

    override val side = NetworkSide.Host

    override fun handlePacket(ctx: ChannelHandlerContext, packet: AbstractPacket) {
    }

    override fun onConnexionEstablished(ctx: ChannelHandlerContext) {
        Server.clientChannel = ctx.channel()
    }

}