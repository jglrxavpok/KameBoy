package org.jglrxavpok.kameboy.network

import io.netty.channel.ChannelHandlerContext
import org.jglrxavpok.kameboy.network.guest.packets.GuestInfos
import org.jglrxavpok.kameboy.network.packets.SerialConfirmation
import org.jglrxavpok.kameboy.network.packets.SerialFinish
import org.jglrxavpok.kameboy.network.packets.SerialPacket
import sun.rmi.runtime.Log
import java.util.HashMap


object PacketRegistry {

    var packets: HashMap<NetworkSide, HashMap<Int, Class<out AbstractPacket>>> = HashMap()

    val sides: HashMap<Class<out AbstractPacket>, NetworkSide> = HashMap()
    val ids: HashMap<Class<out AbstractPacket>, Int> = HashMap()
    val handlers: HashMap<Class<out AbstractPacket>, PacketHandler<in AbstractPacket>> = HashMap()

    init {
        packets[NetworkSide.Guest] = HashMap()
        packets[NetworkSide.Host] = HashMap()
        packets[NetworkSide.Common] = HashMap()

        registerPacket(NetworkSide.Guest, 0x0, GuestInfos.Handler)
        registerPacket(NetworkSide.Common, 0x1, SerialPacket.Handler)
        registerPacket(NetworkSide.Guest, 0x2, SerialConfirmation.Handler)
        registerPacket(NetworkSide.Host, 0x3, SerialFinish.Handler)
    }

    fun getPacketId(packet: Class<out AbstractPacket>): Int {
        return ids[packet]!!
    }

    fun getPacketSide(packet: Class<out AbstractPacket>): NetworkSide {
        return sides[packet]!!
    }

    inline fun <reified PacketType: AbstractPacket> registerPacket(senderSide: NetworkSide, id: Int, handler: PacketHandler<PacketType>) {
        val packetClass = PacketType::class.java
        packets[senderSide]!![id] = packetClass
        handlers[packetClass] = object: PacketHandler<AbstractPacket> {
            override fun handlePacket(packet: AbstractPacket, ctx: ChannelHandlerContext, netHandler: INetworkHandler) {
                handler.handlePacket(packet as PacketType, ctx, netHandler)
            }
        }

        sides[packetClass] = senderSide
        ids[packetClass] = id
    }

    fun create(side: NetworkSide, id: Int): AbstractPacket {
        val packetClass = packets[side]!![id] ?: error("Unknown packet id: $id")
        try {
            return packetClass.newInstance()
        } catch (e: Exception) {
            error(e)
        }
    }
}