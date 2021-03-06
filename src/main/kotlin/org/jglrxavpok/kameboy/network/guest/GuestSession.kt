package org.jglrxavpok.kameboy.network.guest

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import org.jglrxavpok.kameboy.EmulatorCore
import org.jglrxavpok.kameboy.memory.SerialPeripheral
import org.jglrxavpok.kameboy.network.*
import org.jglrxavpok.kameboy.network.guest.packets.GuestInfos
import org.jglrxavpok.kameboy.network.host.Server
import org.jglrxavpok.kameboy.network.packets.SerialConfirmation
import org.jglrxavpok.kameboy.network.packets.SerialPacket
import java.net.ConnectException


object GuestSession: INetworkHandler, SerialPeripheral {
    override val side = NetworkSide.Guest
    override lateinit var core: EmulatorCore
    private var channel: Channel? = null

    private var listener: (Server.ConnectionStatus) -> Unit = {}
    private var state: Server.ConnectionStatus = Server.ConnectionStatus.Shutdown
        set(value) {
            field = value
            listener(state)
        }
    fun connect(host: String, port: Int, listener: (Server.ConnectionStatus) -> Unit) {
        val workerGroup = NioEventLoopGroup()
        this.listener = listener
        try {
            state = Server.ConnectionStatus.Booting
            val b = Bootstrap() // (1)
            b.group(workerGroup) // (2)
            b.channel(NioSocketChannel::class.java) // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true) // (4)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                public override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(PacketDecoder()).addLast(PacketEncoder()).addLast(ChannelHandler(this@GuestSession))
                }
            })

            // Start the client.
            val f = b.connect(host, port) // (5)
            f.addListener { future ->
                if(!future.isSuccess) {
                    state = Server.ConnectionStatus.NoConnection
                }
            }
            f.sync()

            // Wait until the connection is closed.
            f.channel().closeFuture().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            workerGroup.shutdownGracefully()
            if(state != Server.ConnectionStatus.NoConnection)
                state = Server.ConnectionStatus.Shutdown
        }
    }

    fun disconnect() {
        channel?.close()
        channel = null
    }

    override fun handlePacket(ctx: ChannelHandlerContext, packet: AbstractPacket) { }

    override fun onConnexionEstablished(ctx: ChannelHandlerContext) {
        channel = ctx.channel()
        state = Server.ConnectionStatus.Running
        ctx.writeAndFlushPacket(GuestInfos(core.cartridge.title))
        val serial = core.gameboy.mapper.serialIO
        if(this !in serial.connectedPeripherals)
            serial.connectedPeripherals.add(this)
    }

    override fun exception(cause: Throwable) {
        if(cause is ConnectException) {
            state = Server.ConnectionStatus.NoConnection
        }
    }

    override fun transfer(byte: Int) {
        channel?.writeAndFlushPacket(SerialPacket(byte))
    }

    fun isRunning(): Boolean {
        return state == Server.ConnectionStatus.Running
    }

    fun confirmTransfer() {
        channel?.writeAndFlushPacket(SerialConfirmation())
    }
}
