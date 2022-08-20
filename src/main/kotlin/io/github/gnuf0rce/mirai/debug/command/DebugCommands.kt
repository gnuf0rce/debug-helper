/*
 * Copyright 2021-2022 dsstudio Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/gnuf0rce/debug-helper/blob/master/LICENSE
 */


package io.github.gnuf0rce.mirai.debug.command

import io.github.gnuf0rce.mirai.debug.*
import io.github.gnuf0rce.mirai.debug.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.message.flags.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.net.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "unused")
object DebugCommands {

    private val all: List<Command> by lazy { this::class.nestedClasses.mapNotNull { it.objectInstance as? Command } }

    operator fun iterator(): Iterator<Command> = all.iterator()

    private val owner: CommandOwner get() = DebugHelperPlugin

    private val logger get() = DebugHelperPlugin.logger

    object AtAllCommand : SimpleCommand(owner, primaryName = "at-all", description = "全体@") {
        @Handler
        suspend fun CommandSender.handle(text: String, group: Group = subject as Group) {
            try {
                val message = AtAll + text + ForceAsLongMessage
                group.sendMessage(message)
            } catch (e: Throwable) {
                logger.warning({ "'${text}'发送失败" }, e)
                sendMessage("'${text}'发送失败")
            }
        }
    }

    object GarbageCommand : SimpleCommand(owner, primaryName = "gc", description = "垃圾回收") {
        @Handler
        suspend fun CommandSender.handle() {
            System.gc()
            sendMessage("GC完毕")
        }
    }

    object ImageCommand : SimpleCommand(owner, primaryName = "random-image", description = "随机图片") {
        private val http = HttpClient(OkHttp) {
            CurlUserAgent()
            ContentEncoding()
        }

        private val randomImageApi get() = DebugSetting.randomImageApi

        @Handler
        suspend fun CommandSender.handle(contact: Contact = subject as Contact) {
            val resource = http.get(randomImageApi).body<ByteArray>().toExternalResource()
            val message = try {
                val image: Image
                val upload = kotlin.system.measureTimeMillis {
                    image = contact.uploadImage(resource)
                }
                val send = kotlin.system.measureTimeMillis {
                    contact.sendMessage(image)
                }
                "upload: ${upload}ms, send: ${send}ms, url: ${image.queryUrl()}"
            } catch (cause: Throwable) {
                logger.warning({ "出现错误" }, cause)
                "出现错误"
            } finally {
                runInterruptible(Dispatchers.IO) {
                    resource.close()
                }
            }

            sendMessage(message)
        }
    }

    object ForwardCommand : SimpleCommand(owner, primaryName = "forward", description = "转发测试") {
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

    object ForkCommand : SimpleCommand(owner, primaryName = "fork", description = "从mirai-code构造消息") {
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
                logger.warning({ "出现错误" }, e)
                sendMessage("出现错误")
            }
        }
    }

    object RichCommand : SimpleCommand(owner, primaryName = "rich", description = "构造卡片消息") {
        private val SERVICE_ID = """(?im)(?<=serviceID=")\d+""".toRegex()

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
                logger.warning({ "出现错误" }, e)
                sendMessage("出现错误")
            } finally {
                logger.info { "卡片消息处理完成" }
            }
        }
    }

    object DeviceInfoCommand : SimpleCommand(owner, primaryName = "device", description = "设备信息") {
        @Handler
        suspend fun UserCommandSender.handle() {
            try {
                val forward = buildForwardMessage(subject) {
                    var count = 0
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = true
                    }
                    for (bot in Bot.instances) {
                        try {
                            val device = DeviceInfoManager.serialize(info = bot.configuration.deviceInfo!!(bot), json)
                            bot says device
                            count++
                        } catch (cause: Throwable) {
                            logger.warning({ "出现错误" }, cause)
                            bot says (cause.message ?: cause.toString())
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
                logger.warning({ "出现错误" }, e)
                sendMessage("出现错误")
            }
        }
    }

    object BackupCommand : SimpleCommand(owner, primaryName = "backup-data", description = "备份数据") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                val path = DebugHelperPlugin.backup().path
                sendMessage("Mirai-Console-Data 备份至 $path")
            } catch (e: Throwable) {
                logger.warning({ "出现错误" }, e)
                sendMessage("出现错误")
            }
        }
    }

    object ClassCommand : SimpleCommand(owner, primaryName = "class-loader", description = "类加载器测试") {
        @Handler
        @OptIn(MiraiInternalApi::class)
        suspend fun CommandSender.handle(id: String, name: String) {
            val plugin = PluginManager.plugins.filterIsInstance<AbstractJvmPlugin>()
                .find { it.id == id || it.name == id }
                ?: kotlin.run {
                    sendMessage("jvm plugin $id")
                    return
                }

            val clazz = plugin.loader.findLoadedClass(name) ?: kotlin.run {
                sendMessage("Not Found Class $name")
                return
            }

            val classLoader = clazz.classLoader as URLClassLoader

            val message = classLoader.urLs.joinToString(separator = "\n", prefix = "$classLoader: \n")
            sendMessage(message)
        }
    }

    object PropertyCommand : SimpleCommand(owner, primaryName = "system-property", description = "System.setProperty") {
        @Handler
        suspend fun CommandSender.handle(key: String, value: String) {
            System.setProperty(key, value)
            sendMessage("$key - $value  设置完成")
        }
    }
}