# [Debug Helper](https://github.com/gnuf0rce/debug-helper)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 调试小工具 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/debug-helper)](https://github.com/gnuf0rce/debug-helper/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/debug-helper/total)](https://shields.io/category/downloads)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/452)

**使用前应该查阅的相关文档或项目**

* [User Manual](https://github.com/mamoe/mirai/blob/dev/docs/UserManual.md)
* [Permission Command](https://github.com/mamoe/mirai/blob/dev/mirai-console/docs/BuiltInCommands.md#permissioncommand)
* [Chat Command](https://github.com/project-mirai/chat-command)

第一次运行之后请注意配置 [机器人所有者](#DebugSetting)  
机器人会在触发上线事件后向群聊发送上线卡片消息  
有好友申请和加群申请时，会记录事件信息，并联系机器人所有者

## 指令

注意: 使用前请确保可以 [在聊天环境执行指令](https://github.com/project-mirai/chat-command)   
`<...>`中的是指令名  
`[...]`表示参数，当`[...]`后面带`?`时表示参数可选  
`{...}`表示连续的多个参数

本插件指令权限ID 格式为 `xyz.cssxsh.mirai.plugin.debug-helper:command.*`, `*` 是指令的第一指令名  
例如 `/send 12345 上线 6789` 的权限ID为 `xyz.cssxsh.mirai.plugin.debug-helper:command.send`

### DebugCommands

| 指令                                           | 描述              |
|:---------------------------------------------|:----------------|
| `/<send-groups> [text] [atAll]?`             | 向所有群发送消息        |
| `/<at-all> [text] [group]?`                  | 向指定群发送AtAll     |
| `/<send> [contact] [text] [at]?`             | 向指定联系人发送消息      |
| `/<recall> [contact]?`                       | 尝试撤回消息          |
| `/<group>`                                   | 查看当前的群组         |
| `/<friend>`                                  | 查看当前的好友         |
| `/<request>`                                 | 查看申请列表          |
| `/<contact-request> [id] [accept]? [black]?` | 接受联系人申请         |
| `/<contact-delete> [contact]`                | 删除联系人           |
| `/<group-nick> [name] [group]?`              | 设置群名片           |
| `/<gc>`                                      | 主动触发 JVM GC     |
| `/<random-image> [contact]?`                 | 随机发送一张图片        |
| `/<forward> [contact] [title]?`              | 转发消息，句号结束       |
| `/<fork> [contact] {codes}`                  | 从mirai-code构造消息 |
| `/<rich> [content]`                          | 构造卡片消息          |
| `/<registered>`                              | 查看已注册指令         |

#### recall的使用

1. 不指定`contact`时，可以通过回复消息指定要撤销的消息，撤销回复消息指定的消息，如果没有指定，将尝试撤销最后一条不是由指令发送者发送的消息
2. `contact`是群员时，将尝试撤销这个群员的最后一条消息
3. `contact`是群或好友时，将尝试撤销bot的最后一条消息

#### contact-request的使用

1. `id` 是 事件id 或 好友id 或 群id
2. `accept` 和 `black` 参数为(不区分大小写) `true`, `yes`, `enabled`, `on`, `1` 时表示 `true`

## 权限

### Online Exclude

ID: `xyz.cssxsh.mirai.plugin.debug-helper:online.include`  
作用: 拥有此权限的群，会发送上线通知

## 配置

### DebugOnlineConfig

* duration 逐个发送消息延时，单位秒，默认 10s

### DebugSetting

* owner 机器人所有者，同时也是好友申请和加群申请的联系人
* auto_friend_request 自动同意好友请求
* auto_group_request 自动同意加群请求
* auto_member_accept 自动同意新成员请求
* auto_send_status 自动发送机器人状态到所有者的间隔，单位为分钟，为零时不开启此项功能
* auto_download_message 自动保存特殊消息内容，比如闪照
* random_image_api 随机图片API by <https://rainchan.win/projects/pximg>

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:debug-helper --channel stable --type plugin`

### 手动安装

* 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
* 从 [Releases](https://github.com/gnuf0rce/debug-helper/releases) 下载`jar`并将其放入`plugins`文件夹中