package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

object DebugListener : SimpleListenerHost() {

    private val logger by DebugHelperPlugin::logger

    private val owner by DebugSetting::owner

    private val friend by DebugRequestEventData::friend

    private val group by DebugRequestEventData::group

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

    private suspend fun Contact.sendOnlineMessage(): Boolean {
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

    private val exclude by lazy {
        DebugHelperPlugin.registerPermission("online.exclude", "不发送上线通知")
    }

    @EventHandler
    suspend fun NewFriendRequestEvent.mark() {
        friend += toData()
        runCatching {
            bot.getFriendOrFail(owner).sendMessage(buildMessageChain {
                appendLine("@${fromNick}#${fromId}")
                appendLine("申请添加好友")
                appendLine("from $fromGroup")
                appendLine(message)
            })
        }.onFailure {
            logger.warning { "发送消息失败，$it" }
        }
    }

    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.mark() {
        group += toData()
        runCatching {
            bot.getFriendOrFail(owner).sendMessage(buildMessageChain {
                appendLine("@${invitorNick}#${invitorId}")
                appendLine("申请添加群")
                appendLine("to [$groupName](${groupId})")
            })
        }.onFailure {
            logger.warning { "发送消息失败，$it" }
        }
    }

    @EventHandler
    suspend fun BotOnlineEvent.notify() {
        bot.groups.filterNot { exclude.testPermission(it.permitteeId) }.forEach { group ->
            isActive && group.run {
                delay(DebugOnlineConfig.duration * 1000L)
                sendOnlineMessage()
            }
        }
    }
}