package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.data.PluginDataExtensions.mapKeys
import net.mamoe.mirai.data.*
import net.mamoe.mirai.data.RequestEventData.Factory.toRequestEventData
import net.mamoe.mirai.event.events.*

object DebugRequestEventData : AutoSavePluginData("DebugRequestEventData") {

    private val friends by value<MutableMap<Long, List<RequestEventData.NewFriendRequest>>>()
        .mapKeys(Bot::getInstance, Bot::id)

    private val groups by value<MutableMap<Long, List<RequestEventData.BotInvitedJoinGroupRequest>>>()
        .mapKeys(Bot::getInstance, Bot::id)

    private val members by value<MutableMap<Long, List<RequestEventData.MemberJoinRequest>>>()
        .mapKeys(Bot::getInstance, Bot::id)

    fun detail(): String = buildString {
        for ((bot, list) in friends + groups + members) {
            if (list.isEmpty()) continue
            appendLine("--- ${bot.nick} ${bot.id} ---")
            for (request in list) {
                appendLine(request)
            }
        }
        if (isEmpty()) {
            appendLine("没有记录")
        }
    }

    private operator fun List<RequestEventData>.get(id: Long): RequestEventData? {
        for (request in this) {
            if (request.eventId == id) return request
            when (request) {
                is RequestEventData.NewFriendRequest -> {
                    if (request.requester == id) return request
                }
                is RequestEventData.BotInvitedJoinGroupRequest -> {
                    if (request.groupId == id) return request
                    if (request.invitor == id) return request
                }
                is RequestEventData.MemberJoinRequest -> {
                    if (request.invitor == id) return request
                }
            }
        }
        return null
    }

    suspend fun handle(id: Long, accept: Boolean, black: Boolean, message: String): RequestEventData? {
        for ((bot, list) in friends + groups + members) {
            val request = list[id] ?: continue
            if (accept) {
                request.accept(bot)
            } else {
                when (request) {
                    is RequestEventData.NewFriendRequest -> {
                        request.reject(bot, black)
                    }
                    is RequestEventData.BotInvitedJoinGroupRequest -> {
                        request.reject(bot)
                    }
                    is RequestEventData.MemberJoinRequest -> {
                        request.reject(bot, black, message)
                    }
                }
            }
        }
        return null
    }

    operator fun plusAssign(event: NewFriendRequestEvent) {
        friends.compute(event.bot) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }

    operator fun plusAssign(event: BotInvitedJoinGroupRequestEvent) {
        groups.compute(event.bot) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }

    operator fun plusAssign(event: MemberJoinRequestEvent) {
        members.compute(event.bot) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }
}