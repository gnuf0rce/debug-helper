package io.gnuf0rce.mirai.plugin.data

import net.mamoe.mirai.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.data.*
import net.mamoe.mirai.data.RequestEventData.Factory.toRequestEventData
import net.mamoe.mirai.event.events.*

object DebugRequestEventData : AutoSavePluginData("DebugRequestEventData") {

    private val friends by value<MutableMap<Long, List<RequestEventData.NewFriendRequest>>>()

    private val groups by value<MutableMap<Long, List<RequestEventData.BotInvitedJoinGroupRequest>>>()

    private val members by value<MutableMap<Long, List<RequestEventData.MemberJoinRequest>>>()

    private fun requests() = object : Iterator<Map.Entry<Long, List<RequestEventData>>> {
        private val friend = friends.iterator()
        private val group = groups.iterator()
        private val member = members.iterator()

        override fun hasNext(): Boolean = friend.hasNext() || group.hasNext() || member.hasNext()

        override fun next(): Map.Entry<Long, List<RequestEventData>> {
            if (friend.hasNext()) return friend.next()
            if (group.hasNext()) return group.next()
            if (member.hasNext()) return member.next()
            throw NoSuchElementException()
        }
    }

    @OptIn(ConsoleExperimentalApi::class)
    fun detail(): String = buildString {
        for ((qq, list) in requests()) {
            if (list.isEmpty()) continue
            val bot = Bot.getInstance(qq)
            appendLine("--- ${bot.render()} ---")
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
                    if (request.requester == id) return request
                    if (request.invitor == id) return request
                }
            }
        }
        return null
    }

    suspend fun handle(id: Long, accept: Boolean, black: Boolean, message: String): RequestEventData? {
        for ((qq, list) in requests()) {
            val request = list[id] ?: continue
            val bot = Bot.getInstance(qq)
            if (accept) {
                request.accept(bot)
            } else {
                when (request) {
                    is RequestEventData.NewFriendRequest -> request.reject(bot, black)
                    is RequestEventData.BotInvitedJoinGroupRequest -> request.reject(bot)
                    is RequestEventData.MemberJoinRequest -> request.reject(bot, black, message)
                }
            }
        }
        return null
    }

    operator fun plusAssign(event: NewFriendRequestEvent) {
        friends.compute(event.bot.id) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }

    operator fun plusAssign(event: BotInvitedJoinGroupRequestEvent) {
        groups.compute(event.bot.id) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }

    operator fun plusAssign(event: MemberJoinRequestEvent) {
        members.compute(event.bot.id) { _, list ->
            list.orEmpty() + event.toRequestEventData()
        }
    }

    operator fun minusAssign(event: FriendAddEvent) {
        friends.compute(event.bot.id) { _, list ->
            list.orEmpty().filterNot { it.requester == event.friend.id }
        }
    }

    operator fun minusAssign(event: BotJoinGroupEvent) {
        groups.compute(event.bot.id) { _, list ->
            list.orEmpty().filterNot { it.groupId == event.group.id }
        }
    }

    operator fun minusAssign(event: MemberJoinRequestEvent) {
        groups.compute(event.bot.id) { _, list ->
            list.orEmpty().filterNot { it.groupId == event.groupId && it.invitor == event.invitorId }
        }
    }
}