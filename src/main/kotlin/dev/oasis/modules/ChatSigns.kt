package dev.oasis.modules

import dev.oasis.Oasis
import java.time.Instant
import java.time.Duration
import kotlinx.coroutines.launch
import net.minecraft.block.BlockSign
import net.minecraft.util.math.BlockPos
import com.lambda.client.util.TickTimer
import com.lambda.client.module.Category
import java.util.concurrent.ConcurrentHashMap
import com.lambda.client.event.SafeClientEvent
import net.minecraft.tileentity.TileEntitySign
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.event.events.ConnectionEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.util.math.VectorUtils.toBlockPos


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object ChatSigns : PluginModule(
    name = "ChatSigns",
    description = "Read nearby signs in your chat",
    category = Category.CHAT,
    pluginMain = Oasis
){
    private val posSet = hashSetOf<BlockPos>()
    private val chatMode by setting("Chat Mode", value = ChatMode.ESP)
    private val chunks by setting(
        "Chunks", value = 5, range = 1..12, step = 1, description = "Range of chunks to scan in (5 is 2b max)")
    private val showCoords by setting("Show Coordinates", value = true, description = "Include sign coordinates")
    private val oldSigns by setting(
        "Show Old Status", value = true , description = "Annotate signs placed prior to Minecraft version 1.8")
    private val oldColor by setting(
        "Old Text Color", value = TextColor.YELLOW, description = "Text color for old signs")
    private val chatColor by setting(
        "Chat Text Color", value = TextColor.GRAY, description = "Text color for all other signs")
    private val displayMode by setting(
        "Display Mode", value = DisplayMode.FANCY, description = "Toggle between fancy/compact chat")
    private val repeatMode by setting("Repeat Mode", value = RepeatMode.COOLDOWN, { chatMode != ChatMode.ESP })
    private val repeatCD by setting(
        "Cooldown", value = 60, range = 0..500, step = 1,
        { chatMode != ChatMode.ESP && repeatMode == RepeatMode.COOLDOWN }, description = "(seconds)"
    )

    private val timer = TickTimer()
    private val coolingDown = ConcurrentHashMap<BlockPos, Instant>()

    private var lastFocused: BlockPos? = null

    private enum class ChatMode {
        ESP, TARGETED, BOTH
    }
    private enum class RepeatMode {
        IMMEDIATE, COOLDOWN
    }
    private enum class DisplayMode {
        FANCY, COMPACT
    }

    private enum class TextColor {
        RED, DARK_RED, GREEN, DARK_GREEN,
        BLUE, DARK_BLUE, AQUA,
        PURPLE, DARK_PURPLE, GOLD, YELLOW,
        GRAY, DARK_GRAY, BLACK, WHITE
    }


    private fun color(old: Boolean): String {
        if (old)
            return when (oldColor) {
                TextColor.RED -> "§c"
                TextColor.DARK_RED -> "§4"
                TextColor.GREEN -> "§a"
                TextColor.DARK_GREEN -> "§2"
                TextColor.BLUE -> "§9"
                TextColor.DARK_BLUE -> "§1"
                TextColor.AQUA -> "§3"
                TextColor.PURPLE -> "§d"
                TextColor.DARK_PURPLE -> "§5"
                TextColor.GOLD -> "§6"
                TextColor.YELLOW -> "§e"
                TextColor.GRAY -> "§7"
                TextColor.DARK_GRAY -> "§8"
                TextColor.BLACK -> "§0"
                TextColor.WHITE -> "§f"
            }

        return when (chatColor) {
            TextColor.RED -> "§c"
            TextColor.DARK_RED -> "§4"
            TextColor.GREEN -> "§a"
            TextColor.DARK_GREEN -> "§2"
            TextColor.BLUE -> "§9"
            TextColor.DARK_BLUE -> "§1"
            TextColor.AQUA -> "§3"
            TextColor.PURPLE -> "§d"
            TextColor.DARK_PURPLE -> "§5"
            TextColor.GOLD -> "§6"
            TextColor.YELLOW -> "§e"
            TextColor.GRAY -> "§7"
            TextColor.DARK_GRAY -> "§8"
            TextColor.BLACK -> "§0"
            TextColor.WHITE -> "§f"
        }
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
        val signEntity = world.getTileEntity(sign)

        if (signEntity is TileEntitySign) {
            posSet.add(sign)
            val packet = signEntity.updatePacket
            val nbt = packet?.nbtCompound

            val textObjects = signEntity.signText
            val signX = sign.x
            val signY = sign.y
            val signZ = sign.z

            if (textObjects.none {
                    it.unformattedText.trim().isNotEmpty()
                }) return

            val textBuilder = StringBuilder()
            for (text in textObjects) {
                textBuilder.append(text.formattedText.replace("§r", "\n"))
            }
            val textOnSignForChat = textBuilder.toString()

            val old = (oldSigns && nbt != null && OldSigns.isOld("$nbt"))
            var chatData = when {
                old -> " §8[§cOLD§8]§f:\n${color(true)}§o\"${textOnSignForChat.trimEnd().replace("\n", "\n${color(true)}")}\""
                else -> "\n§o${color(false)}\"${textOnSignForChat.trimEnd().replace("\n", "\n${color(false)}")}\""
            }
            if (displayMode == DisplayMode.COMPACT) chatData = chatData.replace("\n", " ")
            val coords = if (showCoords) " [§f${signX}§8, §f${signY}§8, §f${signZ}§8]§f: " else " "

            MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8]$coords§f$chatData")
        }
    }

    private fun signAlreadyLogged(signPos: BlockPos): Boolean {
        return posSet.contains(signPos)
    }

    private fun SafeClientEvent.getTargetedSign(): BlockPos? {
        val pv = mc.renderViewEntity ?: return null

        val maxDist = (chunks * 16).toDouble()
        val trace = pv.rayTrace(maxDist, 0.0f)
        val eyePos = pv.getPositionEyes(0.0f)

        var dist = maxDist
        if (trace != null) dist = trace.hitVec.distanceTo(eyePos)

        val look = pv.getLook(0.0f)
        val offsetVec = eyePos.add(look.x * dist, look.y * dist, look.z * dist)

        val lookingAtPos = offsetVec.toBlockPos()
        val tileEntity = world.getTileEntity(lookingAtPos) ?: return null

        if (tileEntity is TileEntitySign) return lookingAtPos

        return null
    }

    init {
        onDisable {
            posSet.clear()
            coolingDown.clear()
        }

        safeListener<ConnectionEvent.Disconnect> {
            posSet.clear()
            coolingDown.clear()
        }

        safeListener<RenderWorldEvent> {
            if (timer.tick(169) && isEnabled) {
                defaultScope.launch {
                    if (chatMode != ChatMode.TARGETED) {
                        val surroundings = getSurroundingSigns(player.position)

                        if (surroundings.isNotEmpty()) {
                            for (sign in surroundings) {
                                if (signAlreadyLogged(sign)) continue
                                if (!isDisabled) chatSign(sign)
                            }
                        }
                    }

                    if (chatMode == ChatMode.TARGETED || chatMode == ChatMode.BOTH) {
                        val focusedSign = getTargetedSign()
                        if (focusedSign == null) {
                            lastFocused = null
                            return@launch
                        }

                        if (lastFocused == null) lastFocused = focusedSign
                        else if (lastFocused == focusedSign && repeatMode == RepeatMode.IMMEDIATE) return@launch

                        if (repeatMode == RepeatMode.COOLDOWN) {
                            if (coolingDown.containsKey(focusedSign)) {
                                val stamp = coolingDown[focusedSign]!!
                                val now = Instant.now()

                                if (Duration.between(stamp, now).toMillis() > repeatCD * 1000) {
                                    if (!isDisabled) chatSign(focusedSign)
                                    coolingDown.replace(focusedSign, stamp, now)
                                }
                            } else {
                                if (!isDisabled) chatSign(focusedSign)
                                coolingDown[focusedSign] = Instant.now()
                            }
                        } else if (!isDisabled) chatSign(focusedSign)
                        lastFocused = focusedSign
                    }
                }
            }
        }
    }
}