package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value

object DebugOnlineConfig : ReadOnlyPluginConfig("DebugOnlineConfig") {
    val exclude by value(listOf(12345L))
}