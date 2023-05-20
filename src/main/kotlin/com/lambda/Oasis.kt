package com.lambda

import java.util.*
import com.lambda.modules.*
import com.lambda.huds.NotepadHud
import com.lambda.commands.SegFault
import com.lambda.modules.SignatureSign
import com.lambda.client.plugin.api.Plugin


internal object Oasis : Plugin() {
    override fun onLoad() {
        commands.add(SegFault)
        modules.add(ChatSigns)
        modules.add(EntityJesus)
        modules.add(SignatureSign)
        hudElements.add(NotepadHud)

        bgJobs.add(ChatSigns.job)
        bgJobs.add(EntityJesus.job)
    }

    // Random Color Code
    fun rCC(): String {
        val colorCodes = listOf(
            "§4", "§c", "§6", "§e", "§2",
            "§a", "§b", "§3", "§1", "§9",
            "§d", "§5", "§f", "§7", "§8", "§0"
        )
        val rng = Random()

        return colorCodes[rng.nextInt(colorCodes.size)]
    }
}