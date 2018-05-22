package org.jglrxavpok.kameboy.network

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

/**
 * From https://github.com/OurCraft/OurCraft/blob/master/src/main/java/org/craft/network/PacketEncoder.java
 */
class PacketEncoder : MessageToByteEncoder<NettyPacket>() {

    @Throws(Exception::class)
    override fun encode(arg0: ChannelHandlerContext, arg1: NettyPacket, arg2: ByteBuf) {
        arg2.writeInt(arg1.id)
        arg2.writeInt(arg1.side.ordinal)
        arg2.writeInt(arg1.payload.writerIndex())
        arg2.writeBytes(arg1.payload)
    }
}