package dev.oasis.modules

import dev.oasis.Oasis
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlinx.coroutines.launch
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.block.BlockDoor
import net.minecraft.util.math.BlockPos
import com.lambda.client.module.Category
import net.minecraft.util.math.MathHelper
import com.lambda.client.event.SafeClientEvent
import it.unimi.dsi.fastutil.longs.LongArrayList
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.MovementUtils.isMoving
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.event.events.PlayerMoveEvent
import com.lambda.client.event.events.ConnectionEvent
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object AutoDoor : PluginModule(
    name = "AutoDoor",
    description = "Automatically closes doors behind you like a civilized steve",
    category = Category.PLAYER,
    pluginMain = Oasis
) {
    private val autoOpen by setting("Auto Open", value = true, description = "Automatically open doors in front of you")
    private val spamRange by setting("Spam Range", value = 5, range = 1..6, step = 1, { spammer })
    private val tickRate by setting("Spam Delay", value = 5, range = 2..20, step = 1, { spammer })

    private var spammer = false
    private var tickCounter = 0
    private var toggleCounter = 0
    private var lastOpen = autoOpen
    private var lastBlock = Triple(0.0, 0.0, 0.0)


    private fun SafeClientEvent.lookAtBlock(hitVec: Vec3d): FloatArray {
        val eyesPos = player.getPositionEyes(1F)

        val xDiff = hitVec.x - eyesPos.x
        val yDiff = hitVec.y - eyesPos.y
        val zDiff = hitVec.z - eyesPos.z
        val zxDiff = sqrt(xDiff * xDiff + zDiff * zDiff)

        val yaw = Math.toDegrees(atan2(zDiff, xDiff)) - 90F
        val pitch = -Math.toDegrees(atan2(yDiff, zxDiff))

        return floatArrayOf(MathHelper.wrapDegrees(yaw).toFloat(), MathHelper.wrapDegrees(pitch).toFloat())
    }

    private fun SafeClientEvent.interactDoor(pos: BlockPos, movementDirection: EnumFacing) {
        val look = player.rotationYaw
        val pitch = player.rotationPitch
        val hitVec = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        val neededRots = lookAtBlock(hitVec)

        val pPos = player.position
        val side = when (movementDirection) {
            EnumFacing.EAST -> if (player.posX < pos.x) EnumFacing.WEST else EnumFacing.EAST
            EnumFacing.WEST -> if (player.posX > pos.x) EnumFacing.EAST else EnumFacing.WEST
            EnumFacing.NORTH -> if (player.posZ > pos.z) EnumFacing.SOUTH else EnumFacing.NORTH
            EnumFacing.SOUTH -> if (player.posZ < pos.z) EnumFacing.NORTH else EnumFacing.SOUTH
            EnumFacing.UP -> if (pPos.y < pos.y) EnumFacing.DOWN else EnumFacing.UP
            EnumFacing.DOWN -> if (pPos.y > pos.y) EnumFacing.DOWN else EnumFacing.UP
        }

        if (player.isMoving) {
            player.connection.sendPacket(CPacketPlayer.PositionRotation(
                player.posX, player.posY, player.posZ,
                neededRots[0], neededRots[1], player.onGround
            ))
        } else {
            player.connection.sendPacket(CPacketPlayer.Rotation(
                neededRots[0], neededRots[1], player.onGround
            ))
        }
        mc.playerController.processRightClickBlock(
            player,
            world,
            pos,
            side,
            hitVec,
            EnumHand.MAIN_HAND
        )
        player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        if (player.isMoving) {
            player.connection.sendPacket(CPacketPlayer.PositionRotation(
                player.posX, player.posY, player.posZ,
                look, pitch, player.onGround
            ))
        } else {
            player.connection.sendPacket(CPacketPlayer.Rotation(
                look, pitch, player.onGround
            ))
        }
    }

    private fun SafeClientEvent.getSurroundingDoors(): LongArrayList {
        val doors = LongArrayList()
        val pPos = player.position
        val xRange = pPos.x - spamRange..pPos.x + spamRange
        val yRange = pPos.y - spamRange..pPos.y + spamRange
        val zRange = pPos.z - spamRange..pPos.z + spamRange

        val pos = BlockPos.MutableBlockPos()
        for (x in xRange) for (y in yRange) for (z in zRange) {
            pos.setPos(x,y,z)
            if (world.getBlockState(pos).block is BlockDoor) {
                doors.add(pos.toLong())
            }
        }

        return doors
    }


    init {
        onDisable {
            tickCounter = 0
        }

        safeListener<ConnectionEvent.Disconnect> {
            tickCounter = 0
        }

        safeListener<PlayerMoveEvent> {moveEvent ->
            if (isDisabled || spammer) return@safeListener

            val pPos = Triple(player.posX, player.posY, player.posZ)
            if (pPos.first <= lastBlock.first + 0.33
                && pPos.first >= lastBlock.first - 0.33
                && pPos.third <= lastBlock.third + 0.33
                && pPos.third >= lastBlock.third - 0.33) return@safeListener

            lastBlock = pPos
            val velocityX = moveEvent.x
            val velocityY = moveEvent.y
            val velocityZ = moveEvent.z

            val directionRadians = atan2(-velocityZ, velocityX)
            val directionDegrees = Math.toDegrees(directionRadians)
            val normalizedDegrees = (directionDegrees + 360) % 360

            val movementDirection = when {
                normalizedDegrees >= 45 && normalizedDegrees < 135 -> EnumFacing.NORTH
                normalizedDegrees >= 135 && normalizedDegrees < 225 -> EnumFacing.WEST
                normalizedDegrees >= 225 && normalizedDegrees < 315 -> EnumFacing.SOUTH
                normalizedDegrees >= 315 || normalizedDegrees < 45 -> EnumFacing.EAST
                else -> {
                    when {
                        velocityY > 0 -> EnumFacing.UP
                        velocityY < 0 -> EnumFacing.DOWN
                        else -> EnumFacing.UP
                    }
                }
            }

            val frontPos: BlockPos
            val behindPos: BlockPos
            when (movementDirection) {
                EnumFacing.NORTH -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).north()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).south()
                }
                EnumFacing.SOUTH -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).south()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).north()
                }
                EnumFacing.EAST -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).east()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).west()
                }
                EnumFacing.WEST -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).west()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).east()
                }
                EnumFacing.UP -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).up()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).down()
                }
                else -> {
                    frontPos = BlockPos(pPos.first,pPos.second,pPos.third).down()
                    behindPos = BlockPos(pPos.first,pPos.second,pPos.third).up()
                }
            }

            val doorInFront = world.getBlockState(frontPos).block
            val doorBehind = world.getBlockState(behindPos).block
            if (doorInFront is BlockDoor && autoOpen) {
                if (!doorInFront.isPassable(world, frontPos)) interactDoor(frontPos, movementDirection)
                when (movementDirection) {
                    EnumFacing.NORTH, EnumFacing.SOUTH -> {
                        if (world.getBlockState(frontPos.east()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(frontPos.east()).block as BlockDoor
                            if (!nextDoor.isPassable(world, frontPos.east())) interactDoor(frontPos.east(), movementDirection)
                        } else if (world.getBlockState(frontPos.west()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(frontPos.west()).block as BlockDoor
                            if (!nextDoor.isPassable(world, frontPos.west())) interactDoor(frontPos.west(), movementDirection)
                        }
                    }
                    else -> {
                        if (world.getBlockState(frontPos.north()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(frontPos.north()).block as BlockDoor
                            if (!nextDoor.isPassable(world, frontPos.north())) interactDoor(frontPos.north(), movementDirection)
                        } else if (world.getBlockState(frontPos.south()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(frontPos.south()).block as BlockDoor
                            if (!nextDoor.isPassable(world, frontPos.south())) interactDoor(frontPos.south(), movementDirection)
                        }
                    }
                }
            }
            if (doorBehind is BlockDoor) {
                if (doorBehind.isPassable(world, behindPos)) interactDoor(behindPos, movementDirection)
                when (movementDirection) {
                    EnumFacing.NORTH, EnumFacing.SOUTH -> {
                        if (world.getBlockState(behindPos.east()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(behindPos.east()).block as BlockDoor
                            if (nextDoor.isPassable(world, behindPos.east())) interactDoor(behindPos.east(), movementDirection)
                        } else if (world.getBlockState(behindPos.west()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(behindPos.west()).block as BlockDoor
                            if (nextDoor.isPassable(world, behindPos.west())) interactDoor(behindPos.west(), movementDirection)
                        }
                    }
                    else -> {
                        if (world.getBlockState(behindPos.north()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(behindPos.north()).block as BlockDoor
                            if (nextDoor.isPassable(world, behindPos.north())) interactDoor(behindPos.north(), movementDirection)
                        } else if (world.getBlockState(behindPos.south()).block is BlockDoor) {
                            val nextDoor = world.getBlockState(frontPos.south()).block as BlockDoor
                            if (nextDoor.isPassable(world, behindPos.south())) interactDoor(behindPos.south(), movementDirection)
                        }
                    }
                }
            }
        }

        safeListener<ClientTickEvent> {
            if (it.phase != TickEvent.Phase.END) return@safeListener
            if (tickCounter >= 32767) tickCounter = 0

            ++tickCounter
            if (autoOpen != lastOpen) ++toggleCounter; lastOpen = autoOpen

            if (toggleCounter > 7) {
                spammer = !spammer
                when {
                    spammer -> MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4Being rude... §8(§7Door Spammer §2Enabled§7!§8)")
                    else -> MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §2Being polite... §8(§7Door Spammer §4Disabled§7!§8)")
                }
                toggleCounter = 0
            }

            if (tickCounter % 60 == 0) toggleCounter = 0

            if (spammer && tickCounter % tickRate == 0) {
                defaultScope.launch {
                    val doors = getSurroundingDoors()
                    for (pos in doors) {
                        val doorPos = BlockPos.fromLong(pos)
                        val side = when {
                            player.posX > doorPos.x -> EnumFacing.EAST
                            player.posX < doorPos.x -> EnumFacing.WEST
                            player.posZ > doorPos.z -> EnumFacing.SOUTH
                            player.posZ < doorPos.z -> EnumFacing.NORTH
                            player.posY > doorPos.y -> EnumFacing.UP
                            player.posY < doorPos.y -> EnumFacing.DOWN
                            else -> EnumFacing.DOWN
                        }

                        interactDoor(doorPos, side)
                    }
                }
            }
        }
    }
}