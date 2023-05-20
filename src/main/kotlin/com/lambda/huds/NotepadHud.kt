package com.lambda.huds

import com.lambda.Oasis
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.plugin.api.PluginLabelHud

internal object NotepadHud : PluginLabelHud(
    name = "Notepad",
    category = Category.MISC,
    description = "Write custom text to the HUD.",
    pluginMain = Oasis
) {
    private val notepadText1 by setting("Note 1", "Hello World")
    private val textColor1 by setting("Color 1", ColorHolder(255, 255, 255, 255))
    private val notepadText2 by setting("Note 2", "")
    private val textColor2 by setting("Color 2", ColorHolder(255, 255, 255, 255))

    override fun SafeClientEvent.updateText() {
        displayText.add(notepadText1, textColor1)
        displayText.addLine(notepadText2, textColor2)
    }
}