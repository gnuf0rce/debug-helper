/*
 * Copyright 2021-2022 dsstudio Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/gnuf0rce/debug-helper/blob/master/LICENSE
 */


package io.github.gnuf0rce.mirai.debug.data

import net.mamoe.mirai.console.data.*

object DebugSetting : ReadOnlyPluginConfig("DebugSetting") {

    @ValueName("auto_download_message")
    @ValueDescription("自动保存特殊消息内容，比如闪照")
    val autoDownloadMessage by value(false)

    @ValueName("random_image_api")
    @ValueDescription("随机图片API by https://rainchan.win/projects/pximg")
    val randomImageApi by value("https://pximg.rainchan.win/img")
}