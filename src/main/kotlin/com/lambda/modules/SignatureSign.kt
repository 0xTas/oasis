package com.lambda.modules

import java.io.File
import com.lambda.Oasis
import java.time.LocalDate
import com.lambda.Oasis.rCC
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import com.google.common.io.BaseEncoding
import com.lambda.client.module.Category
import java.time.format.DateTimeFormatter
import com.lambda.client.util.FolderUtils
import net.minecraft.util.text.ITextComponent
import com.lambda.client.event.LambdaEventBus
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.util.threads.safeListener
import net.minecraft.util.text.TextComponentString
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.text.MessageSendHelper
import net.minecraft.network.play.client.CPacketUpdateSign


/**
 * @author 0xTas <root@0xTas.dev>
 */
internal object SignatureSign : PluginModule(
    name = "Signature Sign",
    category = Category.MISC,
    description = "AutoFill signs with custom text",
    pluginMain = Oasis
) {
    private val mode by setting("Mode", value = Mode.TEMPLATE)
    private val line1Mode by setting("Line 1 Mode", value = LineMode.CUSTOM, { mode == Mode.TEMPLATE })
    private val line1Text by setting("Line 1 Text", "-=-", { mode == Mode.TEMPLATE })
    private val line2Mode by setting("Line 2 Mode", value = LineMode.USERNAME_WAS_HERE, { mode == Mode.TEMPLATE })
    private val line2Text by setting("Line 2 Text", "", { mode == Mode.TEMPLATE })
    private val line3Mode by setting("Line 3 Mode", value = LineMode.TIMESTAMP, { mode == Mode.TEMPLATE })
    private val line3Text by setting("Line 3 Text", "", { mode == Mode.TEMPLATE })
    private val line4Mode by setting("Line 4 Mode", value = LineMode.CUSTOM, { mode == Mode.TEMPLATE })
    private val line4Text by setting("Line 4 Text", "-=-", { mode == Mode.TEMPLATE })
    private val timestampType by setting("Timestamp Format", value = TimestampType.MMDDYY, { mode == Mode.TEMPLATE })
    private val packetDelay by setting(
        "Packet Delay (ms)",
        value = 2000,
        range = 0..5000,
        step = 1,
        description = "Delay the packet to increase chance of acceptance"
    )
    private val autoDisable by setting("Auto Disable", value = false, description = "Disable after placing a sign")
    private val verbose by setting("Verbose", value = false, description = "Prints confirmation messages in the chat")

    private val mc = Minecraft.getMinecraft()

    private var modified = false

    private enum class Mode {
        TEMPLATE, READ_FROM_FILE
    }

    private enum class LineMode {
        CUSTOM, EMPTY, USERNAME, USERNAME_WAS_HERE, TIMESTAMP,
        OASIS, BASE64, BASE32, ZxHEX, HEX, ROT13, ROT47
    }

    private enum class TimestampType {
        MMDDYY, MMDDYYYY, DDMMYY, DDMMYYYY,
        YYYYMMDD, YYYYDDMM, DAY_MONTH_YEAR, MONTH_DAY_YEAR,
        MONTH_YEAR, YEAR, DAY_MONTH, MONTH_DAY, UNIX_EPOCH
    }

    private fun getSignText(): List<String> {
        val signText = mutableListOf<String>()
        val player = mc.player
        val username = player.name

        when (mode) {
            Mode.READ_FROM_FILE -> {
                val file = File(FolderUtils.lambdaFolder + "sigSign.txt")

                if (file.exists()) {
                    val text = file.readText().replace("§", "").lines()

                    for (i in 0 until text.count()) {
                        if (i >= 4)
                            break
                        signText.add(text[i].take(18))
                    }
                } else {
                    signText.add("File not found.")
                    signText.add("Please create a")
                    signText.add("\"sigSign.txt\" in")
                    signText.add("ur lambda folder")
                    MessageSendHelper.sendChatMessage("§8[${rCC()}☯§8] §4File Not Found. §fPlease add a §2\"sigSign.txt\" §ffile to your Lambda folder.")
                }
            }
            Mode.TEMPLATE -> {
                when (line1Mode) {
                    LineMode.CUSTOM -> signText.add(line1Text)
                    LineMode.EMPTY -> signText.add(" ")
                    LineMode.TIMESTAMP -> signText.add(getTimestamp())
                    LineMode.USERNAME -> signText.add(username)
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here.")
                    LineMode.OASIS -> signText.add("-☯-")
                    LineMode.BASE64 -> signText.add(BaseEncoding.base64().encode(line1Text.toByteArray()))
                    LineMode.BASE32 -> signText.add(BaseEncoding.base32().encode(line1Text.toByteArray()))
                    LineMode.ZxHEX -> signText.add("0x${line1Text.toByteArray().toHex()}")
                    LineMode.HEX -> signText.add(line1Text.toByteArray().toHex())
                    LineMode.ROT13 -> signText.add(rot13(line1Text))
                    LineMode.ROT47 -> signText.add(rot47(line1Text))
                }
                when (line2Mode) {
                    LineMode.CUSTOM -> signText.add(line2Text)
                    LineMode.EMPTY -> signText.add(" ")
                    LineMode.TIMESTAMP -> signText.add(getTimestamp())
                    LineMode.USERNAME -> signText.add(username)
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here.")
                    LineMode.OASIS -> signText.add("0x4f61736973")
                    LineMode.BASE64 -> signText.add(BaseEncoding.base64().encode(line2Text.toByteArray()))
                    LineMode.BASE32 -> signText.add(BaseEncoding.base32().encode(line2Text.toByteArray()))
                    LineMode.ZxHEX -> signText.add("0x${line2Text.toByteArray().toHex()}")
                    LineMode.HEX -> signText.add(line2Text.toByteArray().toHex())
                    LineMode.ROT13 -> signText.add(rot13(line2Text))
                    LineMode.ROT47 -> signText.add(rot47(line2Text))
                }
                when (line3Mode) {
                    LineMode.CUSTOM -> signText.add(line3Text)
                    LineMode.EMPTY -> signText.add(" ")
                    LineMode.TIMESTAMP -> signText.add(getTimestamp())
                    LineMode.USERNAME -> signText.add(username)
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here.")
                    LineMode.OASIS -> signText.add("${System.currentTimeMillis() / 1000} UTC")
                    LineMode.BASE64 -> signText.add(BaseEncoding.base64().encode(line3Text.toByteArray()))
                    LineMode.BASE32 -> signText.add(BaseEncoding.base32().encode(line3Text.toByteArray()))
                    LineMode.ZxHEX -> signText.add("0x${line3Text.toByteArray().toHex()}")
                    LineMode.HEX -> signText.add(line3Text.toByteArray().toHex())
                    LineMode.ROT13 -> signText.add(rot13(line3Text))
                    LineMode.ROT47 -> signText.add(rot47(line3Text))
                }
                when (line4Mode) {
                    LineMode.CUSTOM -> signText.add(line4Text)
                    LineMode.EMPTY -> signText.add(" ")
                    LineMode.TIMESTAMP -> signText.add(getTimestamp())
                    LineMode.USERNAME -> signText.add(username)
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here.")
                    LineMode.OASIS -> signText.add("-☯-")
                    LineMode.BASE64 -> signText.add(BaseEncoding.base64().encode(line4Text.toByteArray()))
                    LineMode.BASE32 -> signText.add(BaseEncoding.base32().encode(line4Text.toByteArray()))
                    LineMode.ZxHEX -> signText.add("0x${line4Text.toByteArray().toHex()}")
                    LineMode.HEX -> signText.add(line4Text.toByteArray().toHex())
                    LineMode.ROT13 -> signText.add(rot13(line4Text))
                    LineMode.ROT47 -> signText.add(rot47(line4Text))
                }
            }
        }

        return signText
    }

    private fun getSignTextComponents(): Array<out ITextComponent> {
        val lines = getSignText()
        val componentLines = mutableListOf<TextComponentString>()

        for (line in lines) {
            componentLines.add(TextComponentString(line))
        }

        return componentLines.toTypedArray()
    }

    private fun getTimestamp(): String {
        val currentDate = LocalDate.now()
        val mmddyy = DateTimeFormatter.ofPattern("MM/dd/yy")
        val mmddyyyy = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val ddmmyy = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val ddmmyyyy = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val yyyymmdd = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val yyyyddmm = DateTimeFormatter.ofPattern("yyyy/dd/MM")

        val currentMonth = (currentDate.month.name.substring(0, 1)
            + currentDate.month.name.substring(1 until currentDate.month.name.count()).lowercase())

        return when (timestampType) {
            TimestampType.MMDDYY -> currentDate.format(mmddyy)
            TimestampType.MMDDYYYY -> currentDate.format(mmddyyyy)
            TimestampType.DDMMYY -> currentDate.format(ddmmyy)
            TimestampType.DDMMYYYY -> currentDate.format(ddmmyyyy)
            TimestampType.YYYYMMDD -> currentDate.format(yyyymmdd)
            TimestampType.YYYYDDMM -> currentDate.format(yyyyddmm)
            TimestampType.DAY_MONTH_YEAR -> "${currentDate.dayOfMonth} $currentMonth ${currentDate.year}"
            TimestampType.MONTH_DAY_YEAR -> "$currentMonth ${currentDate.dayOfMonth} ${currentDate.year}"
            TimestampType.MONTH_YEAR -> "$currentMonth ${currentDate.year}"
            TimestampType.YEAR -> currentDate.year.toString()
            TimestampType.DAY_MONTH -> "${dayOfMonthSuffix(currentDate.dayOfMonth)} of $currentMonth."
            TimestampType.MONTH_DAY -> "$currentMonth ${dayOfMonthSuffix(currentDate.dayOfMonth)}."
            TimestampType.UNIX_EPOCH -> "${System.currentTimeMillis() / 1000} UTC"
        }
    }

    private fun dayOfMonthSuffix(dom: Int): String {
        return if (!dom.toString().endsWith("11") && dom.toString().endsWith("1")) {
            "${dom}st"
        }else if (!dom.toString().endsWith("12") && dom.toString().endsWith("2")) {
            "${dom}nd"
        }else if (!dom.toString().endsWith("13") && dom.toString().endsWith("3")) {
            "${dom}rd"
        }else {
            "${dom}th"
        }
    }

    private fun rot13(input: String): String {
        val inputArr = input.toCharArray()
        for (i in inputArr.indices) {
            var c = inputArr[i].code
            if (c in 'a'.code..'z'.code) {
                c += 13
                if (c > 'z'.code) {
                    c -= 26
                }
            } else if (c in 'A'.code..'Z'.code) {
                c += 13
                if (c > 'Z'.code) {
                    c -= 26
                }
            }
            inputArr[i] = c.toChar()
        }
        return String(inputArr)
    }

    private fun rot47(input: String): String {
        val inputArr = input.toCharArray()
        for (i in inputArr.indices) {
            var c = inputArr[i].code
            if (c in '!'.code..'~'.code) {
                c += 47
                if (c > '~'.code) {
                    c -= 94
                }
            }
            inputArr[i] = c.toChar()
        }
        return String(inputArr)
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    init {
        safeListener<PacketEvent.Send> { packet ->
            if (isDisabled || packet.packet !is CPacketUpdateSign) return@safeListener

            val signText = getSignTextComponents()

            if (!modified) {
                defaultScope.launch {
                    delay(packetDelay.toLong())
                    player.connection.sendPacket(CPacketUpdateSign((packet.packet as CPacketUpdateSign).position, signText))
                }
                packet.cancel()
                modified = true
            } else {
                modified = false
                return@safeListener
            }

            if (mode == Mode.TEMPLATE && verbose)
                MessageSendHelper.sendChatMessage("§8[${rCC()}☯§8] §fSending templated §2CPacketUpdateSign §fto the server.")

            if (mode == Mode.READ_FROM_FILE && verbose)
                MessageSendHelper.sendChatMessage("§8[${rCC()}☯§8] §fRead from file into outbound §2CPacketUpdateSign §fpacket.")

            if (autoDisable)
                disable()
        }
    }

    init {
        LambdaEventBus.subscribe(this)
    }
}