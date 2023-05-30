package com.lambda.modules

import com.lambda.Oasis
import kotlinx.coroutines.launch
import net.minecraft.block.BlockSign
import net.minecraft.util.math.BlockPos
import com.lambda.client.module.Category
import com.lambda.client.event.SafeClientEvent
import net.minecraft.tileentity.TileEntitySign
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.event.events.ConnectionEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent


/**
 * @author 0xTas <root@0xTas.dev>
 */
internal object ChatSigns : PluginModule(
    name = "ChatSigns",
    description = "Read nearby signs in your chat",
    category = Category.CHAT,
    pluginMain = Oasis
){
    private val posSet = hashSetOf<BlockPos>()
    private val showCoords by setting("Show Coordinates", value = true, description = "Include sign coordinates")
    private val oldSigns by setting("Show Old Status", value = true , description = "Annotate signs placed prior to Minecraft version 1.8")
    private val displayMode by setting("Display Mode", value = DisplayMode.FANCY, description = "Toggle between fancy/compact chat")
    private val tickRate by setting(
        "Tick Rate",
        value = 2,
        range = 2..10,
        step = 2,
        description = "Increase if needed"
    )

    private var ticksEnabled = 0

    private enum class DisplayMode {
        FANCY, COMPACT
    }


    private fun SafeClientEvent.getSurroundingSigns(playerPos: BlockPos): HashSet<BlockPos> {
        val signList = HashSet<BlockPos>()
        val world = mc.player?.world ?: return signList

        val startX = playerPos.x - 112
        val startY = 0
        val startZ = playerPos.z - 112
        val endX = playerPos.x + 112
        val endY = world.height
        val endZ = playerPos.z + 112

        for (x in startX..endX) {
            for (y in startY..endY) {
                for (z in startZ..endZ) {
                    val pos = BlockPos(x, y, z)

                    if (world.getBlockState(pos).block is BlockSign) {
                        signList.add(pos)
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

            var textOnSignForChat = ""
            for (text in textObjects) {
                textOnSignForChat += text.formattedText.replace("§r", "\n")
            }
            val old = (oldSigns && nbt != null && isOld("$nbt"))
            val notOld = (oldSigns && nbt != null && !isOld("$nbt"))
            var chatData = if (old) {
                " §8[§cOLD§8]§f:\n§2§o\"${textOnSignForChat.trimEnd().replace("\n", "\n§2")}\""
            } else if (notOld && showCoords){
                "\n§o\"${textOnSignForChat.trimEnd()}\""
            }else if (notOld && !showCoords) {
                "§o\"${textOnSignForChat.trimEnd()}\""
            }else if (showCoords){
                "\n§o\"${textOnSignForChat.trimEnd()}\""
            } else {
                " §o\"${textOnSignForChat.trimEnd()}\""
            }

            if (displayMode == DisplayMode.COMPACT) chatData = chatData.replace("\n", " ")
            if (showCoords) {
                MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] [§f${signX}, ${signY}, ${signZ}§8]§f:$chatData")
            } else {
                MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §f$chatData")
            }
        }
    }

    private fun signAlreadyLogged(signPos: BlockPos): Boolean {
        return posSet.contains(signPos)
    }

    private fun isOld(metadata: String): Boolean {
        return !metadata.contains("""{\"extra\":[{\"text\":""")
    }

    init {
        onDisable {
            posSet.clear()
            ticksEnabled = 0
        }

        safeListener<ConnectionEvent.Disconnect> {
            posSet.clear()
            ticksEnabled = 0
        }

        safeListener<ClientTickEvent> {
            if (it.phase != TickEvent.Phase.START) return@safeListener
            ticksEnabled++

            if (ticksEnabled >= 65535) ticksEnabled = 0
            if (ticksEnabled % tickRate == 0) {
                val player = mc.player ?: return@safeListener
                defaultScope.launch {
                    val surroundings = getSurroundingSigns(player.position)

                    if (surroundings.isNotEmpty()) {
                        for (sign in surroundings) {
                            if (signAlreadyLogged(sign))
                                continue
                            if (!isDisabled) chatSign(sign)
                        }
                    }
                }
            }
        }
    }
}