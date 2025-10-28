# WebSocket服务端框架 

[![Java](https://img.shields.io/badge/Java-8+-green.svg)](https://www.oracle.com/java/)
[![Netty](https://img.shields.io/badge/Netty-4.x-blue.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://www.apache.org/licenses/LICENSE-2.0)

一个基于二进制协议的高性能WebSocket服务端框架，采用注解驱动开发，支持多Endpoint、房间管理、拦截器等企业级特性。

## 📚 项目介绍

该框架参考了Socket.IO的设计理念，但相比于Socket.IO冗长的文本协议，本框架采用了自定义的二进制协议进行通信，大幅减少了网络传输的字节数量，同时显著提升了通信性能。

### 🚀 核心特性

- **🔥 高性能二进制协议** - 相比传统文本协议，减少传输字节数，提升通信效率
- **📡 多Endpoint支持** - 支持同一服务器运行多个WebSocket端点
- **🏠 房间管理** - 内置房间机制，支持广播和分组通信
- **🛡️ 拦截器机制** - 提供认证、鉴权、限流等拦截器支持
- **📝 注解驱动** - 基于注解的开发方式，简化编程模型
- **⚡ 异步非阻塞** - 基于Netty构建，支持高并发连接
- **🔧 依赖注入** - 集成IoC容器，支持自动装配
- **📊 可扩展性** - 模块化设计，易于扩展和定制

## 🏗️ 架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                    WebSocket Application                     │
├──────────────────────────────────────────────────────────────┤
│  @ServerEndpoint    │   Interceptors   │   Broadcast Ops     │
│  ┌─────────────────┬┴──────────────────┴───────────────────┐ │
│  │  Annotations    │            Namespace Manager          │ │
│  │  @OnConnect     ├───────────────────────────────────────┤ │
│  │  @OnEvent       │            Session Manager            │ │
│  │  @OnDisconnect  ├───────────────────────────────────────┤ │
│  │  @OnError       │         Command Router                │ │
│  └─────────────────┴───────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│                    Protocol Layer                            │
│  ┌─────────────────┬──────────────────┬─────────────────┐    │
│  │  Packet Encoder │  Binary Protocol │ Packet Decoder  │    │
│  └─────────────────┴──────────────────┴─────────────────┘    │
├──────────────────────────────────────────────────────────────┤
│                     Netty Layer                              │
│  ┌─────────────────┬──────────────────┬─────────────────┐    │
│  │ Channel Handler │   Event Loop     │  Channel Group  │    │
│  └─────────────────┴──────────────────┴─────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

## 📦 二进制协议格式

框架采用自定义的二进制协议，数据包格式如下：

```
+---------+--------------+------------+------------+--------+------+
| VERSION | PKG_LEN_SIZE | EVENT_TYPE | EVENT_NAME | STATUS | DATA |
+---------+--------------+------------+------------+--------+------+
| 1 byte  | 8 byte       | 1 byte     | 2 byte     | 2 byte | bytes|
+---------+--------------+------------+------------+--------+------+
```

**协议字段说明：**
- `VERSION`: 协议版本号，当前版本为1
- `PKG_LEN_SIZE`: 数据包总长度，8字节
- `EVENT_TYPE`: 事件类型，支持连接、断开、消息等类型
- `EVENT_NAME`: 事件名称，支持65535个不同事件
- `STATUS`: 状态码，表示处理结果
- `DATA`: 业务数据，支持任意格式

## 🚀 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- [Teambeit Cloud Microservices Framework 1.0+](https://github.com/wayken/cloud)

### 1. 添加依赖

```xml
<dependency>
    <groupId>cloud.apposs</groupId>
    <artifactId>teambeit-websocket</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 创建WebSocket端点

```java
@ServerEndpoint("/chat")
public class ChatEndpoint {
    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("用户连接: " + session.getSessionId());
        session.joinRoom("lobby");
    }
    
    @OnEvent(1001) // 聊天消息事件
    public void onChatMessage(WSSession session, String message) {
        // 广播消息到房间内所有用户
        session.getNamespace().getRoomOperations("lobby")
                .sendEvent((short) 1002, "用户消息: " + message);
    }
    
    @OnDisconnect
    public void onDisconnect(WSSession session) {
        System.out.println("用户断开: " + session.getSessionId());
        session.leaveRoom("lobby");
    }
    
    @OnError
    public void onError(WSSession session, Throwable throwable) {
        System.err.println("连接错误: " + throwable.getMessage());
    }
}
```

### 3. 启动服务器

```java
@Component
public class WebSocketServer {
    public static void main(String[] args) throws Exception {
        // 启动WebSocket服务
        WebSocketApplication.run(WebSocketServer.class, args);
    }
}
```

## 📋 核心注解说明

### @ServerEndpoint
定义WebSocket服务端点，指定客户端连接的URL路径。

```java
@ServerEndpoint(value = {"/chat", "/game"}, host = "localhost")
public class MultiPathEndpoint {
    // 支持多路径和主机绑定
}
```

### @OnConnect
WebSocket连接建立时的回调方法。

```java
@OnConnect
public void onConnect(WSSession session) {
    // 连接建立逻辑
}
```

### @OnEvent
处理客户端发送的事件消息。
```java
@OnEvent(1001)
public void handleUserLogin(WSSession session, LoginRequest request) {
    // 处理用户登录事件
}
```

### @OnDisconnect
WebSocket连接断开时的回调方法。
```java
@OnDisconnect
public void onDisconnect(WSSession session) {
    // 连接断开清理逻辑
}
```

### @OnError
连接或处理过程中的错误回调。
```java
@OnError
public void onError(WSSession session, Throwable error) {
    // 错误处理逻辑
}
```

### @Order
控制多个同类型事件处理器的执行顺序。

```java
@OnConnect
@Order(1)
public void firstConnect(WSSession session) {
    // 第一个执行的连接处理器
}

@OnConnect  
@Order(2)
public void secondConnect(WSSession session) {
    // 第二个执行的连接处理器
}
```

## 🏠 房间管理

框架内置了强大的房间管理功能，支持用户加入/离开房间，以及房间内广播。

```java
// 加入房间
session.joinRoom("game-room-1");

// 离开房间
session.leaveRoom("game-room-1");

// 房间内广播
session.getNamespace().getRoomOperations("game-room-1")
       .sendEvent((short) 2001, "游戏开始");

// 获取房间内所有用户
Collection<WSSession> clients = session.getNamespace()
                                      .getRoomOperations("game-room-1")
                                      .getClients();
```

## 🛡️ 拦截器机制

通过实现`CommandarInterceptor`接口可以添加全局拦截器，用于认证、鉴权、限流等功能。

```java
@Component
public class AuthInterceptor implements CommandarInterceptor {
    
    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        // 连接前的认证检查
        String token = data.getHttpHeaders().get("Authorization");
        return validateToken(token);
    }
    
    @Override
    public boolean onEvent(Commandar commandar, WSSession session, Object argument) {
        // 事件处理前的拦截检查
        return checkPermission(session, commandar.getEvent());
    }
    
    @Override
    public void afterCompletion(Commandar commandar, WSSession session, Throwable throwable) {
        // 事件处理完成后的清理工作
        if (throwable != null) {
            logger.error("事件处理异常", throwable);
        }
    }
}
```

## 📡 广播操作

框架提供了灵活的广播机制，支持全局广播和房间广播。

```java
// 全局广播 - 发送给所有连接的客户端
session.getNamespace().getBroadcastOperations()
       .sendEvent((short) 3001, "系统公告", "服务器将在5分钟后维护");

// 房间广播 - 发送给特定房间的客户端
session.getNamespace().getRoomOperations("vip-room")
       .sendEvent((short) 3002, "VIP专属消息");

// 排除发送者的广播
session.getNamespace().getBroadcastOperations()
       .sendEventExclude(session, (short) 3003, "其他用户消息");
```

## ⚙️ 配置选项

```java
WSConfig config = new WSConfig();
config.setHost("0.0.0.0");                    // 绑定主机
config.setPort(8080);                         // 监听端口
config.setNumOfGroup(1);                      // Boss线程数
config.setWorkerCount(0);                     // Worker线程数(0为CPU核数)
config.setMaxFramePayloadLength(65536);       // 最大帧负载长度
config.setMaxHttpContentLength(65536);        // 最大HTTP内容长度
config.setHeartbeatInterval(60);              // 心跳间隔(秒)
config.setHeartbeatTimeout(180);              // 心跳超时(秒)
// 使用自定义配置启动
ApplicationContext context = WebSocketApplication.run(Application.class, config);
```

## 🎯 使用场景

- **💬 实时聊天系统** - 支持私聊、群聊、房间聊天
- **🎮 在线游戏** - 实时游戏状态同步、多人对战
- **📊 实时数据推送** - 股票行情、监控数据实时更新
- **🔔 消息通知系统** - 实时消息推送、系统通知
- **👥 协同办公** - 在线文档协作、实时编辑
- **📹 直播互动** - 弹幕系统、礼物互动

## 📈 性能特点

- **高并发支持**: 基于Netty NIO，支持数万并发连接
- **低延迟通信**: 二进制协议减少序列化开销
- **内存优化**: 零拷贝技术和对象池化
- **可伸缩性**: 支持水平扩展和负载均衡

## 🔧 扩展开发

### 自定义协议编解码器

```java
@Component
public class CustomJsonSupport implements JsonSupport {
    @Override
    public <T> T toObject(String json, Class<T> clazz) {
        // 自定义JSON反序列化
        return customParser.parse(json, clazz);
    }
    
    @Override
    public String toJson(Object object) {
        // 自定义JSON序列化
        return customParser.toJson(object);
    }
}
```

### 自定义调度器

```java
@Component
public class CustomScheduler implements CancelableScheduler {
    @Override
    public void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit) {
        // 自定义任务调度逻辑
    }
}
```

## 📊 监控与运维

框架提供了丰富的运行时信息获取接口：

```java
// 获取命名空间信息
Namespace namespace = context.getNamespace("/chat");
int clientCount = namespace.getAllClients().size();

// 获取房间信息
Set<String> rooms = namespace.getRooms();
Collection<WSSession> roomClients = namespace.getRoomClients("game-1");

// 获取会话信息
WSSession session = namespace.getSession(sessionId);
boolean isConnected = session.isConnected();
```

## 🛠️ 开发工具

项目提供了客户端测试工具，方便开发调试：

```bash
# 解压客户端工具
unzip src/main/resources/websocket-client.zip

# 运行测试客户端
node client.js ws://localhost:8080/chat
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目：

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 📄 许可证

本项目采用 [Apache 2.0](LICENSE) 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Netty](https://netty.io/) - 高性能网络应用框架
- [Jackson](https://github.com/FasterXML/jackson) - JSON处理库
- [Socket.IO](https://socket.io/) - 设计灵感来源

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [Issue](https://github.com/your-org/websocket/issues)
- 发送邮件至: [your-email@example.com]

---

**⭐ 如果这个项目对你有帮助，请给它一个Star！**
