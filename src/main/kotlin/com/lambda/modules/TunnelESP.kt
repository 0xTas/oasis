package com.lambda.modules

import com.lambda.Oasis
import kotlinx.coroutines.launch
import net.minecraft.block.BlockAir
import net.minecraft.util.math.BlockPos
import com.lambda.client.util.TickTimer
import com.lambda.client.module.Category
import net.minecraft.util.math.AxisAlignedBB
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.graphics.GeometryMasks
import com.lambda.client.event.events.RenderWorldEvent


/**
 * @author 0xTas [Tas#1337] <root@0xTas.dev>
 */
internal object TunnelESP: PluginModule(
    name = "TunnelESP",
    description = "Highlights narrow tunnels in the world around you",
    category = Category.RENDER,
    pluginMain = Oasis
){
    private val mode by setting(
        "Render Style", value = RenderMode.TUNNEL, description = "Recommended: Tunnel for NR, Base for OW")
    private val fill by setting("Base Fill", value = true, {mode == RenderMode.BASE})
    private val outline by setting("Base Outline", value = true, {mode == RenderMode.BASE})
    private val tracer by setting("Base Tracer", value = false, {mode == RenderMode.BASE})
    private val color by setting(
        "Base Color", ColorHolder(41, 210, 146), false, {mode == RenderMode.BASE})
    private val aFill by setting(
        "Base Fill Opacity", value = 69, range = 0..255, step = 1, {fill && mode == RenderMode.BASE})
    private val aOutline by setting(
        "Base Outline Opacity", value = 133, range = 0..255, step = 1, {outline && mode == RenderMode.BASE})
    private val aTracer by setting(
        "Base Tracer Opacity", value = 20, range = 0..255, step = 1, {tracer && mode == RenderMode.BASE})
    private val tunFill by setting("Tunnel Fill", value = true, {mode == RenderMode.TUNNEL})
    private val tunOutline by setting("Tunnel Outline", value = true, {mode == RenderMode.TUNNEL})
    private val tunTracer by setting("Tunnel Tracer", value = false, {mode == RenderMode.TUNNEL})
    private val tunColor by setting(
        "Tunnel Color", ColorHolder(41, 210, 146), false, {mode == RenderMode.TUNNEL})
    private val aTunFill by setting(
        "Tunnel Fill Opacity", value = 10, range = 0..255, step = 1, {tunFill && mode == RenderMode.TUNNEL})
    private val aTunOutline by setting(
        "Tunnel Outline Opacity", value = 11, range = 0..255, step = 1, {tunOutline && mode == RenderMode.TUNNEL})
    private val aTunTracer by setting(
        "Tunnel Tracer Opacity", value = 20, range = 0..255, step = 1, {tunTracer && mode == RenderMode.TUNNEL})
    private val eFly by setting(
        "Overworld Efly Mode",
        value = false, description = "Don't bother scanning above the player, but scan down to bedrock")

    private val timer = TickTimer()
    private var renderer: ESPRenderer? = null

    private enum class RenderMode {
        BASE, TUNNEL
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
            RenderMode.BASE -> {
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
            val tunnels = ArrayList<BlockPos>()
            val cache = ArrayList<Triple<AxisAlignedBB, ColorHolder, Int>>()
            val tPos = player.position
            val dim = player.dimension

            val range = 4 * 16
            val startX = tPos.x - range
            val endX = tPos.x + range
            val startZ = tPos.z - range
            val endZ = tPos.z + range
            val startY = 2
            val endY = if (dim == -1 || eFly) {
                if (dim == -1) 121 else player.posY.toInt()
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
                                tunnels.add(BlockPos(node.x, node.y, node.z))
                                continue
                            }
                        }
                        if (west && air(node.up().west())) {
                            if (isEastWestTunnel(node, north, northWest)) {
                                tunnels.add(BlockPos(node.x, node.y, node.z))
                            }
                        }
                    }
                }
            }

            for (tunnel in tunnels) {
                if (mode == RenderMode.TUNNEL) {
                    cache.add(Triple(AxisAlignedBB(tunnel), renderColor, GeometryMasks.Quad.ALL))
                    cache.add(Triple(AxisAlignedBB(tunnel.up()), renderColor, GeometryMasks.Quad.ALL))
                } else cache.add(Triple(AxisAlignedBB(tunnel), renderColor, GeometryMasks.Quad.DOWN))
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