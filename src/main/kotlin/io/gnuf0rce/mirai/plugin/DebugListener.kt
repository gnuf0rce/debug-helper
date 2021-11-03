package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.data.RequestEventData.Factory.toRequestEventData
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

object DebugListener : SimpleListenerHost() {

    private val logger by DebugHelperPlugin::logger

    private fun Bot.owner() = getFriend(DebugSetting.owner)

    private val autoFriendAccept by DebugSetting::autoFriendAccept

    private val autoGroupAccept by DebugSetting::autoGroupAccept

    private val autoMemberAccept by DebugSetting::autoMemberAccept

    private val autoSendStatus by DebugSetting::autoSendStatus

    private val onlineMessageSendDuration by DebugOnlineConfig::duration

    @OptIn(MiraiExperimentalApi::class)
    private fun online(bot: Bot, picture: String = bot.avatarUrl) = buildXmlMessage(1) {
        templateId = -1
        action = "web"
        brief = "QQ Bot 已启动"
        flag = 0

        item {
            layout = 2
            picture(coverUrl = picture)
            title(text = "[${bot.nick}]已加入该会话")
            summary(text = "[${bot.nick}]开始接受指令执行")
        }

        source(name = "QQ Bot 已启动，可以开始执行指令")
    }

    private val avatars = mutableMapOf<Long, ExternalResource>()

    private suspend fun Contact.sendOnlineMessage(millis: Long = 0): Boolean {
        delay(millis)
        return runCatching {
            val avatar = avatars.getOrPut(bot.id) {
                HttpClient(OkHttp).use { it.get<ByteArray>(bot.avatarUrl) }.toExternalResource()
            }
            val image = uploadImage(resource = avatar)
            sendMessage(message = online(bot = bot, picture = image.queryUrl()))
        }.onSuccess {
            logger.info { "向[${id}]发送上线消息成功" }
        }.onFailure {
            logger.warning { "向[${id}]发送上线消息失败 $it" }
        }.isSuccess
    }

    private fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
        return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
    }

    private val include by lazy {
        DebugHelperPlugin.registerPermission("online.include", "发送上线通知")
    }

    @EventHandler
    suspend fun NewFriendRequestEvent.mark() {
        if (autoFriendAccept) accept() else DebugRequestEventData += this
        toRequestEventData()
        runCatching {
            bot.owner()?.sendMessage(buildMessageChain {
                appendLine("@${fromNick}#${fromId} with <${eventId}>")
                appendLine("申请添加好友")
                appendLine("from $fromGroup")
                appendLine(message)
                if (autoFriendAccept) appendLine("已自动同意")
            })
        }.onFailure {
            logger.warning { "发送消息失败，$it" }
        }
    }

    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.mark() {
        if (autoGroupAccept) accept() else DebugRequestEventData += this
        runCatching {
            bot.owner()?.sendMessage(buildMessageChain {
                appendLine("@${invitorNick}#${invitorId} with <${eventId}>")
                appendLine("邀请机器人加入群")
                appendLine("to [$groupName](${groupId})")
                if (autoGroupAccept) appendLine("已自动同意")
            })
        }.onFailure {
            logger.warning { "发送消息失败，$it" }
        }
    }

    @EventHandler
    suspend fun MemberJoinRequestEvent.mark() {
        if (autoMemberAccept) accept() else DebugRequestEventData += this
        runCatching {
            bot.owner()?.sendMessage(buildMessageChain {
                appendLine("@${fromNick}#${fromId} with <${eventId}>")
                appendLine("申请加入群")
                appendLine("to [$groupName](${groupId}) by $invitorId")
                appendLine(message)
                if (autoMemberAccept) appendLine("已自动同意")
            })
        }.onFailure {
            logger.warning { "发送消息失败，$it" }
        }
    }

    private var status = false

    @EventHandler
    suspend fun BotOnlineEvent.notify() = supervisorScope {
        if (autoSendStatus > 0 && !status) {
            this@DebugListener.launch {
                status = true
                while (isActive) {
                    BuiltInCommands.StatusCommand.runCatching {
                        bot.owner()?.asCommandSender()?.handle()
                    }.onFailure {
                        logger.warning({ "发送状态消息失败" }, it)
                    }
                    delay(autoSendStatus * 60_000L)
                }
            }
        }
        bot.groups.filter { include.testPermission(it.permitteeId) }.forEach { group ->
            isActive && group.sendOnlineMessage(onlineMessageSendDuration * 1000L)
        }
    }
}