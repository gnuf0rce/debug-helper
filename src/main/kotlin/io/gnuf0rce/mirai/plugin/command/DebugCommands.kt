package io.gnuf0rce.mirai.plugin.command

import io.gnuf0rce.mirai.plugin.*
import io.gnuf0rce.mirai.plugin.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.internal.command.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.message.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import java.io.InputStream

@OptIn(ConsoleExperimentalApi::class)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
object DebugCommands : CoroutineScope by DebugHelperPlugin.childScope("debug-command") {

    private val all by lazy { this::class.nestedClasses.mapNotNull { it.objectInstance as? Command } }

    fun registerAll() = all.associateWith { it.register() }

    fun unregisterAll() = all.associateWith { it.unregister() }

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

    object SendAllCommand : SimpleCommand(owner = owner, "send-groups", description = "预告") {
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

    object AtAllCommand : SimpleCommand(owner = owner, "at-all", description = "预告") {
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

    object SendCommand : SimpleCommand(owner = owner, "send", description = "发送消息") {
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

    object RecallCommand : SimpleCommand(owner = owner, "recall", description = "撤回消息") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle() {
            try {
                val u = DebugListener.records[fromEvent.subject.id]?.recall()
                    ?: fromEvent.message.findIsInstance<QuoteReply>()?.recallSource()
                if (u != null) {
                    sendMessage("...撤回成功")
                } else {
                    sendMessage("未找到消息")
                }
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object FriendCommand : SimpleCommand(owner = owner, "friend", description = "查看当前的好友") {
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

    object GroupCommand : SimpleCommand(owner = owner, "group", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                sendMessage(buildMessageChain {
                    for (bot in Bot.instances) {
                        appendLine("--- ${bot.render()} ---")
                        for (group in bot.groups) {
                            appendLine("${group.render()}[${group.botPermission}]<${group.members.size}>(${group.botMuteRemaining})")
                        }
                    }
                })
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object RequestListCommand : SimpleCommand(owner = owner, "request", description = "申请列表") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                sendMessage(DebugRequestEventData.detail())
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object ContactRequestCommand : SimpleCommand(owner = owner, "contact-request", description = "接受联系人") {
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

    object FriendDeleteCommand : SimpleCommand(owner = owner, "contact-delete", description = "删除联系人") {
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

    object GroupNickCommand : SimpleCommand(owner = owner, "group-nick", description = "群昵称") {
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

    object GarbageCommand : SimpleCommand(owner = owner, "gc", description = "垃圾回收") {
        @Handler
        suspend fun CommandSender.handle() {
            System.gc()
            sendMessage("GC完毕")
        }
    }

    object ImageCommand : SimpleCommand(owner = owner, "random-image", description = "随机发送一张图片") {
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

    object ForwardCommand : SimpleCommand(owner = owner, "forward", description = "转发") {
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

    object ForkCommand : SimpleCommand(owner = owner, "fork", description = "从mirai-code构造消息") {
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

    object RichCommand : SimpleCommand(owner = owner, "rich", description = "构造卡片消息") {
        private val SERVICE_ID = """(?<=serviceID=")\d+""".toRegex(RegexOption.IGNORE_CASE)

        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(content: String) {
            try {
                @OptIn(MiraiExperimentalApi::class)
                val rich = when (content[0]) {
                    '{' -> {
                        val json = with(fromEvent.message.content) { substring(indexOf('{'), length) }
                        LightApp(content = json)
                    }
                    '<' -> {
                        val json = with(fromEvent.message.content) { substring(indexOf('<'), length) }
                        val serviceId = requireNotNull(SERVICE_ID.find(content)) { "Not serviceID" }.value.toInt()
                        SimpleServiceMessage(serviceId = serviceId, content = json)
                    }
                    else -> throw IllegalArgumentException("Not is json or xml.")
                }
                sendMessage(rich)
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

    object RegisteredCommand : SimpleCommand(owner = owner, "registered", description = "查看已注册指令") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle() {
            try {
                val commands = CommandManagerImpl.allRegisteredCommands
                val strategy = object : ForwardMessage.DisplayStrategy {
                    override fun generateTitle(forward: RawForwardMessage): String {
                        return "已注册指令"
                    }

                    override fun generateSummary(forward: RawForwardMessage): String {
                        return "已注册${commands.size}条指令"
                    }
                }
                val nodes = commands.map { command ->
                    ForwardMessage.Node(
                        time = (System.currentTimeMillis() / 1_000).toInt(),
                        senderId = bot!!.id,
                        senderName = command.owner.parentPermission.id.namespace,
                        message = buildMessageChain {
                            appendLine("Id: ${command.permission.id}")
                            appendLine("HasPermission: ${hasPermission(command.permission)}")
                            appendLine("Description: ${command.description}")
                            appendLine(command.usage)
                        }
                    )
                }

                val forward = RawForwardMessage(nodes).render(strategy)
                sendMessage(forward + IgnoreLengthCheck)
            } catch (e: Throwable) {
                sendMessage("出现错误 $e")
            }
        }
    }

}




