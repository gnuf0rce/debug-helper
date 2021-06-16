package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.toPlainText

@ConsoleExperimentalApi
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
        DebugCommands.registerAll()

        globalEventChannel().run {
            subscribeAlways<NewFriendRequestEvent> {
                accept()
            }
            subscribeAlways<BotInvitedJoinGroupRequestEvent> {
                runCatching {
                    if (invitorId in bot.getGroupOrFail(587589416L)) {
                        accept()
                        logger.info("accept: $invitorId -> $groupId")
                    } else {
                        ignore()
                        logger.info("ignore: $invitorId -> $groupId")
                    }
                }.onFailure(logger::warning)
            }
            subscribeGroupMessages {
                val permissionId = PermissionId.parseFromString(string = "xyz.cssxsh.mirai.plugin.pixiv-helper:command.tag")
                val sign = Image(imageId = "{8671BFB2-C786-62F5-72BE-ACD42B0A738B}.jpg")
                val sub = arrayOf("当当快更新")
                containsAll(sub = sub) and at(1506301834) and sentFrom(312783817) reply {
                    if (sender.permitteeId.hasPermission(permissionId)) {
                        null
                    } else {
                        sender.permitteeId.permit(permissionId)
                        launch {
                            delay(10 * 60 * 1000)
                            group.sendMessage("权限将过期 ".toPlainText() + At(sender))
                            sender.permitteeId.cancel(permissionId, false)
                        }
                        "鉴于你的勇敢，给你十分钟TAG权限 ".toPlainText() + At(sender)
                    }
                }
                contains("tag ") and sentFrom(312783817) reply {
                    if (sender.permitteeId.hasPermission(permissionId)) {
                        null
                    } else {
                        "你没有权限但是可以如图申请 ".toPlainText() + sign + At(sender)
                    }
                }
            }
        }
    }
}