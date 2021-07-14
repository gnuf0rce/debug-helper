package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import xyz.cssxsh.mirai.plugin.data.*

object DebugHelperPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.debug-helper",
        name = "debug-helper",
        version = "0.1.0-dev-1",
    ) {
        author("cssxsh")
    }
) {

    override fun onEnable() {
        DebugSetting.reload()
        DebugRequestEventData.reload()
        DebugCommands.registerAll()

        DebugSubscriber.start()
    }

    override fun onDisable() {
        DebugSubscriber.stop()
    }
}