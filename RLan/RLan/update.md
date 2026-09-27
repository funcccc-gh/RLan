# RLan 项目进程与功能清单

> 更新时间：2026-09-27  
> 当前版本：v0.1.0 (CLI)  
> 构建产物：`build/libs/RLan-0.1.0.jar`

---

## 一、项目概述

RLan 是一个基于 P2P 技术的远程局域网联机工具，支持最多 8 设备同时在线、IPv4/IPv6 双栈、房间密码鉴权、无需服务器中转。纯 P2P 架构，仅启动时一次性 STUN 查询公网地址，之后所有通信直连。

当前为**纯 CLI 版本**，无图形界面，通过命令行交互操作。

### 技术栈

| 组件 | 选型 |
|------|------|
| 构建工具 | Gradle 8.10.2 (Kotlin DSL) |
| 网络层 | Netty 4.1 (UDP) |
| 加密 | AES-256-GCM + SHA-256 |
| 测试 | JUnit 5 + AssertJ |
| 日志 | SLF4j + Logback |
| JVM | Java 21 (Azul Zulu) |

### 网络架构

- **纯 P2P 无服务器中转**：仅启动时一次性 STUN 查询公网地址
- **聊天星型拓扑**：成员 → 房主 → 广播，房主作中继避免成员间额外 NAT 打洞
- **STUN 服务器列表**：`stun.l.google.com:19302`、`stun1.l.google.com:19302`、`stun.qq.com:3478`、`stun.miwifi.com:3478`、`stun.syncthing.net:3478`
- **房间 ID 自包含地址**：`Base64URL(IP字节 + port(2字节) + seq(1字节))`，IPv4 仅 ~10 字符，加入者无需单独输入房主地址

---

## 二、版本历程

| 版本 | 日期 | 主要内容 |
|------|------|----------|
| v0.1.0 | 2026-09-27 | 纯 CLI 版本：移除 JavaFX UI，支持 create/join/close/info/shell/daemon 命令，聊天/文件传输/持久化/系统托盘/断联迁移/消息管理 |

---

## 三、功能清单

### 3.1 核心网络功能

- [x] **UDP P2P 连接** — 基于 Netty 的 UDP 通信，无需服务器中转
- [x] **STUN 公网地址发现** — 启动时查询多个 STUN 服务器获取公网映射地址
- [x] **NAT 穿透** — 利用 STUN 获取的公网地址直连
- [x] **IPv4/IPv6 双栈** — 支持仅 IPv4、仅 IPv6、双栈三种模式
- [x] **局域网自动发现** — 自动检测局域网 IP 地址
- [x] **虚拟网卡模拟** — 应用层模拟虚拟网卡 + IP 包转发
- [x] **消息编解码** — 二进制协议编解码 (MessageCodec)
- [x] **P2P 消息处理** — BiConsumer 保留来源地址 (P2pMessageHandler)
- [x] **节点注册表** — PeerRegistry 管理已知节点

### 3.2 房间管理

- [x] **创建房间** — 房主创建房间，自动生成房间 ID
- [x] **加入房间** — 通过房间 ID + 密码加入，5 秒超时
- [x] **离开房间** — 成员离开时通知房主 (LEAVE_ROOM 消息)
- [x] **关闭房间** — 房主关闭房间
- [x] **房间密码鉴权** — SHA-256 密码哈希校验
- [x] **房间容量控制** — 房主可设置最大设备数 (1-99，默认 8)
- [x] **踢出成员** — 房主可踢出指定成员 (KICK 消息)
- [x] **禁言/取消禁言** — 房主可禁言/取消禁言成员 (MUTE 消息)
- [x] **修改密码** — 房主修改密码触发房间迁移 (ROOM_MIGRATED 消息)
- [x] **房间迁移** — 修改密码后生成新房间 ID，自动通知所有成员迁移
- [x] **房间 ID 编码** — Base64URL(IP + port + seq)，自包含地址信息，IPv4 仅 ~10 字符
- [x] **房间 ID 内部映射** — UUID.nameUUIDFromBytes(host:port:seq) 用于内部查找

### 3.3 聊天功能

- [x] **文字消息** — 实时文字聊天
- [x] **文件传输** — 任意文件发送，自动保存到 `~/.rlan/downloads/`
- [x] **聊天消息持久化** — 聊天记录保存到 `~/.rlan/chats/<roomId>.log`
- [x] **聊天星型拓扑** — 成员 → 房主 → 广播，房主作中继
- [x] **文件消息记录** — 文件消息也记录到聊天日志（`[文件] filename` / `[图片] filename`）
- [x] **发送文件本地保存** — 发送文件时同时保存到 downloads 目录

### 3.4 CLI 命令

#### 单次命令

| 命令 | 说明 |
|------|------|
| `cli create <name> <password>` | 创建房间 |
| `cli join <roomId> <password>` | 加入房间 |
| `cli close <roomId> <password>` | 关闭房间 |
| `cli info` | 查看本机网络信息 |

#### 交互式 Shell

