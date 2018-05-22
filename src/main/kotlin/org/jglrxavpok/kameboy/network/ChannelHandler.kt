package org.jglrxavpok.kameboy.network

import io.netty.channel.*

class ChannelHandler(protected val netHandler: INetworkHandler): ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            val m = msg as NettyPacket
            val packet = PacketRegistry.create(m.side, m.id)
            packet.decodeFrom(m.payload)
            val handler = PacketRegistry.handlers[packet.javaClass]!!
            handler.handlePacket(packet, ctx, netHandler)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        netHandler.onConnexionEstablished(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        netHandler.exception(cause)
        cause.printStackTrace()
        ctx.close()
    }
}


interface INetworkHandler {

    val side: NetworkSide

    fun handlePacket(ctx: ChannelHandlerContext, packet: AbstractPacket)

    fun onConnexionEstablished(ctx: ChannelHandlerContext)

    fun exception(cause: Throwable)
}
