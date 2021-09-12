package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.*

object DebugSetting: ReadOnlyPluginConfig("DebugSetting") {
    @ValueDescription("机器人所有者")
    val owner by value(12345L)

    @ValueName("auto_friend_request")
    @ValueDescription("自动同意好友请求")
    val autoFriendAccept by value(false)

    @ValueName("auto_group_request")
    @ValueDescription("自动同意加群请求")
    val autoGroupAccept by value(false)
}