package org.jglrxavpok.kameboy.network

import io.netty.buffer.*
import io.netty.channel.ChannelHandlerContext

class NettyPacket {

    lateinit var payload: ByteBuf
        internal set
    var id: Int = 0
        internal set
    lateinit var side: NetworkSide
        internal set

    internal constructor() {

    }

    constructor(id: Int, payload: ByteBuf, side: NetworkSide) {
        this.side = side
        this.id = id
        this.payload = payload
    }
}

abstract class AbstractPacket {

    abstract fun decodeFrom(buffer: ByteBuf)

    abstract fun encodeInto(buffer: ByteBuf)

}

@FunctionalInterface
interface PacketHandler<in Packet: AbstractPacket> {
    fun handlePacket(packet: Packet, ctx: ChannelHandlerContext, netHandler: INetworkHandler)
}
