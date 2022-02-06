# [Debug Helper](https://github.com/gnuf0rce/debug-helper)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 调试小工具 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/debug-helper)](https://github.com/gnuf0rce/debug-helper/releases)
![Downloads](https://img.shields.io/github/downloads/gnuf0rce/debug-helper/total)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/452)

**使用前应该查阅的相关文档或项目**

* [User Manual](https://github.com/mamoe/mirai/blob/dev/docs/UserManual.md)
* [Permission Command](https://github.com/mamoe/mirai/blob/dev/mirai-console/docs/BuiltInCommands.md#permissioncommand)
* [Chat Command](https://github.com/project-mirai/chat-command)

`1.2.2` 起
本插件中机器人管理的相关功能已经拆分至 [mirai-administrator](https://github.com/cssxsh/mirai-administrator)  
请注意相关权限的重新处理。

## 指令

注意: 使用前请确保可以 [在聊天环境执行指令](https://github.com/project-mirai/chat-command)   
`<...>`中的是指令名  
`[...]`表示参数，当`[...]`后面带`?`时表示参数可选  
`{...}`表示连续的多个参数

本插件指令权限ID 格式为 `io.gnuf0rce.mirai.plugin.debug-helper:command.*`, `*` 是指令的第一指令名  
例如 `/device` 的权限ID为 `io.gnuf0rce.mirai.plugin.debug-helper:command.device`

### DebugCommands

| 指令                                           | 描述              |
|:---------------------------------------------|:----------------|
| `/<at-all> [text] [group]?`                  | 向指定群发送AtAll     |
| `/<gc>`                                      | 主动触发 JVM GC     |
| `/<random-image> [contact]?`                 | 随机发送一张图片        |
| `/<forward> [contact] [title]?`              | 转发消息，句号结束       |
| `/<fork> [contact] {codes}`                  | 从mirai-code构造消息 |
| `/<rich> [content]`                          | 构造卡片消息          |
| `/<device>`                                  | 查看 Bot 设备信息     |

### DebugSetting

* auto_download_message 自动保存特殊消息内容，比如闪照
* random_image_api 随机图片API by <https://rainchan.win/projects/pximg>

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:debug-helper --channel stable --type plugin`

### 手动安装

* 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
* 从 [Releases](https://github.com/gnuf0rce/debug-helper/releases) 下载`jar`并将其放入`plugins`文件夹中