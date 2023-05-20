package com.lambda.huds

import com.lambda.Oasis
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.plugin.api.PluginLabelHud

internal object NotepadHud : PluginLabelHud(
    name = "Notepad",
    category = Category.MISC,
    description = "Write custom text to the HUD",
    pluginMain = Oasis
) {
    private val newLines by setting("Newlines", value = true, description = "Separate notes on new lines")
    private val bulletPoints by setting("Bullet Points", value = true, description = "Begin each line with a bullet point")
    private val notepadText by setting("Note 1", "Hello")
    private val textColor by setting("Color 1", ColorHolder(84, 166, 255, 255))
    private val notepadText1 by setting("Note 2", "World")
    private val textColor1 by setting("Color 2", ColorHolder(255, 84, 230, 255))
    private val notepadText2 by setting("Note 3", "")
    private val textColor2 by setting("Color 3", ColorHolder(255, 255, 255, 255))
    private val notepadText3 by setting("Note 4", "")
    private val textColor3 by setting("Color 4", ColorHolder(255, 255, 255, 255))
    private val notepadText4 by setting("Note 5", "")
    private val textColor4 by setting("Color 5", ColorHolder(255, 255, 255, 255))
    private val notepadText5 by setting("Note 6", "")
    private val textColor5 by setting("Color 6", ColorHolder(255, 255, 255, 255))
    private val notepadText6 by setting("Note 7", "")
    private val textColor6 by setting("Color 7", ColorHolder(255, 255, 255, 255))
    private val notepadText7 by setting("Note 8", "")
    private val textColor7 by setting("Color 8", ColorHolder(255, 255, 255, 255))
    private val notepadText8 by setting("Note 9", "")
    private val textColor8 by setting("Color 9", ColorHolder(255, 255, 255, 255))
    private val notepadText9 by setting("Note 10", "")
    private val textColor9 by setting("Color 10", ColorHolder(255, 255, 255, 255))

    override fun SafeClientEvent.updateText() {
        if (newLines && !bulletPoints) {
            displayText.addLine(notepadText, textColor)
            displayText.addLine(notepadText1, textColor1)
            displayText.addLine(notepadText2, textColor2)
            displayText.addLine(notepadText3, textColor3)
            displayText.addLine(notepadText4, textColor4)
            displayText.addLine(notepadText5, textColor5)
            displayText.addLine(notepadText6, textColor6)
            displayText.addLine(notepadText7, textColor7)
            displayText.addLine(notepadText8, textColor8)
            displayText.addLine(notepadText9, textColor9)
        } else if (newLines && bulletPoints) {
            displayText.addLine("• $notepadText", textColor)
            displayText.addLine("• $notepadText1", textColor1)
            displayText.addLine("• $notepadText2", textColor2)
            displayText.addLine("• $notepadText3", textColor3)
            displayText.addLine("• $notepadText4", textColor4)
            displayText.addLine("• $notepadText5", textColor5)
            displayText.addLine("• $notepadText6", textColor6)
            displayText.addLine("• $notepadText7", textColor7)
            displayText.addLine("• $notepadText8", textColor8)
            displayText.addLine("• $notepadText9", textColor9)
        }else if (!newLines && bulletPoints) {
            displayText.add("• $notepadText", textColor)
            displayText.add(notepadText1, textColor1)
            displayText.add(notepadText2, textColor2)
            displayText.add(notepadText3, textColor3)
            displayText.add(notepadText4, textColor4)
            displayText.add(notepadText5, textColor5)
            displayText.add(notepadText6, textColor6)
            displayText.add(notepadText7, textColor7)
            displayText.add(notepadText8, textColor8)
            displayText.add(notepadText9, textColor9)
        } else if (!newLines && !bulletPoints) {
            displayText.add(notepadText, textColor)
            displayText.add(notepadText1, textColor1)
            displayText.add(notepadText2, textColor2)
            displayText.add(notepadText3, textColor3)
            displayText.add(notepadText4, textColor4)
            displayText.add(notepadText5, textColor5)
            displayText.add(notepadText6, textColor6)
            displayText.add(notepadText7, textColor7)
            displayText.add(notepadText8, textColor8)
            displayText.add(notepadText9, textColor9)
        }
    }
}