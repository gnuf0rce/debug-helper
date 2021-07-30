# [Debug Helper](https://github.com/gnuf0rce/debug-helper)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的RSS订阅插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/debug-helper)](https://github.com/gnuf0rce/debug-helper/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/debug-helper/total)](https://shields.io/category/downloads)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/452)

## 指令

注意: 使用前请确保可以 [在聊天环境执行指令](https://github.com/project-mirai/chat-command)   
`<...>`中的是指令名，由空格隔开表示或，选择其中任一名称都可执行例如`/send 12345 上线 6789`  
`[...]`表示参数，当`[...]`后面带`?`时表示参数可选  
`{...}`表示连续的多个参数

### DebugCommands

| 指令                              | 描述                 |
|:----------------------------------|:---------------------|
| `/<send-groups> [text] [atAll]`   | 向所有群发送消息     |
| `/<send> [contact] [text] [at]?`  | 向指定联系人发送消息 |
| `/<recall>`                       | 尝试撤回消息         |
| `/<group>`                        | 查看当前的群组       |
| `/<friend>`                       | 查看当前的好友       |
| `/<request>`                      | 查看申请列表         |
| `/<friend-request> [id] [black]?` | 接受好友申请         |
| `/<group-request> [id]`           | 接受群申请           |
| `/<gc>`                           | 主动触发 JVM GC      |

contact 和 at 这两个参数可以是 数字号码 也可以是 @XXX

## 配置

### DebugOnlineConfig

1. exclude 不开启上线信息的群号

### DebugSetting

1. owner 机器人所有者，同时也是好友申请和加群申请的联系人