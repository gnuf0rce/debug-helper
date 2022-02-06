package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.command.*
import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "io.gnuf0rce.mirai.plugin.debug-helper", version = "1.2.2") {
        name("debug-helper")
        author("cssxsh")
    }
) {

    override fun onEnable() {
        DebugSetting.reload()
        for (command in DebugCommands) command.register()

        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }
        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }
        logger.warning { "本插件中机器人管理的相关功能已经拆分至 https://github.com/cssxsh/mirai-administrator" }

        if (DebugSetting.autoDownloadMessage) {
            logger.info { "自动保存特殊消息内容开启" }
            DebugMessageDownloader.registerTo(globalEventChannel())
        }
    }

    override fun onDisable() {
        DebugMessageDownloader.cancelAll()

        for (command in DebugCommands) command.unregister()
    }
}