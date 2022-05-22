package io.github.gnuf0rce.mirai.debug.data

import net.mamoe.mirai.console.data.*

object DebugSetting : ReadOnlyPluginConfig("DebugSetting") {

    @ValueName("auto_download_message")
    @ValueDescription("自动保存特殊消息内容，比如闪照")
    val autoDownloadMessage by value(false)

    @ValueName("random_image_api")
    @ValueDescription("随即图片API by https://rainchan.win/projects/pximg")
    val randomImageApi by value("https://pximg.rainchan.win/img")
}