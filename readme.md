# [Debug Helper](https://github.com/gnuf0rce/debug-helper)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 调试小工具 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/debug-helper)](https://github.com/gnuf0rce/debug-helper/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/debug-helper/total)](https://shields.io/category/downloads)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/452)

机器人会在触发上线事件后向群聊发送上线卡片消息  
有好友申请和加群申请时，会记录事件信息，并联系机器人所有者  

## 指令

注意: 使用前请确保可以 [在聊天环境执行指令](https://github.com/project-mirai/chat-command)   
`<...>`中的是指令名，由空格隔开表示或，选择其中任一名称都可执行例如`/send 12345 上线 6789`  
`[...]`表示参数，当`[...]`后面带`?`时表示参数可选  
`{...}`表示连续的多个参数

### DebugCommands

| 指令                                         | 描述                 |
|:---------------------------------------------|:---------------------|
| `/<send-groups> [text] [atAll]`              | 向所有群发送消息     |
| `/<at-all> [text] [group]?`                  | 向指定群发送AtAll    |
| `/<send> [contact] [text] [at]?`             | 向指定联系人发送消息 |
| `/<recall>`                                  | 尝试撤回消息         |
| `/<group>`                                   | 查看当前的群组       |
| `/<friend>`                                  | 查看当前的好友       |
| `/<request>`                                 | 查看申请列表         |
| `/<contact-request> [id] [accept]? [black]?` | 接受联系人申请       |
| `/<contact-delete> [friend]`                 | 删除联系人           |
| `/<group-nick> [name] [group]?`              | 设置群名片           |
| `/<gc>`                                      | 主动触发 JVM GC      |
| `/<random-image> [contact]?`                 | 随机发送一张图片     |

id 是事件id 或者 好友id 或者 群id  
contact 和 at 这两个参数可以是 数字号码 也可以是 @XXX  

## 权限

### Online Exclude

ID: `xyz.cssxsh.mirai.plugin.debug-helper:online.exclude`  
作用: 拥有此权限的群，不发送上线通知  

## 配置

### DebugOnlineConfig

* exclude 不开启上线信息的群号 (deprecated 1.0.1 移交权限系统管理)
* duration 逐个发送消息延时，单位秒，默认 10s (since 1.0.1)

### DebugSetting

* owner 机器人所有者，同时也是好友申请和加群申请的联系人
* auto_friend_request 自动同意好友请求
* auto_group_request 自动同意加群请求
* auto_send_status 自动发送机器人状态到所有者的间隔，单位为分钟，为零时不开启此项功能
* random_image_api 随即图片API by <https://rainchan.win/projects/pximg>

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:debug-helper --channel stable --type plugin`

### 手动安装

* 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
* 从 [Releases](https://github.com/gnuf0rce/debug-helper/releases) 下载`jar`并将其放入`plugins`文件夹中