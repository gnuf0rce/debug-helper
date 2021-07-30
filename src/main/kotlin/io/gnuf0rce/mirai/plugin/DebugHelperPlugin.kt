package io.gnuf0rce.mirai.plugin

import io.gnuf0rce.mirai.plugin.data.*
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(id = "xyz.cssxsh.mirai.plugin.debug-helper", version = "1.0.0") {
        name("debug-helper")
        author("cssxsh")
    }
) {

    override fun onEnable() {
        DebugSetting.reload()
        DebugRequestEventData.reload()
        DebugOnlineConfig.reload()
        DebugCommands.registerAll()

        DebugSubscriber.start()
    }

    override fun onDisable() {
        DebugSubscriber.stop()
    }
}