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