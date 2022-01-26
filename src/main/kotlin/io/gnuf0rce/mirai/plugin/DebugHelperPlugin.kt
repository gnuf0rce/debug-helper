package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.command.*
import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "xyz.cssxsh.mirai.plugin.debug-helper", version = "1.2.1") {
        name("debug-helper")
        author("cssxsh")
    }
) {

    override fun onEnable() {
        DebugSetting.reload()
        DebugSetting.save()
        DebugRequestEventData.reload()
        DebugOnlineConfig.reload()
        DebugOnlineConfig.save()
        for (command in DebugCommands) {
            command.register()
        }

        if (DebugSetting.owner != DebugSetting.OwnerDefault) {
            logger.info("机器人所有者 ${DebugSetting.owner}")
        } else {
            logger.warning("机器人所有者 未设置")
        }
        logger.info("发送上线通知请使用 /perm add g群号 xyz.cssxsh.mirai.plugin.debug-helper:online.include 赋予权限")

        DebugListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        DebugListener.cancelAll()
        for (command in DebugCommands) {
            command.unregister()
        }
    }
}