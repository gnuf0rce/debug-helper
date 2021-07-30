package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*

object DebugSubscriber : CoroutineScope by DebugHelperPlugin.childScope("debug-subscriber") {

    private val logger by DebugHelperPlugin::logger

    private val owner by DebugSetting::owner

    private val friend by DebugRequestEventData::friend

    private val group by DebugRequestEventData::group

    private val exclude by DebugOnlineConfig::exclude

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

    private val avatars = mutableMapOf<Long, ByteArray>()

    private suspend fun Contact.sendOnlineMessage(): Boolean {
        return runCatching {
            val avatar = avatars.getOrPut(bot.id) { HttpClient(OkHttp).use { it.get(bot.avatarUrl) } }
            val image = uploadImage(avatar.inputStream())
            sendMessage(message = online(bot = bot, picture = image.queryUrl()))
        }.onSuccess {
            logger.info { "向${group}发送上线消息成功" }
        }.onFailure {
            logger.warning { "向${group}发送上线消息失败 $it" }
        }.isSuccess
    }

    fun start() {
        globalEventChannel().apply {
            subscribeAlways<NewFriendRequestEvent> {
                friend += it.toData()
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
            //
            subscribeAlways<BotInvitedJoinGroupRequestEvent> {
                group += it.toData()
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
            //
            subscribeAlways<BotOnlineEvent> {
                delay(30 * 1000L)
                bot.groups.filterNot { it.id in exclude }.forEach { group ->
                    isActive && group.sendOnlineMessage()
                }
            }
            subscribeGroupMessages {
                atBot { group.sendOnlineMessage() }
            }
        }
    }

    fun stop() {
        coroutineContext.cancelChildren()
    }
}