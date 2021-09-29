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
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.message.*
import java.io.InputStream

@OptIn(ConsoleExperimentalApi::class)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
object DebugCommands : CoroutineScope by DebugHelperPlugin.childScope("debug-command") {

    private val all by lazy { this::class.nestedClasses.mapNotNull { it.objectInstance as? Command } }

    fun registerAll() = all.associateWith { it.register() }

    private val owner: CommandOwner get() = DebugHelperPlugin

    private val logger get() = DebugHelperPlugin.logger

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

    object GroupCommand : SimpleCommand(owner = owner, "group", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    Bot.instances.forEach { bot ->
                        appendLine("--- ${bot.nick} ${bot.id} ---")
                        bot.groups.forEach { group ->
                            appendLine("(${group.id})[${group.botPermission}] -> <${group.name}>[${group.members.size}](${group.botMuteRemaining})")
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    private val friend by DebugRequestEventData::friend

    private val group by DebugRequestEventData::group

    object RequestListCommand : SimpleCommand(owner = owner, "request", description = "申请列表") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    appendLine("Friend")
                    appendLine(friend.joinToString("\n") {
                        "Bot: ${it.bot}, EventId: ${it.eventId}, message: ${it.message}, QQ: @${it.fromNick}#${it.fromId}, Group: ${it.fromGroupId}"
                    })
                    appendLine("Group")
                    appendLine(group.joinToString("\n") {
                        "Bot: ${it.bot}, EventId: ${it.eventId}, QQ: @${it.invitorNick}#${it.invitorId}, Group: ${it.groupName}#${it.groupId}"
                    })
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    object ContactRequestCommand : SimpleCommand(owner = owner, "contact-request", description = "接受联系人") {
        @Handler
        suspend fun CommandSender.handle(id: Long, accept: Boolean = true, black: Boolean = false) {
            runCatching {
                val data = friend.find { it.eventId == id || it.fromId == id }?.apply {
                    with(toEvent()) {
                        if (accept) accept() else reject(black)
                    }
                    friend.removeIf { eventId == it.eventId || fromId == it.fromId }
                    sendMessage("@${fromNick}#${fromId} 处理成功")
                } ?: group.find { it.eventId == id || it.groupId == id }?.apply {
                    with(toEvent()) {
                        if (accept) accept() else ignore()
                    }
                    group.removeIf { eventId == it.eventId || groupId == it.groupId }
                    sendMessage("@${invitorNick}#${invitorId} to ${groupName}#${groupId} 处理成功")
                }
                requireNotNull(data) { "找不到事件" }
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
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle() {
            runCatching {
                HttpClient(OkHttp).use { http ->
                    http.get<InputStream>("https://pximg.rainchan.win/img").use { input ->
                        subject!!.sendImage(input)
                    }
                }
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }
}





