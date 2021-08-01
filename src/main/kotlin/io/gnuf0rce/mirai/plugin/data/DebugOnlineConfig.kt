package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object DebugOnlineConfig : ReadOnlyPluginConfig("DebugOnlineConfig") {
    @ValueDescription("不发送上线通知的群号")
    val exclude by value(listOf(12345L))
    @ValueDescription("逐个发送消息延时，单位秒")
    val duration by value(10)
}