package io.gnuf0rce.mirai.plugin.command

import io.gnuf0rce.mirai.plugin.*
import io.gnuf0rce.mirai.plugin.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.message.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import java.io.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
object DebugCommands : CoroutineScope by DebugHelperPlugin.childScope("debug-command") {

    private val all: List<Command> by lazy { this::class.nestedClasses.mapNotNull { it.objectInstance as? Command } }

    operator fun iterator(): Iterator<Command> = all.iterator()

    private val owner: CommandOwner get() = DebugHelperPlugin

    private val logger get() = DebugHelperPlugin.logger

    private val randomImageApi get() = DebugSetting.randomImageApi

    private suspend fun Collection<Contact>.sendMessage(message: Message) = map {
        runCatching {
            it.sendMessage(message)
        }.onFailure {
            logger.warning { "发送消息失败 $it" }
        }
    }

    object SendAllCommand : SimpleCommand(owner = owner, primaryName = "send-groups", description = "群广播") {
        @Handler
        suspend fun CommandSender.handle(text: String, atAll: Boolean = false) {
            try {
                val message = if (atAll) AtAll + text + ForceAsLongMessage else text.toPlainText()
                Bot.instances.flatMap(Bot::groups).sendMessage(message)
            } catch (e: Throwable) {
                sendMessage("'${text}'发送失败, $e")
            }
        }
    }

    object AtAllCommand : SimpleCommand(owner = owner, primaryName = "at-all", description = "全体@") {
        @Handler
        suspend fun CommandSender.handle(text: String, group: Group = subject as Group) {
            try {
                val message = AtAll + text + ForceAsLongMessage
                group.sendMessage(message)
            } catch (e: Throwable) {
                sendMessage("'${text}'发送失败, $e")
            }
        }
    }

