package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.*

object DebugSetting: ReadOnlyPluginConfig("DebugSetting") {
    @ValueDescription("机器人所有者")
    val owner by value(12345L)
}