package org.jglrxavpok.kameboy.network.packets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.network.AbstractPacket
import org.jglrxavpok.kameboy.network.INetworkHandler
import org.jglrxavpok.kameboy.network.PacketHandler

class SerialFinish(): AbstractPacket() {
    override fun decodeFrom(buffer: ByteBuf) {

    }

    override fun encodeInto(buffer: ByteBuf) {

    }

    object Handler: PacketHandler<SerialFinish> {
        override fun handlePacket(packet: SerialFinish, ctx: ChannelHandlerContext, netHandler: INetworkHandler) {
            val core = netHandler.core
            val serialIO = core.gameboy.mapper.serialIO
            core.later {
                serialIO.transferFinish()
            }
        }
    }

}