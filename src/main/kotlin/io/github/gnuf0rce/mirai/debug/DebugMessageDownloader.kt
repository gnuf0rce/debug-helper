package io.github.gnuf0rce.mirai.debug

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.console.util.MessageUtils.firstContentOrNull
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*

object DebugMessageDownloader : SimpleListenerHost() {

    private val logger get() = DebugHelperPlugin.logger

    private val http = HttpClient(OkHttp)

    private val folder get() = DebugHelperPlugin.dataFolder

    private fun download(message: MessageChain) = launch(SupervisorJob()) {
        when (val target = message.firstContentOrNull()) {
            is FlashImage -> {
                try {
                    folder.resolve("flash")
                        .resolve("${message.source.fromId}")
                        .resolve(target.image.imageId)
                        .apply { parentFile.mkdirs() }
                        .writeBytes(http.get(target.image.queryUrl()))
                } catch (cause: Throwable) {
                    logger.warning({ "$target 下载失败" }, cause)
                }
            }
            is OnlineAudio -> {
                try {
                    folder.resolve("audio")
                        .resolve("${message.source.fromId}")
                        .resolve(target.filename)
                        .apply { parentFile.mkdirs() }
                        .writeBytes(http.get(target.urlForDownload))
                } catch (cause: Throwable) {
                    logger.warning({ "$target 下载失败" }, cause)
                }
            }
            is RichMessage -> {
                try {
                    val format = when (target.content[0]) {
                        '<' -> "xml"
                        '{' -> "json"
                        else -> "rich"
                    }
                    folder.resolve("service")
                        .resolve("${message.source.fromId}")
                        .resolve("${message.source.time}.${format}")
                        .apply { parentFile.mkdirs() }
                        .writeText(target.content)
                } catch (cause: Throwable) {
                    logger.warning({ "$target 下载失败" }, cause)
                }
            }
            else -> Unit
        }
    }

    @EventHandler
    fun MessageEvent.mark() {
        download(message)
    }
}