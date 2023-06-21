package com.lambda.modules

import java.io.File
import com.lambda.Oasis
import java.time.LocalDate
import com.lambda.Oasis.rCC
import com.google.common.io.BaseEncoding
import com.lambda.client.module.Category
import java.time.format.DateTimeFormatter
import com.lambda.client.util.FolderUtils
import net.minecraft.util.text.ITextComponent
import com.lambda.client.plugin.api.PluginModule
import net.minecraft.util.text.TextComponentString
import com.lambda.client.util.text.MessageSendHelper


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object SignatureSign : PluginModule(
    name = "Signature Sign",
    category = Category.MISC,
    description = "AutoFill signs with custom text",
    pluginMain = Oasis
) {
    private val mode by setting("Mode", value = Mode.TEMPLATE)
    private val line1Mode by setting("Line 1 Mode", value = LineMode.CUSTOM, { mode == Mode.TEMPLATE })
    private val line1Text by setting("Line 1 Text", "-=-", { mode == Mode.TEMPLATE &&  lineTextVisibility(1) })
    private val line2Mode by setting("Line 2 Mode", value = LineMode.USERNAME_WAS_HERE, { mode == Mode.TEMPLATE })
    private val line2Text by setting("Line 2 Text", "", { mode == Mode.TEMPLATE  &&  lineTextVisibility(2)})
    private val line3Mode by setting("Line 3 Mode", value = LineMode.TIMESTAMP, { mode == Mode.TEMPLATE })
    private val line3Text by setting("Line 3 Text", "", { mode == Mode.TEMPLATE  &&  lineTextVisibility(3)})
    private val line4Mode by setting("Line 4 Mode", value = LineMode.CUSTOM, { mode == Mode.TEMPLATE })
    private val line4Text by setting("Line 4 Text", "-=-", { mode == Mode.TEMPLATE  &&  lineTextVisibility(4)})
    private val timestampType by setting("Timestamp Format", value = TimestampType.MMDDYY, { mode == Mode.TEMPLATE && timeVisibility()})
    private val autoDisable by setting("Auto Disable", value = false, description = "Disable after placing a sign")

    @Suppress("unused")
    private val openLambdaFolder by setting("Open Lambda Folder...", false, {mode == Mode.READ_FROM_FILE},
        consumer = { _, _ ->
            FolderUtils.openFolder(FolderUtils.lambdaFolder)
            false
        }, description = "Opens the folder where autosign.txt should go")

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


    // See MixinGuiEditSign.java
    fun getSignature(): Array<out ITextComponent> {
        return getSignTextComponents()
    }

    fun needsDisabling(): Boolean {
        return autoDisable
    }

    private fun timeVisibility(): Boolean {
        return line1Mode == LineMode.TIMESTAMP || line2Mode == LineMode.TIMESTAMP
            || line3Mode == LineMode.TIMESTAMP || line4Mode == LineMode.TIMESTAMP
    }

    private fun lineTextVisibility(line: Int): Boolean {
        val modeCon = mode == Mode.TEMPLATE
        when (line) {
            1 -> {
                return modeCon && line1Mode != LineMode.EMPTY && line1Mode != LineMode.USERNAME
                    && line1Mode != LineMode.USERNAME_WAS_HERE && line1Mode != LineMode.TIMESTAMP
                    && line1Mode != LineMode.OASIS
            }
            2 -> {
                return modeCon && line2Mode != LineMode.EMPTY && line2Mode != LineMode.USERNAME
                    && line2Mode != LineMode.USERNAME_WAS_HERE && line2Mode != LineMode.TIMESTAMP
                    && line2Mode != LineMode.OASIS
            }
            3 -> {
                return modeCon && line3Mode != LineMode.EMPTY && line3Mode != LineMode.USERNAME
                    && line3Mode != LineMode.USERNAME_WAS_HERE && line3Mode != LineMode.TIMESTAMP
                    && line3Mode != LineMode.OASIS
            }
            4 -> {
                return modeCon && line4Mode != LineMode.EMPTY && line4Mode != LineMode.USERNAME
                    && line4Mode != LineMode.USERNAME_WAS_HERE && line4Mode != LineMode.TIMESTAMP
                    && line4Mode != LineMode.OASIS
            }
            else -> return true
        }
    }

    private fun getSignText(): List<String> {
        val signText = mutableListOf<String>()
        val player = mc.player
        val username = player?.name ?: return signText

        when (mode) {
            Mode.READ_FROM_FILE -> {
                val file = File(FolderUtils.lambdaFolder + "autosign.txt")

                if (file.exists()) {
                    val text = file.readText().replace("§", "").lines()
                    for (i in 0 until text.count()) {
                        if (i >= 4) break
                        signText.add(text[i])
                    }
                } else {
                    signText.add("File not found.")
                    signText.add("Please create an")
                    signText.add("\"autosign.txt\" in")
                    signText.add("/lambda folder")
                    MessageSendHelper.sendChatMessage("§8[${rCC()}☯§8] §4File Not Found. §fPlease add a §2\"autosign.txt\" §ffile to your Lambda folder.")
                }
            }
            Mode.TEMPLATE -> {
                when (line1Mode) {
                    LineMode.CUSTOM -> signText.add(line1Text)
                    LineMode.EMPTY -> signText.add(" ")
                    LineMode.TIMESTAMP -> signText.add(getTimestamp())
                    LineMode.USERNAME -> signText.add(username)
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here")
                    LineMode.OASIS -> signText.add("<☯>")
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
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here")
                    LineMode.OASIS -> signText.add("<$username>")
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
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here")
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
                    LineMode.USERNAME_WAS_HERE -> signText.add("$username was here")
                    LineMode.OASIS -> signText.add("<☯>")
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
            TimestampType.DAY_MONTH_YEAR -> "${dayOfMonthSuffix(currentDate.dayOfMonth)} $currentMonth ${currentDate.year}"
            TimestampType.MONTH_DAY_YEAR -> "$currentMonth ${dayOfMonthSuffix(currentDate.dayOfMonth)} ${currentDate.year}"
            TimestampType.MONTH_YEAR -> "$currentMonth ${currentDate.year}"
            TimestampType.YEAR -> currentDate.year.toString()
            TimestampType.DAY_MONTH -> "${dayOfMonthSuffix(currentDate.dayOfMonth)} of $currentMonth"
            TimestampType.MONTH_DAY -> "$currentMonth ${dayOfMonthSuffix(currentDate.dayOfMonth)}"
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
}