package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.*

object DebugOnlineConfig : ReadOnlyPluginConfig("DebugOnlineConfig") {
    @ValueDescription("逐个发送消息延时，单位秒")
    val duration by value(10)
}