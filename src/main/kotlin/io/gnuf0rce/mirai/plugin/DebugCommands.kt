package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import kotlinx.coroutines.CoroutineScope
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*

@ConsoleExperimentalApi
object DebugCommands: CoroutineScope by DebugHelperPlugin.childScope("debug-command") {

    private val all by lazy { this::class.nestedClasses.mapNotNull { it.objectInstance as? Command } }

    fun registerAll() = all.associateWith { it.register() }

    private val owner: CommandOwner get() = DebugHelperPlugin

    private suspend fun Collection<Contact>.sendMessage(message: String) = map {
        runCatching {
            it.sendMessage(message)
        }
    }

    @Suppress("unused")
    object SendAllCommand : SimpleCommand(owner = owner, "send-groups", description = "预告") {
        @Handler
        suspend fun CommandSender.handle(message: String) {
            runCatching {
                Bot.instances.flatMap(Bot::groups).sendMessage(message)
            }.onFailure {
                sendMessage("'${message}'发送失败, $it")
            }
        }
    }

    @Suppress("unused")
    object SendCommand : SimpleCommand(owner = owner, "send", description = "发送消息") {
        @Handler
        suspend fun CommandSender.handle(contact: Contact, message: String, user: User? = null) {
            runCatching {
                var msg: Message = message.toPlainText()
                if (user != null) msg += At(user)
                contact.sendMessage(msg)
            }.onFailure {
                sendMessage("'${message}'发送失败, $it")
            }
        }
    }

    @Suppress("unused")
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

    @Suppress("unused")
    object GroupCommand : SimpleCommand(owner = owner, "group", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    Bot.instances.forEach { bot ->
                        appendLine("--- ${bot.nick} ${bot.id} ---")
                        bot.groups.forEach { group ->
                            appendLine("$group -> <${group.name}>[${group.members.size}] ")
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    @Suppress("unused")
    object FriendCommand : SimpleCommand(owner = owner, "friend", description = "查看当前的好友") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    Bot.instances.forEach { bot ->
                        appendLine("--- ${bot.nick} ${bot.id} ---")
                        bot.friends.forEach { friend ->
                            appendLine("$friend -> <${friend.nick}>")
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    @Suppress("unused")
    object FriendRequestCommand : SimpleCommand(owner = owner, "friend-request", description = "接受好友") {

        private val friend by DebugRequestEventData::friend

        @Handler
        suspend fun CommandSender.handle(id: Long, accept: Boolean, black: Boolean) {
            runCatching {
                requireNotNull(friend.find { it.eventId == id }) { "找不到事件" }.also {
                    val event = it.toEvent()
                    if (accept) {
                        event.accept()
                    } else {
                        event.reject(black)
                    }
                }
            }.onSuccess {
                friend.removeIf { it.eventId == id }
                sendMessage("处理成功")
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    @Suppress("unused")
    object GroupRequestCommand : SimpleCommand(owner = owner, "group-request", description = "接受群") {

        private val group by DebugRequestEventData::group

        @Handler
        suspend fun CommandSender.handle(id: Long, accept: Boolean) {
            runCatching {
                requireNotNull(group.find { it.eventId == id }) { "找不到事件" }.also {
                    val event = it.toEvent()
                    if (accept) {
                        event.accept()
                    } else {
                        event.ignore()
                    }
                }
            }.onSuccess {
                group.removeIf { it.eventId == id }
                sendMessage("处理成功")
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    @Suppress("unused")
    object GarbageCommand : SimpleCommand(owner = owner, "gc", description = "垃圾回收") {

        @Handler
        suspend fun CommandSender.handle() {
            System.gc()
            sendMessage("GC完毕")
        }
    }
}





