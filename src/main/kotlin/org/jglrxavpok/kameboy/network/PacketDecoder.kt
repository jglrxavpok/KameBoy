package org.jglrxavpok.kameboy.network

import io.netty.buffer.*
import io.netty.channel.*
import io.netty.handler.codec.*

class PacketDecoder : ByteToMessageDecoder() {

    private val buffer: ByteBuf
    private var waitingPayloadSize: Int = 0
    private var packet: NettyPacket? = null

    init {
        waitingPayloadSize = -1
        buffer = Unpooled.buffer()
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, data: ByteBuf, out: MutableList<Any>) {
        buffer.writeBytes(data)
        if (waitingPayloadSize >= 0) {
            if (buffer.readableBytes() < waitingPayloadSize)
                return
            val payload = buffer.readBytes(Math.min(buffer.readableBytes(), waitingPayloadSize))
            val payloadData = payload.readBytes(waitingPayloadSize)
            packet!!.payload = payloadData
            out.add(packet!!)
            println("read packet $packet")
            packet = null
            waitingPayloadSize = -1
            decode(ctx, payload, out)
        } else {
            if (buffer.readableBytes() >= 12) {
                packet = NettyPacket()
                packet!!.id = buffer.readInt()
                packet!!.side = NetworkSide.values()[buffer.readInt()]
                val payloadSize = buffer.readInt()
                if (buffer.readableBytes() < payloadSize) {
                    waitingPayloadSize = payloadSize
                    println("waiting for $waitingPayloadSize bytes") // TODO: remove
                } else {
                    val payload = buffer.readBytes(payloadSize)
                    packet!!.payload = payload
                    out.add(packet!!)
                    waitingPayloadSize = -1
                }
            }
        }
    }
}
