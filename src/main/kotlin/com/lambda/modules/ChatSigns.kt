package com.lambda.modules

import com.lambda.Oasis
import kotlinx.coroutines.launch
import net.minecraft.block.BlockSign
import net.minecraft.util.math.BlockPos
import com.lambda.client.util.TickTimer
import com.lambda.client.module.Category
import com.lambda.client.event.SafeClientEvent
import net.minecraft.tileentity.TileEntitySign
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.event.events.ConnectionEvent
import com.lambda.client.event.events.RenderWorldEvent


/**
 * @author 0xTas [Tas#1337] <root@0xTas.dev>
 */
internal object ChatSigns : PluginModule(
    name = "ChatSigns",
    description = "Read nearby signs in your chat",
    category = Category.CHAT,
    pluginMain = Oasis
){
    private val posSet = hashSetOf<BlockPos>()
    private val chunks by setting("Chunks", value = 7, range = 1..16, step = 1, description = "Range of chunks to scan in")
    private val showCoords by setting("Show Coordinates", value = true, description = "Include sign coordinates")
    private val oldSigns by setting("Show Old Status", value = true , description = "Annotate signs placed prior to Minecraft version 1.8")
    private val displayMode by setting("Display Mode", value = DisplayMode.FANCY, description = "Toggle between fancy/compact chat")

    private val timer = TickTimer()

    private enum class DisplayMode {
        FANCY, COMPACT
    }


    private fun SafeClientEvent.getSurroundingSigns(playerPos: BlockPos): ArrayList<BlockPos> {
        val signList = ArrayList<BlockPos>()

        val range = chunks * 16
        val startX = playerPos.x - range
        val startY = 1
        val startZ = playerPos.z - range
        val endX = playerPos.x + range
        val endY = if (player.dimension == -1) 125 else world.height
        val endZ = playerPos.z + range

        val pos = BlockPos.MutableBlockPos(0,0,0)
        for (x in startX..endX) {
            for (y in startY..endY) {
                for (z in startZ..endZ) {
                    pos.setPos(x,y,z)
                    if (world.getBlockState(pos).block is BlockSign) {
                        signList.add(BlockPos(pos.x, pos.y, pos.z))
                    }
                }
            }
        }
        return signList
    }

    private fun SafeClientEvent.chatSign(sign: BlockPos) {
        val world = mc.player?.world ?: return
        val signEntity = world.getTileEntity(sign)

        if (signEntity is TileEntitySign) {
            posSet.add(sign)
            val packet = signEntity.updatePacket
            val nbt = packet?.nbtCompound

            val textObjects = signEntity.signText
            val signX = sign.x
            val signY = sign.y
            val signZ = sign.z

            var shouldRet = true
            for (text in textObjects) {
                if (text.unformattedText.trim().isNotEmpty())
                    shouldRet = false
            }
            if (shouldRet) return

            val textBuilder = StringBuilder()
            for (text in textObjects) {
                textBuilder.append(text.formattedText.replace("§r", "\n"))
            }
            val textOnSignForChat = textBuilder.toString()

            val old = (oldSigns && nbt != null && OldSigns.isOld("$nbt"))
            val notOld = (oldSigns && nbt != null && !OldSigns.isOld("$nbt"))

            var chatData = when {
                old -> " §8[§cOLD§8]§f:\n§2§o\"${textOnSignForChat.trimEnd().replace("\n", "\n§2")}\""
                notOld && showCoords -> "\n§o\"${textOnSignForChat.trimEnd()}\""
                notOld && !showCoords -> "§o\"${textOnSignForChat.trimEnd()}\""
                showCoords -> "\n§o\"${textOnSignForChat.trimEnd()}\""
                else ->"§o\"${textOnSignForChat.trimEnd()}\""
            }
            if (displayMode == DisplayMode.COMPACT) chatData = chatData.replace("\n", " ")
            val coords = if (showCoords) " [§f${signX}§8, §f${signY}§8, §f${signZ}§8]§f: " else " "

            MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8]$coords§f$chatData")
        }
    }

    private fun signAlreadyLogged(signPos: BlockPos): Boolean {
        return posSet.contains(signPos)
    }

    init {
        onDisable {
            posSet.clear()
        }

        safeListener<ConnectionEvent.Disconnect> {
            posSet.clear()
        }

        safeListener<RenderWorldEvent> {
            if (timer.tick(169) && isEnabled) {
                defaultScope.launch {
                    val surroundings = getSurroundingSigns(player.position)

                    if (surroundings.isNotEmpty()) {
                        for (sign in surroundings) {
                            if (signAlreadyLogged(sign)) continue
                            if (!isDisabled) chatSign(sign)
                        }
                    }
                }
            }
        }
    }
}