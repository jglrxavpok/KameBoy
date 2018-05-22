package org.jglrxavpok.kameboy.network.guest.packets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.network.*

class GuestInfos(var gameTitle: String): AbstractPacket() {

    internal constructor(): this("")

    override fun decodeFrom(buffer: ByteBuf) {
        gameTitle = buffer.readString()
    }

    override fun encodeInto(buffer: ByteBuf) {
        buffer.writeString(gameTitle)
    }

    object Handler: PacketHandler<GuestInfos> {
        override fun handlePacket(packet: GuestInfos, ctx: ChannelHandlerContext, netHandler: INetworkHandler) {
            with(packet) {
                println("(GUEST) Hey! I am playing $gameTitle!")
            }
        }
    }
}