| 命令 | 说明 |
|------|------|
| `create <name> <password>` | 创建房间 |
| `join <roomId> <password>` | 加入房间 |
| `send <message>` | 向当前房间发送消息 |
| `file <path>` | 向当前房间发送文件 |
| `leave` | 离开当前房间 |
| `close` | 关闭当前房间(仅房主) |
| `rooms [roomId]` | 列出房间/查看指定房间详情 |
| `switch <roomId>` | 切换当前房间 |
| `members` | 查看当前房间成员列表(含用户ID、房主标记) |
| `message [n]` | 查看最近n条聊天记录(默认全部) |
| `message download [file] [path]` | 查看/另存已接收文件 |
| `message delete [n\|date <ymd>]` | 删除聊天记录(全部/最近n条/指定日期前) |
| `message filemaxsize [MB]` | 查看/设置文件大小限制 |
| `message textmaxlength [n]` | 查看/设置文字长度限制 |
| `message maxcount [n]` | 查看/设置消息数量限制 |
| `message autodelete [days]` | 查看/设置自动删除天数(0=关闭) |
| `kick <memberId>` | 踢出成员(仅房主) |
| `mute [memberId]` | 查看禁言列表/禁言解禁成员(仅房主) |
| `capacity [n]` | 查看/设置房间容量(仅房主) |
| `passwd <newPassword>` | 修改房间密码(仅房主) |
| `nick [nickname]` | 查看/设置当前房间昵称 |
| `quit` | 最小化到系统托盘(后台运行) |
| `exit` | 直接退出 |

#### Daemon 模式

- `cli daemon` — 后台运行，仅系统托盘，不读 stdin
- 托盘右键 "Exit" 退出 daemon

### 3.5 持久化与配置

- [x] **房间信息持久化** — `~/.rlan/rooms.properties`，重启后自动恢复房间
- [x] **聊天记录持久化** — `~/.rlan/chats/<roomId>.log`，每行一条消息
- [x] **用户设置持久化** — `~/.rlan/settings.properties`
  - [x] 昵称
  - [x] 默认房间容量
  - [x] 文件大小限制 (默认 10MB)
  - [x] 文字长度限制 (默认 4096)
  - [x] 消息数量限制 (默认 1000)
  - [x] 自动删除天数 (默认 0=关闭)
- [x] **自动恢复** — 重启后房主端自动重建房间，房客端自动重新加入
- [x] **断联迁移** — 每 30 秒检测公网地址变化，自动迁移房间并通知成员

### 3.6 消息管理

- [x] **文件大小限制** — 发送时检查文件大小，超限拒绝
- [x] **文字长度限制** — 发送时检查文字长度，超限拒绝
- [x] **消息数量限制** — 超量自动删除最旧消息
- [x] **自动过期删除** — 定时删除超过指定天数的消息
- [x] **手动删除** — 支持全部/最近n条/指定日期前删除
- [x] **文件另存** — `message download <file> <path>` 从 downloads 目录另存到指定路径

### 3.7 安全

- [x] **房间密码加密** — SHA-256 哈希存储
- [x] **房间 ID 安全** — Base64URL 编码，不含敏感信息明文
- [x] **密码修改迁移** — 修改密码生成全新房间 ID，旧 ID 失效

---

## 四、项目结构

```
RLan/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat / gradle/wrapper/
├── src/main/java/rlan/
│   ├── Main.java                       # 入口，调用 CliMain.run(args)
│   ├── CliMain.java                    # CLI 全部功能
│   ├── core/Core.java
│   ├── config/
│   │   ├── Config.java
│   │   └── UserSettings.java
│   ├── net/
│   │   ├── NetUtil.java
│   │   ├── p2p/                        # ConnectionManager, P2pEndpoint, P2pMessageHandler, MessageCodec, PeerRegistry, P2PNode
│   │   ├── stun/StunClient.java
│   │   ├── tunnel/                     # VirtualLan, IpPacket, PacketForwarder
│   │   └── voice/VoiceCall.java
│   ├── protocol/
│   │   ├── Packet.java
│   │   ├── MessageType.java
│   │   └── ChatProtocol.java
│   └── room/
│       ├── Room.java
│       ├── RoomManager.java
│       ├── JoinResult.java
│       ├── RoomMessage.java
│       ├── RoomService.java
│       ├── RoomHandle.java
│       ├── RoomCrypto.java
│       ├── RoomStore.java
│       └── ChatStore.java
├── src/main/resources/
│   └── logback.xml
└── src/test/java/rlan/
    ├── room/                           # RoomTest, RoomManagerTest, RoomIntegrationTest, RoomPasswordChangeTest
    └── net/tunnel/                     # VirtualLanTest, IpPacketTest
```

---

## 五、持久化文件

| 文件 | 路径 | 格式 | 说明 |
|------|------|------|------|
| 用户设置 | `~/.rlan/settings.properties` | Properties | 昵称/默认容量/消息限制 |
| 房间列表 | `~/.rlan/rooms.properties` | Properties | 房间名/角色/ID/密码/地址 (version=3) |
| 聊天记录 | `~/.rlan/chats/<roomId>.log` | 自定义 | 每行一条消息 |
| 下载文件 | `~/.rlan/downloads/` | 原始文件 | 发送和接收的文件副本 |

---

## 六、构建与运行

```bash
# 编译
export JAVA_HOME="/c/Java/zulu21-win_x64"
"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain compileJava --offline --no-daemon -q

# 测试
"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --offline --no-daemon

# 打包
"$JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain jar --offline -q

# 运行
java -jar build/libs/RLan-0.1.0.jar cli shell
java -jar build/libs/RLan-0.1.0.jar cli daemon
```

---

## 七、待办 / 未来方向

- [ ] 聊天消息加密传输
- [ ] 麦克风实时通话
- [ ] 跨平台适配与测试
