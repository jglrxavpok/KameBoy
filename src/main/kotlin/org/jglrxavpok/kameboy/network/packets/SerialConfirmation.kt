package org.jglrxavpok.kameboy.network.packets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.KameboyCore.Companion.CoreInstance
import org.jglrxavpok.kameboy.network.AbstractPacket
import org.jglrxavpok.kameboy.network.INetworkHandler
import org.jglrxavpok.kameboy.network.PacketHandler

class SerialConfirmation: AbstractPacket() {
    override fun decodeFrom(buffer: ByteBuf) { }

    override fun encodeInto(buffer: ByteBuf) { }

    object Handler: PacketHandler<SerialConfirmation> {
        override fun handlePacket(packet: SerialConfirmation, ctx: ChannelHandlerContext, netHandler: INetworkHandler) {
            val core = CoreInstance.core
            val serial = core.gameboy.mapper.serialIO
            serial.confirmTransfer()
        }

    }

}
