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

本插件指令权限ID 格式为 `io.github.gnuf0rce.debug-helper:command.*`, `*` 是指令的第一指令名  
例如 `/device` 的权限ID为 `io.github.gnuf0rce.debug-helper:command.device`  
插件启动后将会备份一次数据

### DebugCommands

| 指令                                 | 描述                 |
|:-----------------------------------|:-------------------|
| `/<at-all> [text] [group]?`        | 向指定群发送 AtAll       |
| `/<gc>`                            | 主动触发 JVM GC        |
| `/<random-image> [contact]?`       | 随机发送一张图片           |
| `/<forward> [contact] [title]?`    | 转发消息，句号结束          |
| `/<fork> [contact] {codes}`        | 从mirai-code构造消息    |
| `/<rich> [content]`                | 构造卡片消息             |
| `/<device>`                        | 查看 Bot 设备信息        |
| `/<backup-data>`                   | 备份数据               |
| `/<reload> [id]`                   | 热重载插件              |
| `/<system-property> [key] [value]` | 设置 system-property |

### DebugSetting

* auto_download_message 自动保存特殊消息内容，比如闪照
* random_image_api 随机图片API by <https://rainchan.win/projects/pximg>

### 修改协议内容

在 `data/io.github.gnuf0rce.debug-helper/` 下新建文本文件 `ANDROID_PHONE.txt` (可根据需要修改的协议名更改文件名)  

填入例如 (注意这是 ANDROID_PHONE 的协议内容，如果需要其他协议，请自行解决)
```text
com.tencent.mobileqq
537066978
0.9.15.9425
6.0.0.2463
150470524
66560
16724722
A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D
1640921786
16
```

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:debug-helper --channel stable --type plugin`

### 手动安装

* 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
* 从 [Releases](https://github.com/gnuf0rce/debug-helper/releases) 下载`jar`并将其放入`plugins`文件夹中