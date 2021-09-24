package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.command.*
import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "xyz.cssxsh.mirai.plugin.debug-helper", version = "1.0.4") {
        name("debug-helper")
        author("cssxsh")
    }
) {
    @OptIn(ConsoleExperimentalApi::class)
    private fun <T : PluginConfig> T.save() = loader.configStorage.store(this@DebugHelperPlugin, this)

    override fun onEnable() {
        DebugSetting.reload()
        DebugSetting.save()
        DebugRequestEventData.reload()
        DebugOnlineConfig.reload()
        DebugOnlineConfig.save()
        DebugCommands.registerAll()

        logger.info("机器人所有者 ${DebugSetting.owner}")

        DebugListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        DebugListener.cancelAll()
    }
}