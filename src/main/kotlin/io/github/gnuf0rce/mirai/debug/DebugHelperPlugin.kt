/*
 * Copyright 2021-2022 dsstudio Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/gnuf0rce/debug-helper/blob/master/LICENSE
 */


package io.github.gnuf0rce.mirai.debug

import io.github.gnuf0rce.mirai.debug.command.*
import io.github.gnuf0rce.mirai.debug.data.*
import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.io.*
import java.nio.file.*
import java.time.*
import java.util.zip.*

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "io.github.gnuf0rce.debug-helper", version = "1.3.5") {
        name("debug-helper")
        author("cssxsh")
    }
) {

    fun backup(): File {
        val backup = File("./backup/${LocalDate.now()}.${System.currentTimeMillis()}.zip")
        backup.parentFile.mkdirs()
        val extensions = listOf("json", "yml")
        val paths = listOf(PluginManager.pluginsConfigPath, PluginManager.pluginsDataPath, Path.of("./bots"))
        val buffer = 1 shl 23
        val buffered = backup.outputStream().buffered(buffer)
        ZipOutputStream(buffered).use { output ->
            for (path: Path in paths) {
                val begin = path.nameCount - 1
                for (folder in path.toFile().listFiles() ?: continue) {
                    for (file in folder.listFiles() ?: continue) {
                        if (!file.isFile) continue
                        if (file.extension !in extensions) continue
                        if (file.length() > buffer) continue

                        val current = file.toPath()
                        val name = current.subpath(begin, current.nameCount).toString()
                        output.putNextEntry(ZipEntry(name).apply { time = file.lastModified() })
                        file.inputStream().use { input -> input.transferTo(output) }
                        output.flush()
                    }
                }
            }
        }
        buffered.close()

        return backup
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    fun protocol() {
        for (protocol in BotConfiguration.MiraiProtocol.values()) {
            val file = resolveDataFile("${protocol.name}.txt")
            if (file.exists().not()) continue
            logger.warning { "将尝试修改 Mirai 内部协议信息 ${protocol.name} by ${file.path}" }
            val lines = file.readLines()
            val iterator = lines.iterator()
            MiraiProtocolInternal.protocols[protocol] = MiraiProtocolInternal(
                apkId = iterator.next(),
                id = iterator.next().toLong(),
                ver = iterator.next(),
                sdkVer = iterator.next(),
                miscBitMap = iterator.next().toInt(),
                subSigMap = iterator.next().toInt(),
                mainSigMap = iterator.next().toInt(),
                sign = iterator.next(),
                buildTime = iterator.next().toLong(),
                ssoVersion = iterator.next().toInt(),
             )
        }
    }

    override fun PluginComponentStorage.onLoad() {
        backup()
        protocol()
    }

    override fun onEnable() {
        DebugSetting.reload()
        for (command in DebugCommands) command.register()

        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }
        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }
        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }

        if (DebugSetting.autoDownloadMessage) {
            logger.info { "自动保存特殊消息内容开启" }
            DebugMessageDownloader.folder = DebugHelperPlugin.dataFolder
            DebugMessageDownloader.registerTo(globalEventChannel())
        }
    }

    override fun onDisable() {
        DebugMessageDownloader.cancel()

        for (command in DebugCommands) command.unregister()
    }
}