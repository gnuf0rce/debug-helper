/*
 * Copyright 2021-2022 dsstudio Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/gnuf0rce/debug-helper/blob/master/LICENSE
 */


package io.github.gnuf0rce.mirai.debug

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*
import java.io.File

object DebugMessageDownloader : SimpleListenerHost() {

    private val logger: MiraiLogger = MiraiLogger.Factory.create(this::class, "debug-helper.downloader")

    private val http = HttpClient(OkHttp) {
        CurlUserAgent()
        ContentEncoding()
    }

    internal var folder = File("./cache")

    private fun download(message: MessageChain) = launch(SupervisorJob()) {
        when (val target = message.findIsInstance<MessageContent>()) {
            is FlashImage -> {
                try {
                    val url = target.image.queryUrl()
                    val file = folder.resolve("flash")
                        .resolve("${message.source.fromId}")
                        .resolve(target.image.imageId)
                    file.parentFile.mkdirs()
                    file.writeBytes(http.get(url).body())
                } catch (cause: Throwable) {
                    logger.warning({ "$target 下载失败" }, cause)
                }
            }
            is OnlineAudio -> {
                try {
                    val url = target.urlForDownload
                    val file = folder.resolve("audio")
                        .resolve("${message.source.fromId}")
                        .resolve(target.filename)
                    file.parentFile.mkdirs()
                    file.writeBytes(http.get(url).body())
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
                    val file = folder.resolve("service")
                        .resolve("${message.source.fromId}")
                        .resolve("${message.source.time}.${format}")
                    file.parentFile.mkdirs()
                    file.writeText(target.content)
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