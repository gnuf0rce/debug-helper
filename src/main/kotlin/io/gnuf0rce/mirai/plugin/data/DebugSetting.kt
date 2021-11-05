package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.console.data.*

object DebugSetting: ReadOnlyPluginConfig("DebugSetting") {
    const val OwnerDefault = 12345L

    @ValueName("owner")
    @ValueDescription("机器人所有者")
    val owner by value(OwnerDefault)

    @ValueName("auto_friend_request")
    @ValueDescription("自动同意好友请求")
    val autoFriendAccept by value(false)

    @ValueName("auto_group_request")
    @ValueDescription("自动同意加群请求")
    val autoGroupAccept by value(false)

    @ValueName("auto_member_accept")
    @ValueDescription("自动同意新成员请求")
    val autoMemberAccept by value(false)

    @ValueName("auto_send_status")
    @ValueDescription("自动发送机器人状态到所有者的间隔，单位为分钟，为零时不开启此项功能")
    val autoSendStatus by value(60)

    @ValueName("random_image_api")
    @ValueDescription("随即图片API by https://rainchan.win/projects/pximg")
    val randomImageApi by value("https://pximg.rainchan.win/img")
}