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
            runCatching {
                val message = if (atAll) AtAll + text + ForceAsLongMessage else text.toPlainText()
                Bot.instances.flatMap(Bot::groups).sendMessage(message)
            }.onFailure {
                sendMessage("'${text}'发送失败, $it")
            }
        }
    }

    object AtAllCommand : SimpleCommand(owner = owner, "at-all", description = "预告") {
        @Handler
        suspend fun CommandSender.handle(text: String, group: Group = subject as Group) {
            runCatching {
                val message = AtAll + text + ForceAsLongMessage
                group.sendMessage(message)
            }.onFailure {
                sendMessage("'${text}'发送失败, $it")
            }
        }
    }

    object SendCommand : SimpleCommand(owner = owner, "send", description = "发送消息") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact, text: String, at: User? = null) {
            runCatching {
                val message: Message = if (at != null) At(at) + text else text.toPlainText()
                contact.sendMessage(message)
            }.onFailure {
                sendMessage("'${text}'发送失败, $it")
            }
        }
    }

    object RecallCommand : SimpleCommand(owner = owner, "recall", description = "撤回消息") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle() {
            runCatching {
                fromEvent.message.firstIsInstance<QuoteReply>().recallSource()
            }.onSuccess {
                sendMessage("...撤回成功")
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object FriendCommand : SimpleCommand(owner = owner, "friend", description = "查看当前的好友") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    for (bot in Bot.instances) {
                        appendLine("--- ${bot.render()} ---")
                        for (friend in bot.friends) {
                            appendLine(friend.render())
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object GroupCommand : SimpleCommand(owner = owner, "group", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    for (bot in Bot.instances) {
                        appendLine("--- ${bot.render()} ---")
                        for (group in bot.groups) {
                            appendLine("${group.render()}[${group.botPermission}]<${group.members.size}>(${group.botMuteRemaining})")
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object RequestListCommand : SimpleCommand(owner = owner, "request", description = "申请列表") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(DebugRequestEventData.detail())
            }.onFailure {
                sendMessage("出现错误 $it")
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
            runCatching {
                val request = requireNotNull(DebugRequestEventData.handle(id, accept, black, message)) { "找不到事件" }
                sendMessage("请求已处理 $request")
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object FriendDeleteCommand : SimpleCommand(owner = owner, "contact-delete", description = "删除联系人") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact) {
            runCatching {
                (contact as? Friend)?.delete()
                (contact as? Group)?.quit()
                (contact as? Stranger)?.delete()
            }.onSuccess {
                sendMessage("处理成功")
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object GroupNickCommand : SimpleCommand(owner = owner, "group-nick", description = "群昵称") {
        @Handler
        suspend fun CommandSender.handle(name: String, group: Group = subject as Group) {
            runCatching {
                group.botAsMember.nameCard = name
            }.onSuccess {
                sendMessage("处理成功")
            }.onFailure {
                sendMessage("出现错误 $it")
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
            runCatching {
                http.get<InputStream>(randomImageApi).use { input ->
                    contact.sendImage(input)
                }
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object ForwardCommand : SimpleCommand(owner = owner, "forward", description = "转发") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(contact: Contact, title: String = "转发测试") {
            runCatching {
                val nodes = mutableListOf<MessageEvent>()
                while (isActive && nodes.size <= 200) {
                    val message = fromEvent.nextMessage { nodes.add(it) }.contentToString()

                    if (message.endsWith('.') || message.endsWith('。')) break
                }
                contact.sendMessage(nodes.toForwardMessage(object : ForwardMessage.DisplayStrategy {
                    override fun generateTitle(forward: RawForwardMessage): String = title
                }))
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object ForkCommand : SimpleCommand(owner = owner, "fork", description = "从mirai-code构造消息") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(contact: Contact, vararg codes: String) {
            runCatching {
                for (code in codes) {
                    try {
                        val message = MiraiCode.deserializeMiraiCode(code, fromEvent.subject)
                        contact.sendMessage(message)
                    } finally {
                        logger.info { "$code 已处理" }
                    }
                }
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }
}