    object SendCommand : SimpleCommand(owner = owner, primaryName = "send", description = "发送消息") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact, text: String, at: User? = null) {
            try {
                val message: Message = if (at != null) At(at) + text else text.toPlainText()
                contact.sendMessage(message)
            } catch (e: Throwable) {
                sendMessage("'${text}'发送失败, $e")
            }
        }
    }

    object RecallCommand : SimpleCommand(owner = owner, primaryName = "recall", description = "撤回消息") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact? = null) {
            try {
                val (record, source) = when {
                    contact is Member -> {
                        val record = DebugListener.records.getValue(contact.group.id)
                        record to record.findLast { it.fromId == contact.id }
                    }
                    contact != null -> {
                        val record = DebugListener.records.getValue(contact.id)
                        record to record.findLast { it.fromId == contact.bot.id }
                    }
                    this is CommandSenderOnMessage<*> -> {
                        val record = DebugListener.records.getValue(fromEvent.subject.id)
                        record to (fromEvent.message.findIsInstance<QuoteReply>()?.source
                            ?: record.findLast { it.fromId != fromEvent.source.fromId })
                    }
                    else -> {
                        throw IllegalArgumentException("无法指定要撤回消息")
                    }
                }
                if (source != null) {
                    source.recall()
                    record.remove(source)
                    sendMessage("${source.fromId} 的消息撤回成功")
                } else {
                    sendMessage("未找到消息")
                }
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object FriendCommand : SimpleCommand(owner = owner, primaryName = "friend", description = "查看当前的好友") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                sendMessage(buildMessageChain {
                    for (bot in Bot.instances) {
                        appendLine("--- ${bot.render()} ---")
                        for (friend in bot.friends) {
                            appendLine(friend.render())
                        }
                    }
                })
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object GroupCommand : SimpleCommand(owner = owner, primaryName = "group", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                sendMessage(buildMessageChain {
                    for (bot in Bot.instances) {
                        appendLine("--- ${bot.render()} ---")
                        for (group in bot.groups) {
                            group as net.mamoe.mirai.internal.contact.GroupImpl
                            appendLine("${group.render()}[${group.botPermission}]<${group.members.size}>(${group.botMuteRemaining}s) ${group.uin}")
                        }
                    }
                })
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object RequestListCommand : SimpleCommand(owner = owner, primaryName = "request", description = "申请列表") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                sendMessage(DebugRequestEventData.detail())
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object ContactRequestCommand :
        SimpleCommand(owner = owner, primaryName = "contact-request", description = "接受联系人") {
        @Handler
        suspend fun CommandSender.handle(
            id: Long,
            accept: Boolean = true,
            black: Boolean = false,
            message: String = ""
        ) {
            try {
                val request = requireNotNull(DebugRequestEventData.handle(id, accept, black, message)) { "找不到事件" }
                sendMessage("请求已处理 $request")
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object FriendDeleteCommand : SimpleCommand(owner = owner, primaryName = "contact-delete", description = "删除联系人") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact) {
            try {
                val r = (contact as? Friend)?.delete()
                    ?: (contact as? Group)?.quit()
                    ?: (contact as? Stranger)?.delete()
                if (r != null) {
                    sendMessage("处理成功")
                } else {
                    sendMessage("未找到联系人")
                }
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object GroupNickCommand : SimpleCommand(owner = owner, primaryName = "group-nick", description = "群昵称") {
        @Handler
        suspend fun CommandSender.handle(name: String, group: Group = subject as Group) {
            try {
                group.botAsMember.nameCard = name
                sendMessage("处理成功")
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object GarbageCommand : SimpleCommand(owner = owner, primaryName = "gc", description = "垃圾回收") {
        @Handler
        suspend fun CommandSender.handle() {
            System.gc()
            sendMessage("GC完毕")
        }
    }

    object ImageCommand : SimpleCommand(owner = owner, primaryName = "random-image", description = "随机发送一张图片") {
        private val http = HttpClient(OkHttp)

        @Handler
        suspend fun CommandSender.handle(contact: Contact = subject as Contact) {
            try {
                http.get<InputStream>(randomImageApi).use { input ->
                    contact.sendImage(input)
                }
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object ForwardCommand : SimpleCommand(owner = owner, primaryName = "forward", description = "转发") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(contact: Contact, title: String = "转发测试") {
            try {
                val nodes = mutableListOf<MessageEvent>()
                while (isActive && nodes.size <= 200) {
                    val message = fromEvent.nextMessage { nodes.add(it) }.contentToString()

                    if (message.endsWith('.') || message.endsWith('。')) break
                }
                contact.sendMessage(nodes.toForwardMessage(object : ForwardMessage.DisplayStrategy {
                    override fun generateTitle(forward: RawForwardMessage): String = title
                }))
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object ForkCommand : SimpleCommand(owner = owner, primaryName = "fork", description = "从mirai-code构造消息") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(contact: Contact, vararg codes: String) {
            try {
                for (code in codes) {
                    try {
                        val message = MiraiCode.deserializeMiraiCode(code, fromEvent.subject)
                        contact.sendMessage(message)
                    } finally {
                        logger.info { "$code 已处理" }
                    }
                }
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object RichCommand : SimpleCommand(owner = owner, primaryName = "rich", description = "构造卡片消息") {
        private val SERVICE_ID = """(?<=serviceID=")\d+"""
            .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(content: String) {
            try {
                @OptIn(MiraiExperimentalApi::class)
                val rich = when (val char = content.first { it.isWhitespace().not() }) {
                    '{' -> {
                        val json = with(fromEvent.message.content) { substring(indexOf('{'), length) }
                        LightApp(content = json)
                    }
                    '<' -> {
                        val xml = with(fromEvent.message.content) { substring(indexOf('<'), length) }
                        val serviceId = requireNotNull(SERVICE_ID.find(xml)) { "Not serviceID" }.value.toInt()
                        SimpleServiceMessage(serviceId = serviceId, content = xml)
                    }
                    else -> throw IllegalArgumentException("Not is json or xml with \\x${char.code.toString(16)}")
                }
                sendMessage(rich)
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            } finally {
                logger.info { "卡片消息处理完成" }
            }
        }
    }

    object RegisteredCommand : SimpleCommand(owner = owner, primaryName = "registered", description = "查看已注册指令") {
        @Handler
        suspend fun UserCommandSender.handle() {
            try {
                val registered = CommandManager.allRegisteredCommands
                val forward = buildForwardMessage(subject) {
                    for (command in registered) {
                        bot.named(command.owner.parentPermission.id.namespace) says {
                            appendLine("Id: ${command.permission.id}")
                            appendLine("HasPermission: ${hasPermission(command.permission)}")
                            appendLine("Description: ${command.description}")
                            appendLine(command.usage)
                        }
                    }

                    displayStrategy = object : ForwardMessage.DisplayStrategy {
                        override fun generateTitle(forward: RawForwardMessage): String {
                            return "已注册指令"
                        }

                        override fun generateSummary(forward: RawForwardMessage): String {
                            return "已注册${registered.size}条指令"
                        }
                    }
                }
                sendMessage(forward + IgnoreLengthCheck)
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object DeviceInfoCommand : SimpleCommand(owner = owner, primaryName = "device") {
        @Handler
        suspend fun UserCommandSender.handle() {
            try {
                val forward = buildForwardMessage(subject) {
                    var count = 0
                    for (bot in Bot.instances) {
                        try {
                            val json = DeviceInfoManager.serialize(bot.configuration.deviceInfo!!(bot))
                            bot says json
                            count++
                        } catch (cause: Throwable) {
                            bot says cause.toString()
                        }
                    }

                    displayStrategy = object : ForwardMessage.DisplayStrategy {
                        override fun generateTitle(forward: RawForwardMessage): String {
                            return "机器人设备信息"
                        }

                        override fun generateSummary(forward: RawForwardMessage): String {
                            return "共${bot}条设备信息"
                        }
                    }
                }
                sendMessage(forward + IgnoreLengthCheck)
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

}