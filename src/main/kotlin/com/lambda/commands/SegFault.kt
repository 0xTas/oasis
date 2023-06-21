package com.lambda.commands

import java.util.*
import sun.misc.Unsafe
import com.lambda.Oasis
import com.lambda.client.command.ClientCommand
import com.lambda.client.util.text.MessageSendHelper


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
object SegFault : ClientCommand(
    name = "segfault",
    alias = arrayOf("crash", "hcf"),
    description = "Crashes your game :3"
) {
    private object SafetyBypass {
        fun bypassSafety(): Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            .apply { isAccessible = true }
            .let { it.get(null) as Unsafe }
    }

    private val byebye = SafetyBypass.bypassSafety()

    init {
        execute("Crashes your game :3") {
            for (i in 0 until 42)
                MessageSendHelper.sendChatMessage("§8[${Oasis.rCC()}☯§8] §4Goodbye §2Friend§f.")
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val segFault = byebye.allocateMemory(69)
                    byebye.putAddress(69, 420)
                    print(byebye.getAddress(segFault))
                }
            }, 420)

        }
    }
}