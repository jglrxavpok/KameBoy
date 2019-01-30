package org.jglrxavpok.kameboy.network.packets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.helpful.toBit
import org.jglrxavpok.kameboy.network.AbstractPacket
import org.jglrxavpok.kameboy.network.INetworkHandler
import org.jglrxavpok.kameboy.network.PacketHandler

class SerialPacket(var fromMaster: Boolean, var bit: Boolean): AbstractPacket() {

    internal constructor(): this(false, false)

    override fun decodeFrom(buffer: ByteBuf) {
        bit = buffer.readBoolean()
        fromMaster = buffer.readBoolean()
    }

    override fun encodeInto(buffer: ByteBuf) {
        buffer.writeBoolean(bit)
        buffer.writeBoolean(fromMaster)
    }

    object Handler: PacketHandler<SerialPacket> {
        override fun handlePacket(packet: SerialPacket, ctx: ChannelHandlerContext, netHandler: INetworkHandler) {
            val core = netHandler.core
            val serialIO = core.gameboy.mapper.serialIO
            val type = if(serialIO.hasInternalClock) "Master" else "Slave"
            core.later {
                println(">> ($type) Received ${packet.bit.toBit()}")
                serialIO.receive(packet.bit, packet.fromMaster)
            }
        }
    }
}