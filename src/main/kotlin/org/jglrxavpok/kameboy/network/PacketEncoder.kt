package org.jglrxavpok.kameboy.network

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * From https://github.com/OurCraft/OurCraft/blob/master/src/main/java/org/craft/network/PacketEncoder.java
 */
class PacketEncoder : MessageToByteEncoder<NettyPacket>() {

    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, packet: NettyPacket, out: ByteBuf) {
        out.writeInt(packet.id)
        out.writeInt(packet.side.ordinal)
        out.writeInt(packet.payload.writerIndex())
        out.writeBytes(packet.payload)
    }
}