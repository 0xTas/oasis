package dev.oasis

import java.util.*
import dev.oasis.modules.*
import dev.oasis.commands.*
import dev.oasis.huds.NotepadHud
import com.lambda.client.plugin.api.Plugin


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
internal object Oasis : Plugin() {
    override fun onLoad() {
        modules.add(FarmAura)
        modules.add(OldSigns)
        modules.add(AutoDoor)
        modules.add(TunnelESP)
        commands.add(LastSeen)
        commands.add(SegFault)
        modules.add(ChatSigns)
        modules.add(BannerInfo)
        modules.add(EntityJesus)
        modules.add(SignatureSign)
        modules.add(UpdateNotifier)
        hudElements.add(NotepadHud)

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