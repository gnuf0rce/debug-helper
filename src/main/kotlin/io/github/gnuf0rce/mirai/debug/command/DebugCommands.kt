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
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.descriptor.*
import net.mamoe.mirai.console.internal.plugin.*
import net.mamoe.mirai.console.internal.util.*
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.message.flags.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.*
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
            } catch (cause: Exception) {
                logger.warning({ "'${text}'发送失败" }, cause)
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
            try {
                val image: Image
                val upload = kotlin.system.measureTimeMillis {
                    image = contact.uploadImage(resource)
                }
                val send = kotlin.system.measureTimeMillis {
                    contact.sendMessage(image)
                }
                logger.info { "size: ${resource.size} upload: ${upload}ms, send: ${send}ms, url: ${image.queryUrl()}" }
            } catch (cause: Exception) {
                logger.warning({ "出现错误" }, cause)
            } finally {
                runInterruptible(Dispatchers.IO) {
                    resource.close()
                }
            }
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
            } catch (cause: Exception) {
                logger.warning({ "出现错误" }, cause)
                sendMessage("出现错误")
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
            } catch (cause: Exception) {
                logger.warning({ "出现错误" }, cause)
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
            } catch (cause: Exception) {
                logger.warning({ "出现错误" }, cause)
                sendMessage("出现错误")
            } finally {
                logger.info { "卡片消息处理完成" }
            }
        }
    }

    object BackupCommand : SimpleCommand(owner, primaryName = "backup-data", description = "备份数据") {
        @Handler
        suspend fun CommandSender.handle() {
            try {
                val path = DebugHelperPlugin.backup().path
                sendMessage("Mirai-Console-Data 备份至 $path")
            } catch (cause: Exception) {
                logger.warning({ "出现错误" }, cause)
                sendMessage("出现错误")
            }
        }
    }

    object DependencyCommand : SimpleCommand(owner, primaryName = "dependency", description = "plugins dependency") {
        @OptIn(ConsoleFrontEndImplementation::class)
        @Handler
        suspend fun CommandSender.handle() {
            if (isConsole()) throw CommandArgumentParserException("isConsole")
            val message = buildMessageChain {
                for (plugin in MiraiConsole.pluginManager.plugins) {
                    if (plugin !is JvmPlugin) continue
                    appendLine("=== ${plugin.id} ${plugin.version} ===")
                    val classLoader = plugin.javaClass.classLoader as JvmPluginClassLoaderN
                    appendLine("--- depend ---")
                    for (dependency in classLoader.dependencies) {
                        for (dep in dependency.sharedClLoadedDependencies) {
                            appendLine(dep)
                        }
                    }
                    appendLine("--- shared ---")
                    for (dep in classLoader.sharedClLoadedDependencies) {
                        appendLine(dep)
                    }
                    appendLine("--- private ---")
                    for (dep in classLoader.privateClLoadedDependencies) {
                        appendLine(dep)
                    }
                }
            }

            sendMessage(message = message)
        }
    }

//    object ReLoadCommand : SimpleCommand(owner, primaryName = "reload", description = "reload plugin") {
//        @OptIn(ConsoleFrontEndImplementation::class)
//        @Suppress("UNCHECKED_CAST")
//        @Handler
//        suspend fun CommandSender.handle(id: String) {
//            val plugins = (MiraiConsole.pluginManager as PluginManagerImpl).resolvedPlugins
//            val loader = MiraiConsoleImplementation.getInstance().jvmPluginLoader as BuiltInJvmPluginLoaderImpl
//
//            val plugin = plugins.filterIsInstance<AbstractJvmPlugin>()
//                .find { it.id == id || it.name == id }
//                ?: kotlin.run {
//                    sendMessage("jvm plugin $id not found.")
//                    return
//                }
//            val classLoader = plugin.javaClass.classLoader as JvmPluginClassLoaderN
//            val jar = classLoader.file
//            val cache = loader::class.java.getDeclaredField("pluginFileToInstanceMap")
//                .apply { isAccessible = true }
//                .get(loader) as MutableMap<File, JvmPlugin>
//
//            PluginManager.disablePlugin(plugin = plugin)
//            try {
//                plugin.cancel()
//            } catch (cause: Exception) {
//                logger.error({ "jvm plugin $id cancel throw exception." }, cause)
//            }
//            plugins.remove(plugin)
//            try {
//                val permissions = PermissionService.INSTANCE.javaClass.getDeclaredField("permissions")
//                    .apply { isAccessible = true }
//                    .get(PermissionService.INSTANCE) as MutableMap<*, *>
//                permissions.remove(plugin.parentPermission.id)
//                permissions.remove(plugin.parentPermission.id.run { "$namespace.$name" })
//            } catch (cause: Exception) {
//                logger.error({ "jvm plugin $id permission remove exception." }, cause)
//            }
//            try {
//                runInterruptible(Dispatchers.IO) {
//                    classLoader.close()
//                }
//            } catch (cause: Exception) {
//                logger.error({ "jvm plugin $id class loader close exception." }, cause)
//            }
//            cache.remove(jar)
//            loader.classLoaders.remove(classLoader)
//
//            val name = jar.name.substringBefore('-')
//            val newJar = jar.parentFile
//                .listFiles { _, filename -> filename.startsWith(name) }
//                .maxBy { it.lastModified() }
//            val newClassLoader = JvmPluginClassLoaderN.newLoader(newJar, loader.jvmPluginLoadingCtx)
//            loader.classLoaders.add(newClassLoader)
//            // exportManagers
//            val newPlugin = with(PluginServiceHelper) {
//                val single = newClassLoader.findServices(JvmPlugin::class, KotlinPlugin::class, JavaPlugin::class)
//                    .loadAllServices().single()
//
//                newClassLoader.linkedLogger = single.logger
//                cache[jar] = single
//                single
//            }
//            plugins.add(newPlugin)
//            PluginManager.loadPlugin(plugin = newPlugin)
//            PluginManager.enablePlugin(plugin = newPlugin)
//
//
//            sendMessage("jvm plugin $id reloaded.")
//        }
//    }

    object PropertyCommand : SimpleCommand(owner, primaryName = "system-property", description = "System.setProperty") {
        @Handler
        suspend fun CommandSender.handle(key: String, value: String) {
            if (value != "null") {
                System.setProperty(key, value)
                sendMessage("$key - $value 设置完成")
            } else {
                System.clearProperty(key)
                sendMessage("$key 移除完成")
            }
        }
    }

    object CookieCommand : SimpleCommand(owner, primaryName = "cookie", description = "Bot Cookie") {
        @Handler
        suspend fun CommandSender.handle() {
            sendMessage(buildMessageChain {
                for (bot in Bot.instances) {
                    appendLine("=== ${bot.id} ===")

                    bot as net.mamoe.mirai.internal.QQAndroidBot
                    val info = bot.client.wLoginSigInfo

                    appendLine("bkn: ${info.bkn}")
                    appendLine("sKey: ${info.sKey.data.decodeToString()}")
                    appendLine("psKey: ")
                    for ((host, value) in info.psKeyMap) {
                        appendLine("    $host - ${value.data.decodeToString()}")
                    }
                    appendLine("pt4Token: ")
                    for ((host, value) in info.pt4TokenMap) {
                        appendLine("    $host - ${value.data.decodeToString()}")
                    }
                    appendLine("payToken: ${info.payToken.toUHexString()}")
                }
            })
        }
    }
}