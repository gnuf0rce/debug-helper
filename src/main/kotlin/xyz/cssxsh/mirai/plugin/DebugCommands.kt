package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.CoroutineScope
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import okio.ByteString.Companion.decodeHex

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
                Bot.instances.flatMap(Bot::groups).sendMessage("机器人关闭调试一下 $message")
            }.onSuccess {
                sendMessage("'${message}'发送成功")
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
            }.onSuccess {
                sendMessage("'${message}'发送成功")
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
                fromEvent.message.firstIsInstance<QuoteReply>()
            }.mapCatching {
                it.recallSource()
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
                            appendLine("$group -> [${group.members.size}] ${group.name}")
                        }
                    }
                })
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }

    @Suppress("unused")
    object FriendCommand : SimpleCommand(owner = owner, "friend", description = "查看当前的群组") {
        @Handler
        suspend fun CommandSender.handle() {
            runCatching {
                sendMessage(buildMessageChain {
                    Bot.instances.forEach { bot ->
                        appendLine("--- ${bot.nick} ${bot.id} ---")
                        bot.friends.forEach { friend ->
                            appendLine("$friend -> ${friend.nick}")
                        }
                    }
                })
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

    private val MarketFaceImplKClass by lazy {
        Class.forName("net.mamoe.mirai.internal.message.MarketFaceImpl").kotlin
    }

    private val MarketFaceBodyKClass by lazy {
        Class.forName("net.mamoe.mirai.internal.network.protocol.data.proto.ImMsgBody").kotlin.nestedClasses.first {
            it.simpleName == "MarketFace"
        }
    }

    private fun MarketFace(id: Int, key: String, md5: String): MarketFace {
        val body = MarketFaceBodyKClass.constructors.first { it.parameters.size == 13 }.run {
            callBy(parameters.associateWith { parameter ->
                when (parameter.name) {
                    "faceId" -> md5.decodeHex().toByteArray()
                    "faceInfo" -> 1
                    "faceName" -> "[表情测试]".toByteArray()
                    "imageHeight" -> 200
                    "imageWidth" -> 200
                    "itemType" -> 6
                    "key" -> key.toByteArray()
                    "mediaType" -> 0
                    "mobileParam" -> byteArrayOf()
                    "param" -> byteArrayOf()
                    "pbReserve" -> byteArrayOf()
                    "subType" -> 3
                    "tabId" -> id
                    else -> throw IllegalArgumentException()
                }
            })
        }
        return MarketFaceImplKClass.constructors.first { it.parameters.size == 1 }.call(body) as MarketFace
    }

    @Suppress("unused")
    object MarketFaceCommand : SimpleCommand(owner = owner, "face", description = "发送一个表情") {
        @Handler
        suspend fun CommandSenderOnMessage<*>.handle(id: Int, key: String, md5: String) {
            runCatching {
                sendMessage(MarketFace(id, key, md5))
            }.onFailure {
                sendMessage("出现错误 $it")
            }
        }
    }
}





