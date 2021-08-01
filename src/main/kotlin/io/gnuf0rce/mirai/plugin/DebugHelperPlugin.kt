package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.data.PluginConfig
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

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

        DebugSubscriber.start()
    }

    override fun onDisable() {
        DebugSubscriber.stop()
    }
}