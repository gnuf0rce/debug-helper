package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.registerTo

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "xyz.cssxsh.mirai.plugin.debug-helper", version = "1.0.1") {
        name("debug-helper")
        author("cssxsh")
    }
) {
    private fun <T : PluginConfig> T.save() = loader.configStorage.store(this@DebugHelperPlugin, this)

    override fun onEnable() {
        DebugSetting.reload()
        DebugSetting.save()
        DebugRequestEventData.reload()
        DebugOnlineConfig.reload()
        DebugOnlineConfig.save()
        DebugCommands.registerAll()

        DebugListener.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        DebugListener.cancelAll()
    }
}