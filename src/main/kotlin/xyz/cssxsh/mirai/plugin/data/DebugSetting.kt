package xyz.cssxsh.mirai.plugin.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object DebugSetting: ReadOnlyPluginConfig("DebugSetting") {
    @ValueDescription("机器人所有者")
    val owner by value(12345L)
}