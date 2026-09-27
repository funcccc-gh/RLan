# RLan

> 基于 P2P 技术的远程局域网联机工具，让跨网络的设备如同处于同一局域网般进行游戏联机。

## 项目简介

RLan 旨在通过点对点（P2P）技术，在公网环境下为多台设备建立一张"远程虚拟局域网"。玩家无需处于同一物理网络，也无需依赖中心化转发服务器，即可像局域网联机一样进行游戏。

当前为**纯 CLI 版本**，无图形界面，通过命令行交互操作。

## 功能特性

- **P2P 远程联机**：通过连接其他设备直接进行游戏，数据无需经过服务器中转
- **远程局域网**：RLan 通过 P2P 技术建立远程虚拟局域网，使跨网设备如同身处同一局域网
- **多设备支持**：单个房间最多支持 **8 台设备**同时在线
- **房间机制**：允许用户创建一个房间，或加入一个已有的房间
- **双协议栈**：同时支持 **IPv4 与 IPv6**，适应不同网络环境
- **房间密码**：创建房间时可设置密码，仅持有密码的参与者方可加入，保障房间私密性
- **聊天与文件传输**：支持文字消息和文件传输，自动持久化聊天记录
- **消息管理**：支持文件大小/文字长度/数量限制，自动过期删除
- **断联迁移**：公网地址变化时自动迁移房间并通知成员
- **系统托盘**：支持后台 daemon 模式，最小化到系统托盘

## 使用方式

### 交互式 Shell

```bash
java -jar RLan-0.1.0.jar cli shell
```

进入交互式命令行，支持 create/join/send/file/leave/rooms/members/message 等命令。

### 单次命令

```bash
# 创建房间
java -jar RLan-0.1.0.jar cli create <name> <password>

# 加入房间
java -jar RLan-0.1.0.jar cli join <roomId> <password>

# 查看本机网络信息
java -jar RLan-0.1.0.jar cli info
```

### Daemon 模式

```bash
java -jar RLan-0.1.0.jar cli daemon
```

后台运行，仅系统托盘图标，不读 stdin。托盘右键 "Exit" 退出。

## 技术栈

| 项 | 说明 |
|----|------|
| 编程语言 | Java 21 (Azul Zulu) |
| 构建系统 | Gradle 8.10.2 (Kotlin DSL) |
| 网络层 | Netty 4.1 (UDP) |
| 加密 | AES-256-GCM + SHA-256 |
| 测试 | JUnit 5 + AssertJ |
| 日志 | SLF4j + Logback |

## 构建与运行

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
```

## 数据目录

所有持久化数据存储在 `~/.rlan/` 目录下：

| 路径 | 说明 |
|------|------|
| `~/.rlan/rooms.properties` | 房间信息 |
| `~/.rlan/chats/<roomId>.log` | 聊天记录 |
| `~/.rlan/downloads/` | 发送和接收的文件副本 |
| `~/.rlan/settings.properties` | 用户设置 |

## 平台支持

RLan 目标支持主流桌面操作系统（Windows、Linux、macOS）。由于"远程局域网"需要在系统层面建立虚拟网卡以模拟局域网接口，不同平台对虚拟网卡的支持方式存在差异，实际可用性将以各平台适配进度为准。

## 许可证

待定。
