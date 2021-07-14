@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package xyz.cssxsh.mirai.plugin.data

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.utils.MiraiInternalApi
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@MiraiInternalApi
object DebugRequestEventData : AutoSavePluginData("DebugRequestEventData") {

    private var friend0 by value(listOf<String>())

    private var group0 by value(listOf<String>())

    var friend by object : ReadWriteProperty<Any?, List<NewFriendRequestEvent>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): List<NewFriendRequestEvent> {
            return friend0.map {
                it.split("|").let { (qq, eventId, fromId, fromNick) ->
                    NewFriendRequestEvent(
                        bot = Bot.getInstance(qq.toLong()),
                        eventId = eventId.toLong(),
                        message = "",
                        fromId = fromId.toLong(),
                        fromGroupId = 0,
                        fromNick = fromNick
                    )
                }
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<NewFriendRequestEvent>) {
            friend0 = value.map {
                "${it.bot.id}|${it.eventId}|${it.fromId}|${it.fromNick}"
            }
        }
    }

    var group by object : ReadWriteProperty<Any?, List<BotInvitedJoinGroupRequestEvent>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): List<BotInvitedJoinGroupRequestEvent> {
            return group0.map {
                it.split("|").let { (qq, eventId, invitorId, groupId) ->
                    BotInvitedJoinGroupRequestEvent(
                        bot = Bot.getInstance(qq.toLong()),
                        eventId = eventId.toLong(),
                        invitorId = invitorId.toLong(),
                        groupId = groupId.toLong(),
                        invitorNick = "",
                        groupName = "",
                    )
                }
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<BotInvitedJoinGroupRequestEvent>) {
            group0 = value.map {
                "${it.bot.id}|${it.eventId}|${it.invitorId}|${it.groupId}"
            }
        }
    }
}