package com.lambda.modules

import java.net.URL
import com.lambda.Oasis
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import net.minecraft.util.text.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lambda.client.util.TickTimer
import com.lambda.client.module.Category
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import net.minecraft.util.text.TextComponentString
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.event.events.RenderWorldEvent


/**
 * @author 0xTas [@0xTas] <root@0xTas.dev>
 */
internal object UpdateNotifier: PluginModule(
    name = "OasisUpdates",
    description = "Notifies you in chat when a new version of Oasis is available on Github",
    pluginMain = Oasis,
    showOnArray = false,
    enabledByDefault = true,
    category = Category.CLIENT,
) {
    private var notified = false
    private val timer = TickTimer()
    private val version = Oasis.version

    private const val releaseURL = "https://github.com/0xTas/oasis/releases/latest/"

    private fun extractNewVersionNumber(url: String): String {
        val vIndex = url.lastIndexOf('v')
        if (vIndex == -1 || vIndex >= url.length - 1) return ""

        return url.substring(vIndex+1)
    }

    init {
        safeListener<RenderWorldEvent> {
            if (notified || isDisabled) return@safeListener

            if (timer.tick(4269)) {
                defaultScope.launch {
                    val req = withContext(Dispatchers.IO) {
                        URL(releaseURL).openConnection()
                    } as HttpURLConnection
                    req.instanceFollowRedirects = false
                    withContext(Dispatchers.IO) {
                        req.connect()
                    }

                    val resURL = req.getHeaderField("Location")
                    if (!resURL.endsWith(version)) {
                        val newVersion = extractNewVersionNumber(resURL)
                        val linkText = TextComponentString("§8[${Oasis.rCC()}☯§8] §7Click §a§ohere §7to open the Github page§f.")
                        val style = Style().setClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, releaseURL))
                        style.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponentString(releaseURL))

                        linkText.style = style
                        MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §7A new version of §2O§4a§3s§ei§5s §8[§fv§6$newVersion§8] §7is available§f!")
                        MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §7(You have §fv§c$version§7)§f.")
                        MessageSendHelper.sendChatMessage(linkText)
                    }
                    notified = true
                }
            }
        }
    }
}