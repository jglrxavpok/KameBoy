package org.jglrxavpok.kameboy.network.host

import io.netty.channel.ChannelOption
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import org.jglrxavpok.kameboy.network.ChannelHandler
import org.jglrxavpok.kameboy.network.PacketDecoder
import org.jglrxavpok.kameboy.network.PacketEncoder


object Server {

    private var listener: (ConnectionStatus) -> Unit = {}
    private var serverChannel: Channel? = null
    var state = ConnectionStatus.Shutdown
        set(value) {
            field = value
            listener(value)
        }

    fun start(port: Int, stateLister: (ConnectionStatus) -> Unit) {
        val bossGroup = NioEventLoopGroup() // (1)
        val workerGroup = NioEventLoopGroup()
        listener = stateLister
        try {
            state = ConnectionStatus.Booting
            val b = ServerBootstrap() // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java) // (3)
                    .childHandler(object : ChannelInitializer<SocketChannel>() { // (4)
                        @Throws(Exception::class)
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(PacketDecoder()).addLast(PacketEncoder()).addLast(ChannelHandler(ServerNetHandler()))
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)

            // Bind and start to accept incoming connections.
            val f = b.bind(port).addListener { future ->
                if(future.isSuccess) {
                    state = ConnectionStatus.Running
                }
            }.sync() // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            serverChannel = f.channel()
            serverChannel!!.closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
            state = ConnectionStatus.Shutdown
        }
    }

    fun stop() {
        serverChannel?.close()
        serverChannel = null
    }

    enum class ConnectionStatus {
        Shutdown, Running, Booting, NoConnection
    }
}