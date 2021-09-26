@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:OptIn(MiraiInternalApi::class)

package io.gnuf0rce.mirai.plugin.data

import kotlinx.serialization.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.*

object DebugRequestEventData : AutoSavePluginData("DebugRequestEventData") {

    val friend by value(mutableListOf<FriendRequestEventData>())

    val group by value(mutableListOf<GroupRequestEventData>())
}

@Serializable
data class FriendRequestEventData(
    val bot: Long,
    val eventId: Long,
    val message: String,
    val fromId: Long,
    val fromGroupId: Long,
    val fromNick: String
)

fun NewFriendRequestEvent.toData() = FriendRequestEventData(
    bot.id,
    eventId,
    message,
    fromId,
    fromGroupId,
    fromNick
)

fun FriendRequestEventData.toEvent() = NewFriendRequestEvent(
    Bot.getInstance(bot),
    eventId,
    message,
    fromId,
    fromGroupId,
    fromNick
)

@Serializable
data class GroupRequestEventData(
    val bot: Long,
    val eventId: Long,
    val invitorId: Long,
    val groupId: Long,
    val groupName: String,
    val invitorNick: String
)

fun BotInvitedJoinGroupRequestEvent.toData() = GroupRequestEventData(
    bot.id,
    eventId,
    invitorId,
    groupId,
    groupName,
    invitorNick
)

fun GroupRequestEventData.toEvent() = BotInvitedJoinGroupRequestEvent(
    Bot.getInstance(bot),
    eventId,
    invitorId,
    groupId,
    groupName,
    invitorNick
)