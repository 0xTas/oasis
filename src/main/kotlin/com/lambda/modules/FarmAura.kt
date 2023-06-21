package com.lambda.modules

import com.lambda.Oasis
import kotlin.math.sqrt
import kotlin.math.atan2
import net.minecraft.block.*
import net.minecraft.item.Item
import net.minecraft.init.Items
import net.minecraft.init.Blocks
import net.minecraft.world.World
import kotlinx.coroutines.launch
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemBow
import net.minecraft.util.EnumHand
import net.minecraft.item.ItemSpade
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.Vec3d
import net.minecraft.util.EnumFacing
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemAppleGold
import com.lambda.client.util.TickTimer
import net.minecraft.util.math.BlockPos
import com.lambda.client.module.Category
import net.minecraft.util.math.MathHelper
import net.minecraft.network.play.client.*
import net.minecraft.block.state.IBlockState
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.util.items.swapToItem
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import net.minecraft.client.multiplayer.WorldClient
import com.lambda.client.util.MovementUtils.isMoving
import com.lambda.client.util.items.swapToItemOrMove
import com.lambda.client.event.events.ConnectionEvent
import com.lambda.client.event.events.RenderWorldEvent


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object FarmAura: PluginModule(
    name = "FarmAura",
    category = Category.PLAYER,
    description = "Automatically preforms farming tasks for you",
    pluginMain = Oasis
) {
    private val page by setting("Page", value = Page.MODES)
    private val shouldReplant by setting("Plant Seeds", value = true, {page == Page.MODES})
    private val shouldBonemeal by setting("Use Bonemeal", value = true, {page == Page.MODES})
    private val shouldHarvest by setting("Harvest Plants", value = true, {page == Page.MODES})
    private val interactWheat by setting("Wheat", value = true, {page == Page.CROPS})
    private val interactPotatoes by setting("Potatoes", value = true, {page == Page.CROPS})
    private val interactCarrots by setting("Carrots", value = true, {page == Page.CROPS})
    private val interactBeetroot by setting("Beetroot", value = true, {page == Page.CROPS})
    private val interactReeds by setting("Sugar Cane", value = true, {page == Page.CROPS})
    private val interactNetherWart by setting("Nether Wart", value = true, {page == Page.CROPS})
    private val reachRange by setting(
        "Range",
        value = 3,
        range = 1..5,
        step = 1,
        description = "Reach range. Lower values may help in strict environments."
    )
    private val seeds = hashMapOf(
        Blocks.REEDS to Items.REEDS,
        Blocks.CARROTS to Items.CARROT,
        Blocks.POTATOES to Items.POTATO,
        Blocks.WHEAT to Items.WHEAT_SEEDS,
        Blocks.NETHER_WART to Items.NETHER_WART,
        Blocks.BEETROOTS to Items.BEETROOT_SEEDS,
    )

    private enum class Page {
        MODES, CROPS
    }

    private val timer = TickTimer()
    private var farmTasks: HashMap<BlockPos, Item> = HashMap()


    private fun interactMap(): HashMap<Block, Boolean> {
        return hashMapOf(
            Blocks.REEDS to interactReeds,
            Blocks.WHEAT to interactWheat,
            Blocks.POTATOES to interactPotatoes,
            Blocks.CARROTS to interactCarrots,
            Blocks.BEETROOTS to interactBeetroot,
            Blocks.NETHER_WART to interactNetherWart
        )
    }

    private fun SafeClientEvent.registerFarmTasks(blocks: ArrayList<BlockPos>): ArrayList<BlockPos> {
        val actionableBlocks = ArrayList<BlockPos>()
        val world = mc.player?.world ?: return actionableBlocks

        blocks.parallelStream()
            .filter {crop ->
                (seeds.containsKey(world.getBlockState(crop).block)
                || (world.getBlockState(crop.down()).block is BlockFarmland
                && world.getBlockState(crop).block is BlockAir)
                    || (world.getBlockState(crop.down()).block is BlockSoulSand
                    && world.getBlockState(crop).block is BlockAir))
            }
            .forEach {crop ->
                val cropBlock = world.getBlockState(crop).block
                val seed = seeds[cropBlock]
                if (seed != null && interactMap()[cropBlock] != null) {
                    if (farmTasks[crop] == null && interactMap()[cropBlock]!!)
                        farmTasks[crop] = seed
                    actionableBlocks.add(crop)
                }else if (world.getBlockState(crop.down()).block is BlockFarmland
                    && world.getBlockState(crop).block is BlockAir)
                {
                    if (farmTasks[crop] == null)
                        farmTasks[crop] = Items.WHEAT_SEEDS
                    actionableBlocks.add(crop)
                }else if (world.getBlockState(crop.down()).block is BlockSoulSand
                    && world.getBlockState(crop).block is BlockAir) {
                    if (farmTasks[crop] == null)
                        farmTasks[crop] = Items.NETHER_WART
                    actionableBlocks.add(crop)
                }
            }
        return actionableBlocks
    }

    private fun SafeClientEvent.canHarvest(blockState: IBlockState, pos: BlockPos): Boolean {
        val world = mc.player?.world ?: return false

        return when (val block = blockState.block) {
            is BlockCrops -> block.isMaxAge(blockState)
            is BlockPumpkin, is BlockMelon -> false // For now..
            is BlockReed -> world.getBlockState(pos.down()).block is BlockReed && world.getBlockState(pos.up()).block is BlockReed
            is BlockNetherWart -> blockState.getValue(BlockNetherWart.AGE) >= 3
            else -> false
        }
    }

    private fun SafeClientEvent.canBonemeal(pos: BlockPos): Boolean {
        val world = mc.player?.world ?: return false
        val state = world.getBlockState(pos)
        return when (val block = state.block) {
            is BlockCrops -> !block.isMaxAge(state)
            else -> false
        }
    }

    private fun SafeClientEvent.canReplant(pos: BlockPos): Boolean {
        val world: World = mc.player?.world ?: return false
        val item = farmTasks[pos] ?: return false

        if (item == Items.WHEAT_SEEDS || item == Items.CARROT || item == Items.POTATO
            || item == Items.BEETROOT_SEEDS) {
            return (world.getBlockState(pos.down()).block is BlockFarmland
                && world.getBlockState(pos).block is BlockAir)
        }
        if (item == Items.NETHER_WART) return (world.getBlockState(pos.down()).block is BlockSoulSand
            && world.getBlockState(pos).block is BlockAir)

        return false
    }

    private fun SafeClientEvent.tryReplant(needed: Item, pos: BlockPos): Boolean {
        val world: World = mc.player?.world ?: return false
        val currentItem = player.getHeldItem(EnumHand.MAIN_HAND)
        val boneMeal = ItemStack(Item.getItemById(351), 1, 15).item

        if (needed == boneMeal) {
            val interact = interactMap()[world.getBlockState(pos).block]
            if (interact != null) {
                if (!interact) {
                    return true
                }
            }
        }

        val hitVec = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        val offsetVec = Vec3d(pos.x + 0.5, pos.y + 0.9375, pos.z + 0.5)

        if (!currentItem.isEmpty && currentItem.item == needed) {
            val look = player.rotationYaw
            val pitch = player.rotationPitch
            val neededRots = lookAtBlock(hitVec)

            if (player.isMoving) {
                player.connection.sendPacket(CPacketPlayer.PositionRotation(
                    player.posX, player.posY, player.posZ,
                    neededRots[0], neededRots[1], player.onGround)
                )
            } else {
                player.connection.sendPacket(CPacketPlayer.Rotation(
                    neededRots[0], neededRots[1], player.onGround)
                )
            }
            mc.playerController.processRightClickBlock(
                player,
                world as WorldClient,
                pos,
                EnumFacing.UP,
                offsetVec,
                EnumHand.MAIN_HAND
            )
            player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            if (player.isMoving) {
                player.connection.sendPacket(CPacketPlayer.PositionRotation(
                    player.posX, player.posY, player.posZ,
                    look, pitch, player.onGround)
                )
            } else {
                player.connection.sendPacket(CPacketPlayer.Rotation(
                    look, pitch, player.onGround)
                )
            }
            farmTasks.remove(pos)
            return true
        }

        val cropsNeeded = mutableListOf<Item>()
        when (needed) {
            Items.WHEAT_SEEDS -> {
                if (interactWheat) cropsNeeded.add(Items.WHEAT_SEEDS)
                if (interactBeetroot) cropsNeeded.add(Items.BEETROOT_SEEDS)
                if (interactCarrots) cropsNeeded.add(Items.CARROT)
                if (interactPotatoes) cropsNeeded.add(Items.POTATO)
            }
            Items.BEETROOT_SEEDS -> {
                if (interactBeetroot) cropsNeeded.add(Items.BEETROOT_SEEDS)
                if (interactWheat) cropsNeeded.add(Items.WHEAT_SEEDS)
                if (interactCarrots) cropsNeeded.add(Items.CARROT)
                if (interactPotatoes) cropsNeeded.add(Items.POTATO)
            }
            Items.CARROT -> {
                if (interactCarrots) cropsNeeded.add(Items.CARROT)
                if (interactPotatoes) cropsNeeded.add(Items.POTATO)
                if (interactBeetroot) cropsNeeded.add(Items.BEETROOT_SEEDS)
                if (interactWheat) cropsNeeded.add(Items.WHEAT_SEEDS)
            }
            Items.POTATO -> {
                if (interactPotatoes) cropsNeeded.add(Items.POTATO)
                if (interactCarrots) cropsNeeded.add(Items.CARROT)
                if (interactBeetroot) cropsNeeded.add(Items.BEETROOT_SEEDS)
                if (interactWheat) cropsNeeded.add(Items.WHEAT_SEEDS)
            }
            Items.NETHER_WART -> {
                if (interactNetherWart) cropsNeeded.add(Items.NETHER_WART)
            }
            boneMeal -> if (shouldBonemeal) cropsNeeded.add(boneMeal)
            else -> return false
        }

        if (cropsNeeded.isEmpty()) return false
        for (neededItem in cropsNeeded) {
            if (!swapToNeededItem(neededItem)) continue

            val look = player.rotationYaw
            val pitch = player.rotationPitch
            val neededRots = lookAtBlock(hitVec)

            if (player.isMoving) {
                player.connection.sendPacket(CPacketPlayer.PositionRotation(
                    player.posX, player.posY, player.posZ,
                    neededRots[0], neededRots[1], player.onGround)
                )
            } else {
                player.connection.sendPacket(CPacketPlayer.Rotation(
                    neededRots[0], neededRots[1], player.onGround)
                )
            }
            mc.playerController.processRightClickBlock(
                player,
                world as WorldClient,
                pos,
                EnumFacing.UP,
                offsetVec,
                EnumHand.MAIN_HAND
            )
            player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            if (player.isMoving) {
                player.connection.sendPacket(CPacketPlayer.PositionRotation(
                    player.posX, player.posY, player.posZ,
                    look, pitch, player.onGround)
                )
            } else {
                player.connection.sendPacket(CPacketPlayer.Rotation(
                    look, pitch, player.onGround)
                )
            }
            farmTasks.remove(pos)
            return true
        }
        return false
    }

    private fun SafeClientEvent.swapToNeededItem(needed: Item): Boolean {
        val swapped = swapToItem(needed) {
            it.item == needed
        }
        if (!swapped) {
            val moved = swapToItemOrMove(
                this@FarmAura,
                needed,
                predicateItem = {it.item == needed},
                predicateSlot = {
                    it.item !is ItemSword && it.item !is ItemPickaxe
                        && it.item !is ItemSpade && it.item !is ItemBow
                        && it.item !is ItemAxe && it.item !is ItemAppleGold
                }
            )
            if (!moved) return false
        }
        return true
    }

    private fun SafeClientEvent.harvestCrop(block: BlockPos): Boolean {
        val player = mc.player ?: return false

        val blockCrop = player.world.getBlockState(block).block
        val interact = interactMap()[blockCrop]
        if (interact != null) {
            if (!interact) {
                return true
            }
        }

        val deltaPos = block.subtract(Vec3i(player.posX, player.posY, player.posZ))
        val side: EnumFacing = when {
            deltaPos.x > 0 -> EnumFacing.WEST
            deltaPos.x < 0 -> EnumFacing.EAST
            deltaPos.z > 0 -> EnumFacing.NORTH
            deltaPos.z < 0 -> EnumFacing.SOUTH
            deltaPos.y > 0 -> EnumFacing.DOWN
            deltaPos.y < 0 -> EnumFacing.UP
            else -> EnumFacing.UP
        }

        val hitVec = Vec3d(block.x + 0.5, block.y + 0.5, block.z + 0.5)
        val look = player.rotationYaw
        val pitch = player.rotationPitch
        val neededRots = lookAtBlock(hitVec)

        if (player.isMoving) {
            player.connection.sendPacket(CPacketPlayer.PositionRotation(
                player.posX, player.posY, player.posZ,
                neededRots[0], neededRots[1], player.onGround)
            )
        } else {
            player.connection.sendPacket(CPacketPlayer.Rotation(
                neededRots[0], neededRots[1], player.onGround)
            )
        }
        if (!mc.playerController.onPlayerDamageBlock(block, side)) {
            return false
        }
        player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        if (player.isMoving) {
            player.connection.sendPacket(CPacketPlayer.PositionRotation(
                player.posX, player.posY, player.posZ,
                look, pitch, player.onGround)
            )
        } else {
            player.connection.sendPacket(CPacketPlayer.Rotation(
                look, pitch, player.onGround)
            )
        }

        return true
    }

    private fun SafeClientEvent.getSurroundingBlocks(playerPos: BlockPos, range: Int): ArrayList<BlockPos> {
        val positions = ArrayList<BlockPos>()
        if (mc.player == null || mc.player.world == null) return positions
        for (x in playerPos.x - range..playerPos.x + range) {
            for (y in playerPos.y - range..playerPos.y + range) {
                for (z in playerPos.z - range..playerPos.z + range) {
                    positions.add(BlockPos(x, y, z))
                }
            }
        }
        return positions
    }

    private fun lookAtBlock(hitVec: Vec3d): FloatArray {
        val player = mc.player ?: return floatArrayOf()
        val eyesPos = player.getPositionEyes(1F)

        val xDiff = hitVec.x - eyesPos.x
        val yDiff = hitVec.y - eyesPos.y
        val zDiff = hitVec.z - eyesPos.z
        val zxDiff = sqrt(xDiff * xDiff + zDiff * zDiff)

        val yaw = Math.toDegrees(atan2(zDiff, xDiff)) - 90F
        val pitch = -Math.toDegrees(atan2(yDiff, zxDiff))

        return floatArrayOf(MathHelper.wrapDegrees(yaw).toFloat(), MathHelper.wrapDegrees(pitch).toFloat())
    }


    init {
        onDisable {
            farmTasks.clear()
        }

        safeListener<ConnectionEvent.Disconnect> {
            farmTasks.clear()
        }

        safeListener<RenderWorldEvent> {
            if (timer.tick(169) && isEnabled) {
                defaultScope.launch {
                    try {
                        val blocks = getSurroundingBlocks(player.position, reachRange)
                        val actionableBlocks = registerFarmTasks(blocks)
                        if (shouldHarvest) {
                            for (block in actionableBlocks) {
                                if (canHarvest(world.getBlockState(block), block)) {
                                    if (harvestCrop(block)) return@launch
                                }
                            }
                        }

                        if (shouldReplant) {
                            for (block in actionableBlocks) {
                                if (canReplant(block) && farmTasks[block] != null) {
                                    if (tryReplant(farmTasks[block]!!, block.down())) {
                                        return@launch
                                    }
                                }
                            }
                        }

                        if (shouldBonemeal) {
                            for (block in actionableBlocks) {
                                if (canBonemeal(block)) {
                                    val boneMeal = ItemStack(Item.getItemById(351), 1, 15).item
                                    if (tryReplant(boneMeal, block)) return@launch
                                }
                            }
                        }
                    }catch (e: NullPointerException) {return@launch}
                }
            }
        }
    }
}