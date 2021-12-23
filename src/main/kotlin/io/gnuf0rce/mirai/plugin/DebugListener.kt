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
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

@OptIn(MiraiExperimentalApi::class)
object DebugListener : SimpleListenerHost() {

    private val logger by DebugHelperPlugin::logger

    private val http = HttpClient(OkHttp)

    /**
     * @see [Bot.getFriendOrFail]
     * @see [DebugSetting.owner]
     */
    private fun Bot.owner() = getFriendOrFail(DebugSetting.owner)

    private val autoFriendAccept by DebugSetting::autoFriendAccept

    private val autoGroupAccept by DebugSetting::autoGroupAccept

    private val autoMemberAccept by DebugSetting::autoMemberAccept

    private val autoSendStatus by DebugSetting::autoSendStatus

    private val autoDownloadMessage by DebugSetting::autoDownloadMessage

    private val onlineMessageSendDuration by DebugOnlineConfig::duration

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
                http.get<ByteArray>(bot.avatarUrl).toExternalResource()
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

    /**
     * @see [BotOnlineEvent.notify]
     */
    @Suppress("unused")
    val include by lazy {
        DebugHelperPlugin.registerPermission("online.include", "发送上线通知")
    }

    /**
     * @see [autoFriendAccept]
     * @see [DebugRequestEventData]
     */
    @EventHandler
    suspend fun NewFriendRequestEvent.mark() {
        try {
            if (autoFriendAccept) accept() else DebugRequestEventData += this
            bot.owner().sendMessage(buildMessageChain {
                appendLine("@${fromNick}#${fromId} with <${eventId}>")
                appendLine("申请添加好友")
                appendLine("from $fromGroup")
                appendLine(message)
                if (autoFriendAccept) appendLine("已自动同意")
            })
        } catch (cause: Throwable) {
            logger.warning({ "处理 新好友申请事件失败" }, cause)
        }
    }

    /**
     * @see [autoGroupAccept]
     * @see [DebugRequestEventData]
     */
    @EventHandler
    suspend fun BotInvitedJoinGroupRequestEvent.mark() {
        try {
            if (autoGroupAccept) accept() else DebugRequestEventData += this
            bot.owner().sendMessage(buildMessageChain {
                appendLine("@${invitorNick}#${invitorId} with <${eventId}>")
                appendLine("邀请机器人加入群")
                appendLine("to [${groupName}](${groupId})")
                if (autoGroupAccept) appendLine("已自动同意")
            })
        } catch (cause: Throwable) {
            logger.warning({ "处理 被邀请加群事件失败" }, cause)
        }
    }

    /**
     * @see [autoMemberAccept]
     * @see [DebugRequestEventData]
     */
    @EventHandler
    suspend fun MemberJoinRequestEvent.mark() {
        try {
            if (autoMemberAccept) accept() else DebugRequestEventData += this
            bot.owner().sendMessage(buildMessageChain {
                appendLine("@${fromNick}#${fromId} with <${eventId}>")
                appendLine("申请加入群")
                appendLine("to [$groupName](${groupId}) by $invitorId")
                appendLine(message)
                if (autoMemberAccept) appendLine("已自动同意")
            })
        } catch (cause: Throwable) {
            logger.warning({ "处理 新成员加群事件失败" }, cause)
        }
    }

    /**
     * @see [DebugRequestEventData]
     */
    @EventHandler
    fun FriendAddEvent.handle() {
        DebugRequestEventData -= this
    }

    /**
     * @see [DebugRequestEventData]
     */
    @EventHandler
    suspend fun BotJoinGroupEvent.handle() {
        DebugRequestEventData -= this
        if (this is BotJoinGroupEvent.Invite) {
            try {
                @OptIn(ConsoleExperimentalApi::class)
                bot.owner().sendMessage("机器人被 ${invitor.render()} 邀请加入群")
            } catch (cause: Throwable) {
                logger.warning({ "处理 机器人被邀请加群事件失败" }, cause)
            }
        }
    }

    /**
     * @see [DebugRequestEventData]
     */
    @EventHandler
    fun MemberJoinRequestEvent.handle() {
        DebugRequestEventData -= this
    }

    /**
     * @see [MessageEvent.mark]
     * @see [MessagePostSendEvent.mark]
     */
    val records = mutableMapOf<Long, MutableList<MessageSource>>()

    private val keys = listOf(FlashImage, OnlineAudio, RichMessage)

    private fun download(message: MessageChain) = launch(SupervisorJob()) {
        when (val target = keys.firstNotNullOfOrNull { key -> message[key] } ?: return@launch) {
            is FlashImage -> {
                try {
                    DebugHelperPlugin.dataFolder.resolve("flash")
                        .resolve("${message.source.fromId}")
                        .resolve(target.image.imageId)
                        .apply { parentFile.mkdirs() }
                        .writeBytes(http.get(target.image.queryUrl()))
                } catch (e: Throwable) {
                    logger.warning { "$target 下载失败, $e" }
                }
            }
            is OnlineAudio -> {
                try {
                    DebugHelperPlugin.dataFolder.resolve("audio")
                        .resolve("${message.source.fromId}")
                        .resolve(target.filename)
                        .apply { parentFile.mkdirs() }
                        .writeBytes(http.get(target.urlForDownload))
                } catch (e: Throwable) {
                    logger.warning { "$target 下载失败, $e" }
                }
            }
            is RichMessage -> {
                try {
                    val format = when (target.content[0]) {
                        '<' -> "xml"
                        '{' -> "json"
                        else -> "rich"
                    }
                    DebugHelperPlugin.dataFolder.resolve("service")
                        .resolve("${message.source.fromId}")
                        .resolve("${message.source.time}.${format}")
                        .apply { parentFile.mkdirs() }
                        .writeText(target.content)
                } catch (e: Throwable) {
                    logger.warning { "$target 下载失败, $e" }
                }
            }
            else -> {
                logger.warning { "Not Found Save Method." }
            }
        }
    }

    /**
     * @see [records]
     * @see [autoDownloadMessage]
     */
    @EventHandler
    fun MessageEvent.mark() {
        records.getOrPut(subject.id, ::mutableListOf).add(source)
        if (autoDownloadMessage) {
            download(message)
        }
    }

    /**
     * @see [records]
     */
    @EventHandler
    fun MessagePostSendEvent<*>.mark() {
        records.getOrPut(target.id, ::mutableListOf).add(source ?: return)
    }

    private var status = false

    /**
     * @see [include]
     */
    @EventHandler
    fun BotOnlineEvent.notify() {
        if (autoSendStatus > 0 && !status) {
            launch(SupervisorJob()) {
                status = true
                while (isActive) {
                    BuiltInCommands.StatusCommand.runCatching {
                        bot.owner().asCommandSender().handle()
                    }.onFailure {
                        logger.warning({ "发送状态消息失败" }, it)
                    }
                    delay(autoSendStatus * 60_000L)
                }
            }
        }
        launch(SupervisorJob()) {
            for (group in bot.groups) {
                if (!isActive) break
                if (!include.testPermission(group.permitteeId)) continue

                group.sendOnlineMessage(onlineMessageSendDuration * 1000L)
            }
        }
    }
}