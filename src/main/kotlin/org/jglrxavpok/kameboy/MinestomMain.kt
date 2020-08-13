package org.jglrxavpok.kameboy

import fr.themode.demo.MainDemo
import net.minestom.server.entity.Player
import net.minestom.server.entity.type.decoration.EntityItemFrame
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.*
import net.minestom.server.instance.batch.ChunkBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.MapMeta
import net.minestom.server.map.framebuffers.LargeGLFWFramebuffer
import net.minestom.server.map.framebuffers.MapColorRenderer
import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.packet.server.play.MapDataPacket
import net.minestom.server.utils.Position
import net.minestom.server.utils.time.TimeUnit
import java.util.*
import kotlin.concurrent.thread
import net.minestom.server.MinecraftServer as MS

fun main(args: Array<String>) {
    val server = MS.init()

    val instanceManager: InstanceManager = MS.getInstanceManager()
    // Create the instance
    val instanceContainer = instanceManager.createInstanceContainer()
    // Set the ChunkGenerator
    instanceContainer.chunkGenerator = GeneratorDemo()
    // Enable the auto chunk loading (when players come close)
    instanceContainer.enableAutoChunkLoad(true)

    // Add event listeners
    val connectionManager: ConnectionManager = MS.getConnectionManager()
    connectionManager.addPlayerInitialization { player: Player ->
        // Set the spawning instance
        player.addEventCallback(PlayerLoginEvent::class.java) { event: PlayerLoginEvent -> event.spawningInstance = instanceContainer }

        // Teleport the player at spawn
        player.addEventCallback(PlayerSpawnEvent::class.java) { event: PlayerSpawnEvent? -> player.teleport(Position(0f, 45f, 0f)) }
    }

    for (x in -1..1) {
        for (z in -1..1) {
            instanceContainer.loadChunk(x, z)
        }
    }
    // 256x256 framebuffer
    for(x in 0..1) {
        for(z in 0..1) {
            val frame = EntityItemFrame(Position(-x.toFloat(), 43f-z, 1f), EntityItemFrame.ItemFrameOrientation.NORTH)
            frame.itemStack = ItemStack(Material.FILLED_MAP, 1).apply { itemMeta = MapMeta(z*2+x) }
            frame.position.yaw = 180f
            frame.instance = instanceContainer
        }
    }

    val framebuffer = LargeGLFWFramebuffer(256, 256)
    framebuffer.changeRenderingThreadToCurrent()
    val renderer = MapColorRenderer(framebuffer, MinestomRenderer::renderEmulatorContentsToTexture)
    framebuffer.unbindContextFromThread()
    framebuffer.setupRenderLoop(15, TimeUnit.MILLISECOND, renderer)

    val views = (0..3).map { i -> framebuffer.createSubView((i%2)*128,(i/2)*128) }
    MS.getSchedulerManager().buildTask {
        for (i in 0..3) {
            val packet = MapDataPacket()
            packet.mapId = i
            views[i].preparePacket(packet)
            MS.getConnectionManager().onlinePlayers.forEach {
                it.playerConnection.sendPacket(packet)
            }
        }
    }.repeat(15, TimeUnit.MILLISECOND).schedule()

    server.start("0.0.0.0", 25565) { playerConnection, responseData ->
        responseData.setDescription("Play gameboy games in Minecraft!")
        responseData.setName("Kameboy Server")
    }

    thread {
        KameboyCore(args)
    }
}

private class GeneratorDemo : ChunkGenerator() {
    override fun generateChunkData(batch: ChunkBatch, chunkX: Int, chunkZ: Int) {
        // Set chunk blocks
        for (x in 0 until Chunk.CHUNK_SIZE_X) {
            for (z in 0 until Chunk.CHUNK_SIZE_Z) {
                for (y in 0..39) {
                    batch.setBlock(x, y, z, Block.STONE)
                }
            }
        }
    }

    override fun fillBiomes(biomes: Array<Biome>, chunkX: Int, chunkZ: Int) {
        Arrays.fill(biomes, Biome.PLAINS)
    }

    override fun getPopulators(): List<ChunkPopulator> {
        return emptyList()
    }
}