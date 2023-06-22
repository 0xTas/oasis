package com.lambda.commands

import java.net.URL
import com.lambda.Oasis
import java.time.Instant
import java.time.Duration
import java.util.TimeZone
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import com.lambda.client.LambdaMod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lambda.client.command.ClientCommand
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.text.MessageSendHelper


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 * credits to https://2b2t.dev for the api
 */
object LastSeen : ClientCommand(
    name = "lastseen",
    alias = arrayOf("ls", "seen"),
    description = "Checks the last seen status of a 2b2t player"
) {
    private var lastUsed: Instant? = null
    private const val apiURL = "https://api.2b2t.dev/seen?username="

    init {
        string("player name") { nameArg ->
            execute("Search a player's last seen status") {
                val playerName = nameArg.value.replace(" ", "")

                if (playerName.isEmpty()) {
                    MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4Name cannot be empty.")
                    return@execute
                }

                if (lastUsed != null) {
                    val now = Instant.now()
                    if (Duration.between(lastUsed, now).toMillis() < 3000) {
                        MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4You are being rate-limited.\n§ePlease wait a few seconds before trying again.")
                        return@execute
                    }
                }

                defaultScope.launch {
                    val reqURL = apiURL+playerName
                    val req: HttpURLConnection
                    val res: String
                    try {
                        req = withContext(Dispatchers.IO) {
                            URL(reqURL).openConnection()
                        } as HttpURLConnection

                        req.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        req.setRequestProperty("User-Agent", "oasis-lambda-plugin-github.com/0xTas/oasis")

                        withContext(Dispatchers.IO) {
                            res = req.inputStream.readBytes().toString(Charsets.UTF_8)
                        }
                    } catch (e: Exception) {
                        MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4An error occurred. \nTry again later or check latest.log for more info.")
                        LambdaMod.LOG.warn("[oasis] seen cmd failed to make a connection - $e")
                        return@launch
                    }

                    try {
                        val seenJson = JsonParser().parse(res)
                        if (seenJson.asJsonArray.size() > 0) {
                            val lastSeen = seenJson.asJsonArray[0].asJsonObject["seen"].asString

                            val lastSeenFormat = "yyyy-MM-dd HH:mm:ss"
                            val wantedFormat = "MMMM dd yyyy, HH:mm:ss"

                            val lastSeenTZ = TimeZone.getTimeZone("EDT")
                            val localTZ = TimeZone.getDefault()

                            val inputFormatter = SimpleDateFormat(lastSeenFormat)
                            inputFormatter.timeZone = lastSeenTZ

                            val outputFormatter = SimpleDateFormat(wantedFormat)
                            outputFormatter.timeZone = localTZ

                            val date = inputFormatter.parse(lastSeen)
                            val output = outputFormatter.format(date)

                            val seen = output.replace(", ", ", §fat §2")
                            MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §2$playerName §fwas last seen on §2$seen.")
                        } else {
                            MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4That player has not been seen..")
                        }
                    } catch (e: Exception) {
                        MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4An error occurred. §fSee §2latest.log §ffor more info.")
                        LambdaMod.LOG.warn("[oasis] seen cmd failed to parse api response - $e")
                    }
                    lastUsed = Instant.now()
                }
            }
        }
    }
}