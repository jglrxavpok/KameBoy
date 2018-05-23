package org.jglrxavpok.kameboy.network

import io.netty.util.concurrent.GenericFutureListener
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext


fun ChannelHandlerContext.writeAndFlushPacket(packet: AbstractPacket) {
    this.channel().writeAndFlushPacket(packet)
}

fun Channel.writeAndFlushPacket(packet: AbstractPacket) {
    val channel = this
    val id = PacketRegistry.getPacketId(packet.javaClass)
    val side = PacketRegistry.getPacketSide(packet.javaClass)
    val buffer = channel.alloc().buffer()
    packet.encodeInto(buffer)
    val nettyPacket = NettyPacket(id, buffer, side)
    channel.writeAndFlush(nettyPacket).addListener { future ->
        if (!future.isSuccess) {
            future.cause().printStackTrace()
        } else {
            println("success! $packet")
        }
    }
}

fun ByteBuf.writeString(string: String) {
    val codePoints = string.codePoints()
    val count = codePoints.count()
    writeLong(count)
    string.codePoints().forEach { writeInt(it) }
}

fun ByteBuf.readString(): String {
    val length = readLong()
    return buildString {
        for(i in 0 until length) {
            appendCodePoint(readInt())
        }
    }
}