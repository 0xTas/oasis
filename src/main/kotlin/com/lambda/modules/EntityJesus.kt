package com.lambda.modules

import com.lambda.Oasis
import kotlin.math.floor
import kotlinx.coroutines.delay
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import com.lambda.client.module.Category
import net.minecraft.entity.item.EntityBoat
import net.minecraft.util.math.AxisAlignedBB
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.mixin.extension.playerY
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.threads.BackgroundJob
import com.lambda.client.event.events.PlayerMoveEvent
import net.minecraft.network.play.client.CPacketPlayer
import com.lambda.client.mixin.extension.playerIsOnGround
import com.lambda.client.util.EntityUtils.flooredPosition
import net.minecraft.network.play.server.SPacketPlayerPosLook
import com.lambda.client.event.events.AddCollisionBoxToListEvent


// Credit to the writers and contributors of the Jesus module in Lambda 3.3 for the starting point.
/**
 * @author 0xTas [@0xTas] <root@0xTas.dev>
 */
object EntityJesus : PluginModule(
    name = "EntityJesus",
    description = "Ride entities (mainly horses) over water",
    category = Category.MOVEMENT,
    pluginMain = Oasis
) {
    private var fakeY = 0.0
    private var toLand = true
    private var interval = false

    // Prevent fall distance from building up and thanos-snapping your poor horse upon returning to land
    val job = BackgroundJob("EntityJesus", 2500) {
        interval = true
        delay(100)
        interval = false
    }

    init {
        onDisable {
            toLand = true
            fakeY = .0
        }

        safeListener<AddCollisionBoxToListEvent> {
            if (interval) return@safeListener
            val entity = player.ridingEntity ?: return@safeListener
            if (mc.gameSettings.keyBindSneak.isKeyDown || entity is EntityBoat) return@safeListener
            if (isOnLand(entity)) {toLand = true; return@safeListener}

            if (world.getBlockState(BlockPos(entity.positionVector.add(.0, -.1 + entity.motionY, .0))).material.isLiquid
            ) {
                val bb = entity.entityBoundingBox
                it.collisionBoxList.add(AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + 0.1, bb.maxZ)
                    .offset(entity.posX, floor(entity.posY), entity.posZ))
            }
        }

        safeListener<PlayerMoveEvent> { event ->
            if (interval) return@safeListener
            val entity = player.ridingEntity ?: return@safeListener
            if (mc.gameSettings.keyBindSneak.isKeyDown|| entity is EntityBoat) return@safeListener
            if (isOnLand(entity)) {toLand = true; return@safeListener}

            if (entity.isInWater || world.getBlockState(entity.flooredPosition).material.isLiquid) {
                toLand = false
                val targetMotionY = 0.11
                val diff = (targetMotionY - entity.motionY) / 2
                event.y = entity.motionY + diff
                entity.motionY += diff
            }

            // Gotta go fast
            if (!entity.onGround)
                entity.onGround = true
        }

        safeListener<PacketEvent.Send> { event ->
            if (interval) return@safeListener
            if (event.packet !is CPacketPlayer) return@safeListener

            val entity = player.ridingEntity ?: return@safeListener
            if (entity is EntityBoat) return@safeListener
            if (isOnLand(entity)) {toLand = true; return@safeListener}

            if (mc.gameSettings.keyBindSneak.isKeyDown) {
                entity.posY -= fakeY
                fakeY = 0.0
                return@safeListener
            }

            val entityBB = entity.entityBoundingBox
            val nextBB = entityBB.offset(entity.motionX * 1.5, -0.001, entity.motionZ * 1.5)
            val feetBB = entityBB.contract(0.0, (entity.height - entity.stepHeight).toDouble(), 0.0)
            val terrainBB = world.getCollisionBoxes(entity, nextBB).filter { it.intersects(feetBB) }

            val packet = event.packet as CPacketPlayer

            // Assist a smooth transition to land without rubberbanding (ideally; it's not perfect)
            if (world.getCollisionBoxes(entity, nextBB).isNotEmpty() && entity.motionY > 0 && !toLand) {
                val maxY = terrainBB.maxByOrNull { it.maxY }?.maxY ?: entity.posY

                entity.posY = maxY + (entity.stepHeight / 2)
                packet.playerY = entity.posY
                fakeY = 0.0
                toLand = true
                return@safeListener
            }

            packet.playerIsOnGround = true
            val targetY = entity.posY + fakeY
            val diff = (targetY - packet.playerY) / 2
            packet.playerY += diff
        }

        safeListener<PacketEvent.Receive> {
            if (interval) return@safeListener
            if (it.packet !is SPacketPlayerPosLook) return@safeListener
            val entity = player.ridingEntity ?: return@safeListener
            if (entity is EntityBoat) return@safeListener
            if (isOnLand(entity)) {toLand = true; return@safeListener}

            fakeY = player.posY - (it.packet as SPacketPlayerPosLook).y
        }
    }

    private fun isOnLand(entity: Entity): Boolean {
        val world = mc.world ?: return true
        return (!world.getBlockState(BlockPos(entity.positionVector.add(.0, -.1 + entity.motionY, .0))).material.isLiquid && !entity.isInWater)
    }

}