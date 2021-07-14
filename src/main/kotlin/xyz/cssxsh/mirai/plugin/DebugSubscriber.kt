package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.plugin.data.*

@ConsoleExperimentalApi
object DebugSubscriber: CoroutineScope by DebugHelperPlugin.childScope("debug-subscriber") {

    private val logger by DebugHelperPlugin::logger

    private val owner by DebugSetting::owner

    private var friend by DebugRequestEventData::friend

    private var group by DebugRequestEventData::group

    fun start() {
        globalEventChannel().apply {
            subscribeAlways<NewFriendRequestEvent> {
                friend = friend + it
                runCatching {
                    bot.getFriendOrFail(owner).sendMessage(buildMessageChain {
                        appendLine("@${fromNick}#${fromId}")
                        appendLine("申请添加好友")
                        appendLine("from $fromGroup")
                        appendLine(message)
                    })
                }.onFailure {
                    logger.warning { "发送消息失败，$it" }
                }
            }
            //
            subscribeAlways<BotInvitedJoinGroupRequestEvent> {
                group = group + it

                runCatching {
                    bot.getFriendOrFail(owner).sendMessage(buildMessageChain {
                        appendLine("@${invitorNick}#${invitorId}")
                        appendLine("申请添加群")
                        appendLine("to [$groupName](${groupId})")
                    })
                }.onFailure {
                    logger.warning { "发送消息失败，$it" }
                }

            }
            //
        }
    }

    fun stop() {
        coroutineContext.cancelChildren()
    }
}