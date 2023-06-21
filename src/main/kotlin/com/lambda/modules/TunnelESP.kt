package com.lambda.modules

import com.lambda.Oasis
import kotlinx.coroutines.launch
import net.minecraft.block.BlockAir
import net.minecraft.util.math.BlockPos
import com.lambda.client.util.TickTimer
import com.lambda.client.module.Category
import net.minecraft.util.math.AxisAlignedBB
import it.unimi.dsi.fastutil.ints.IntArrayList
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.util.color.ColorHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.graphics.ESPRenderer
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import com.lambda.client.util.graphics.GeometryMasks
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.modules.movement.ElytraFlight


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object TunnelESP: PluginModule(
    name = "TunnelESP",
    description = "Highlights narrow tunnels in the world around you",
    category = Category.RENDER,
    pluginMain = Oasis
){
    private val mode by setting(
        "Render Style", value = RenderMode.TUNNEL, description = "Recommend Tunnel for NR, Floor for OW")
    private val fill by setting("Floor Fill", value = true, {mode == RenderMode.FLOOR})
    private val outline by setting("Floor Outline", value = true, {mode == RenderMode.FLOOR})
    private val tracer by setting("Floor Tracer", value = false, {mode == RenderMode.FLOOR})
    private val color by setting(
        "Floor Color", ColorHolder(41, 118, 210), false, {mode == RenderMode.FLOOR})
    private val aFill by setting(
        "Floor Fill Opacity", value = 69, range = 0..255, step = 1, {fill && mode == RenderMode.FLOOR})
    private val aOutline by setting(
        "Floor Outline Opacity", value = 133, range = 0..255, step = 1, {outline && mode == RenderMode.FLOOR})
    private val aTracer by setting(
        "Floor Tracer Opacity", value = 20, range = 0..255, step = 1, {tracer && mode == RenderMode.FLOOR})
    private val tunFill by setting("Tunnel Fill", value = true, {mode == RenderMode.TUNNEL})
    private val tunOutline by setting("Tunnel Outline", value = true, {mode == RenderMode.TUNNEL})
    private val tunTracer by setting("Tunnel Tracer", value = false, {mode == RenderMode.TUNNEL})
    private val tunColor by setting(
        "Tunnel Color", ColorHolder(41, 118, 210), false, {mode == RenderMode.TUNNEL})
    private val aTunFill by setting(
        "Tunnel Fill Opacity", value = 48, range = 0..255, step = 1, {tunFill && mode == RenderMode.TUNNEL})
    private val aTunOutline by setting(
        "Tunnel Outline Opacity", value = 24, range = 0..255, step = 1, {tunOutline && mode == RenderMode.TUNNEL})
    private val aTunTracer by setting(
        "Tunnel Tracer Opacity", value = 20, range = 0..255, step = 1, {tunTracer && mode == RenderMode.TUNNEL})
    private val chunkRange by setting(
        "Chunk Range", value = 5, range = 1..5, step = 1, description = "Decrease this (or allocate more RAM) if laggy"
    )

    private val timer = TickTimer()
    private var renderer: ESPRenderer? = null

    private enum class RenderMode {
        FLOOR, TUNNEL
    }


    private fun getRenderer(): ESPRenderer? {
        val renderer: ESPRenderer?
        try {
            renderer = ESPRenderer()
        } catch (e: ExceptionInInitializerError) {
            return null
        }
        return renderer
    }

    private fun SafeClientEvent.isNorthSouthTunnel(node: BlockPos, west: Boolean, northWest: Boolean): Boolean {
        return !west && !northWest && !air(node.east()) && !air(node.north().east()) && !air(node.up().west())
            && !air(node.up().east()) && !air(node.down().north()) && !air(node.up(2).north())
            && !air(node.up().north().west()) && !air(node.up().north().east())
    }

    private fun SafeClientEvent.isEastWestTunnel(node: BlockPos, north: Boolean, northWest: Boolean): Boolean {
        return !north && !northWest && !air(node.south()) && !air(node.south().west()) && !air(node.up().north())
            && !air(node.up().south()) && !air(node.down().west()) && !air(node.up(2).west())
            && !air(node.up().west().north()) && !air(node.up().west().south())
    }

    private fun SafeClientEvent.updateRenderer() {
        val renderColor: ColorHolder
        when (mode) {
            RenderMode.FLOOR -> {
                renderColor = color
                renderer?.aFilled = if (fill) aFill else 0
                renderer?.aOutline = if (outline) aOutline else 0
                renderer?.aTracer = if (tracer) aTracer else 0
            }
            RenderMode.TUNNEL -> {
                renderColor = tunColor
                renderer?.aFilled = if (tunFill) aTunFill else 0
                renderer?.aOutline = if (tunOutline) aTunOutline else 0
                renderer?.aTracer = if (tunTracer) aTunTracer else 0
            }
        }

        defaultScope.launch {
            val tunnels = LongArrayList()
            val neededQuads = IntArrayList()
            val cache = ObjectArrayList<Triple<AxisAlignedBB, ColorHolder, Int>>()
            val tPos = player.position
            val dim = player.dimension

            val range = chunkRange * 16
            val startX = tPos.x - range
            val endX = tPos.x + range
            val startZ = tPos.z - range
            val endZ = tPos.z + range
            val startY = 5
            val endY = if (dim == -1) {
                121
            } else if (ElytraFlight.isEnabled) {
                tPos.y
            } else {
                (tPos.y + range).coerceAtMost(world.height)
            }

            val node = BlockPos.MutableBlockPos(0,0,0)
            for (x in startX..endX) {
                for (y in startY..endY) {
                    for (z in startZ..endZ) {
                        node.setPos(x,y,z)
                        if (!air(node) || !air(node.up()) || air(node.up(2)) || air(node.down())) continue

                        val north = air(node.north())
                        val west = air(node.west())
                        val northWest = air(node.north().west())

                        if (north && air(node.up().north())) {
                            if (isNorthSouthTunnel(node, west, northWest)) {
                                tunnels.add(node.toLong())
                                if (air(node.south()) && air(node.up().south())) {
                                    neededQuads.add(
                                        GeometryMasks.Quad.DOWN or GeometryMasks.Quad.EAST or GeometryMasks.Quad.WEST
                                    )
                                    neededQuads.add(
                                        GeometryMasks.Quad.UP or GeometryMasks.Quad.EAST or GeometryMasks.Quad.WEST
                                    )
                                } else {
                                    neededQuads.add(
                                        GeometryMasks.Quad.DOWN or GeometryMasks.Quad.EAST
                                            or GeometryMasks.Quad.WEST or GeometryMasks.Quad.SOUTH
                                    )
                                    neededQuads.add(
                                        GeometryMasks.Quad.UP or GeometryMasks.Quad.EAST
                                            or GeometryMasks.Quad.WEST or GeometryMasks.Quad.SOUTH
                                    )
                                }
                                continue
                            }
                        }
                        if (west && air(node.up().west())) {
                            if (isEastWestTunnel(node, north, northWest)) {
                                tunnels.add(node.toLong())
                                if (air(node.east()) && air(node.up().east())) {
                                    neededQuads.add(
                                        GeometryMasks.Quad.DOWN or GeometryMasks.Quad.NORTH or GeometryMasks.Quad.SOUTH
                                    )
                                    neededQuads.add(
                                        GeometryMasks.Quad.UP or GeometryMasks.Quad.NORTH or GeometryMasks.Quad.SOUTH
                                    )
                                } else {
                                    neededQuads.add(
                                        GeometryMasks.Quad.DOWN or GeometryMasks.Quad.NORTH
                                            or GeometryMasks.Quad.SOUTH or GeometryMasks.Quad.EAST
                                    )
                                    neededQuads.add(
                                        GeometryMasks.Quad.UP or GeometryMasks.Quad.NORTH
                                            or GeometryMasks.Quad.SOUTH or GeometryMasks.Quad.EAST
                                    )
                                }
                            }
                        }
                    }
                }
            }

            var i = 0
            for (tunnel in tunnels) {
                if (mode == RenderMode.TUNNEL) {
                    val pos = BlockPos.fromLong(tunnel)
                    cache.add(Triple(AxisAlignedBB(pos), renderColor, neededQuads.getInt(i)))
                    cache.add(Triple(AxisAlignedBB(pos.up()), renderColor, neededQuads.getInt(i + 1)))
                } else cache.add(Triple(AxisAlignedBB(BlockPos.fromLong(tunnel)), renderColor, GeometryMasks.Quad.DOWN))
                i += 2
            }

            renderer?.replaceAll(cache)
        }
    }

    private fun SafeClientEvent.air(node: BlockPos): Boolean {
        return world.getBlockState(node).block is BlockAir
    }

    init {
        safeListener<RenderWorldEvent> {
            if (isDisabled) return@safeListener
            if (renderer == null) {
                renderer = getRenderer()
            } else {
                if (timer.tick(169)) {
                    updateRenderer()
                }
                renderer?.render(false)
            }
        }
    }
}