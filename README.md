# teambeit-websocket

基于 Netty 构建的高性能 WebSocket 二进制通讯框架，提供注解驱动的开发模式、房间广播、RPC 请求响应、附件传输以及分布式集群支持。

## 特性

- 注解驱动，开发简洁（`@ServerEndpoint`、`@OnConnect`、`@OnCommand` 等）
- 自定义二进制协议，高效紧凑
- 支持 WebSocket RPC 请求-响应通讯
- 支持二进制附件传输
- 内置房间（Room）管理与广播
- 拦截器机制，支持鉴权、限流、日志等
- 分布式集群支持（Memory / Redis / Hazelcast）
- 支持 SSL/TLS

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cloud.apposs</groupId>
    <artifactId>teambeit-websocket</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

在 `src/main/resources/application.all.xml` 中添加：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bootor-config>
    <!-- 扫描基础包（必填） -->
    <property name="basePackage">com.example.websocket</property>
    <property name="charset">utf-8</property>
    <property name="host">0.0.0.0</property>
    <property name="port">7010</property>
    <!-- 分布式类型：memory / redission / hazelcast -->
    <property name="distributedType">memory</property>
</bootor-config>
```

### 3. 启动服务

```java
public class Application {
    public static void main(String[] args) throws Exception {
        WebSocketApplication.run(Application.class, args);
    }
}
```

### 4. 定义 Endpoint

```java
@ServerEndpoint("/socket.io")
public class ChatEndpoint {
    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("client connected: " + session.getSessionId());
    }

    @OnCommand("chat")
    public void onChat(WSSession session, ChatObject msg) throws Exception {
        session.sendCommand("chat", msg.getUsername() + ": " + msg.getMessage());
    }

    @OnDisconnect
    public void onDisconnect(WSSession session) {
        System.out.println("client disconnected");
    }

    @OnError
    public void onError(Throwable ex) {
        ex.printStackTrace();
    }
}
```


## 注解说明

| 注解 | 作用 |
|------|------|
| `@ServerEndpoint(path)` | 标记一个类为 WebSocket 服务端，`path` 为客户端连接路径 |
| `@OnConnect` | 客户端建立连接时触发，方法参数为 `WSSession` |
| `@OnDisconnect` | 客户端断开连接时触发，方法参数为 `WSSession` |
| `@OnCommand(cmd)` | 接收指定指令时触发，支持自定义参数类型 |
| `@OnError` | 连接发生异常时触发，方法参数为 `Throwable` |
| `@Order` | 调整同一事件多个处理方法的执行顺序 |

### @OnCommand 参数绑定

方法参数支持任意自定义 POJO（需有默认构造函数），框架会自动从 JSON 数组中按顺序反序列化：

```java
// 客户端发送：socket.emit("chat", {"username":"alice","message":"hi"}, "extra")
@OnCommand("chat")
public void onChat(WSSession session, ChatObject msg, String extra) throws Exception {
    // msg 自动反序列化为 ChatObject，extra 为字符串
}
```

## 协议格式

框架使用自定义二进制协议，包头固定 4 字节：

```
+---------+--------------+--------+----------+---+-----------+
| VERSION | COMMAND_TYPE | STATUS | METADATA | # | PARAMETER |
+---------+--------------+--------+----------+---+-----------+
| 1 byte  | 1 byte       | 2 byte | JSON     |   | JSON Array|
+---------+--------------+--------+----------+---+-----------+
```

- `VERSION`：协议版本，当前为 `0x01`
- `COMMAND_TYPE`：`0=HANDSHAKE` `1=COMMAND` `2=DISCONNECT` `3=ERROR`
- `STATUS`：状态码，范围 `[0, 65535]`
- `METADATA`：JSON 对象，包含 `_cmd`（指令名）、`_id`（RPC请求ID）、`_num`（附件数量）
- `PARAMETER`：JSON 数组，业务参数列表

完整数据包示例：

```
1\x01\x00\x00{"_cmd":"chat","_id":"uuid","_num":0}#[{"username":"alice","message":"hi"}]
```

## 核心功能

### 广播与房间

```java
@OnCommand("join")
public void onJoin(WSSession session, String room) throws Exception {
    session.joinRoom(room);
}

@OnCommand("broadcast")
public void onBroadcast(WSSession session, String room, ChatObject msg) throws Exception {
    // 广播给房间内所有客户端
    session.getDistributedRoomOperations(room).sendCommand("chat", msg);
}

@OnCommand("broadcast_all")
public void onBroadcastAll(WSSession session, ChatObject msg) throws Exception {
    // 广播给命名空间下所有客户端
    session.getDistributedRoomOperations().sendCommand("chat", msg);
}
```

### RPC 请求-响应

服务端接收带 `_id` 的请求，通过 `sendResponse` 回复：

```java
@OnCommand("query")
public void onQuery(WSSession session, Metadata metadata, QueryRequest req) throws Exception {
    QueryResult result = doQuery(req);
    // 将结果回复给发起 RPC 请求的客户端
    session.sendResponse(metadata.getCommandId(), result);
}
```

### 附件传输

发送和接收二进制附件（如文件）：

```java
// 接收附件
@OnCommand("upload")
public void onUpload(WSSession session, AttachmentObject file) throws Exception {
    System.out.println("received: " + file.getName() + ", size: " + file.getContent().length);
}

// 发送附件
@OnCommand("download")
public void onDownload(WSSession session, Metadata metadata) throws Exception {
    AttachmentObject file = new AttachmentObject();
    file.setName("report.pdf");
    file.setContent(loadFileBytes());
    session.sendResponse(metadata.getCommandId(), file);
}
```

附件对象中使用占位符标记二进制数据位置，框架会自动在主包后追加附件数据包。

### 拦截器

实现 `CommandarInterceptor` 或继承 `CommandarInterceptorAdapter`，并加上 `@Component` 注解：

```java
@Component
public class AuthInterceptor extends CommandarInterceptorAdapter {

    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        // 握手阶段鉴权，返回 false 则拒绝连接
        String token = data.getParameters().get("token");
        return validateToken(token);
    }

    @Override
    public boolean onCommand(Commandar commandar, WSSession session, List<Object> args) {
        // 每次指令处理前调用，返回 false 则中断处理
        return true;
    }

    @Override
    public void afterCompletion(Commandar commandar, WSSession session, Throwable ex) {
        // 指令处理完成后调用，可用于性能监控、资源清理
    }
}
```

## 参数校验

框架内置 OOP 风格的参数校验器，通过在 POJO 字段上添加注解自动完成校验与类型转换，校验失败时抛出 `IllegalArgumentException`。

### 使用方式

```java
public class RegisterRequest {
    @NotBlank
    private String username;

    @Email
    private String email;

    @Mobile(require = false)
    private String phone;

    @Number(min = 18, max = 120)
    private int age;
}
```

### 校验注解说明

| 注解 | 适用类型 | 说明 | 主要属性 |
|------|----------|------|----------|
| `@NotNull` | 任意对象 | 值不能为 null | `message` |
| `@NotEmpty` | String / List / Map | 不能为空（长度/元素数 > 0） | `require`, `message` |
| `@NotBlank` | String | trim 后长度必须 > 0，回写 trim 后的值 | `require`, `message` |
| `@Bool` | Boolean | 必须为布尔类型，支持默认值 | `require`, `value`, `message` |
| `@Digits` | int | 非负整数（>= 0），可设最大值 | `require`, `max`, `message` |
| `@Digits64` | long | 非负长整数（>= 0L），可设最大值 | `require`, `max`, `message` |
| `@Number` | int | 整数，可设范围 `[min, max]` | `require`, `min`, `max`, `message` |
| `@Number64` | long | 长整数，可设范围 `[min, max]` | `require`, `min`, `max`, `message` |
| `@Id` | long | 正整数（> 0L），通常为 IdWorker 生成的 ID | `require`, `max`, `message` |
| `@Length` | String | 字符串长度范围，默认自动 trim | `require`, `trim`, `min`, `max`, `message` |
| `@Email` | String | 合法的电子邮箱格式 | `require`, `message` |
| `@Mobile` | String | 合法的手机号码格式 | `require`, `message` |
| `@Pattern` | String | 匹配一个或多个正则表达式 | `require`, `regex`, `xor`, `message` |

### 属性说明

- `require`：默认 `true`，为 `false` 时若值为 null 则跳过校验直接返回 null
- `message`：自定义错误信息，为空时框架自动生成包含字段名和指令名的提示


## 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `basePackage` | — | 扫描包路径（必填） |
| `host` | `0.0.0.0` | 绑定地址 |
| `port` | `7010` | 绑定端口 |
| `numOfGroup` | CPU核数+1 | EventLoop 线程数 |
| `executorOn` | `false` | 是否开启业务线程池 |
| `workerCount` | CPU核数×2 | 业务线程池大小 |
| `keepAlive` | `false` | 是否保持长连接不超时 |
| `maxHttpContentLength` | `2MB` | HTTP 内容最大长度 |
| `maxFramePayloadLength` | `2MB` | WebSocket 帧最大长度 |
| `firstDataTimeout` | `5000ms` | 首次数据传输超时 |
| `distributedType` | `memory` | 分布式类型 |
| `distributedServiceName` | `websocket` | 分布式服务名（集群必填） |
| `distributedServerAddress` | `127.0.0.1:3679` | 分布式服务地址 |
| `socketReconnectOn` | `true` | 客户端自动重连 |
| `socketMaxReconnectAttempts` | `5` | 最大重连次数（-1不限制） |
| `socketReconnectInterval` | `3000ms` | 重连间隔 |
| `logLevel` | `INFO` | 日志级别 |
| `logAppender` | `CONSOLE` | 日志输出终端 |

## 分布式部署

框架支持三种分布式模式，通过 `distributedType` 配置切换：

**内存模式（单机/开发）**
```xml
<property name="distributedType">memory</property>
```

**Redis 模式（需引入 redisson 依赖）**
```xml
<property name="distributedType">redission</property>
<property name="distributedServiceName">my-app-ws</property>
<property name="distributedServerAddress">127.0.0.1:6379</property>
```

**Hazelcast 模式（需引入 hazelcast 依赖）**
```xml
<property name="distributedType">hazelcast</property>
<property name="distributedServiceName">my-app-ws</property>
```

> 同一业务的多个实例必须使用相同的 `distributedServiceName` 才能组成集群，不同业务使用不同名称互不干扰。

## SSL/TLS

```java
WSConfig config = WebSocketApplication.generateConfiguration(Application.class, args);
config.setSslProtocol("TLSv1.2");
config.setKeyStore(Application.class.getResourceAsStream("/keystore.jks"));
config.setKeyStorePassword("your-password");
WebSocketApplication.run(Application.class, config, args);
```

## 项目结构

```
src/main/java/cloud/apposs/websocket/
├── annotation/          # 注解定义（@ServerEndpoint、@OnCommand 等）
├── broadcast/           # 广播操作接口与实现
├── commandar/           # 指令路由与调用
├── distributed/         # 分布式服务（Memory/Redis/Hazelcast）
├── interceptor/         # 拦截器接口与适配器
├── namespace/           # 命名空间与房间管理
├── netty/               # Netty 底层实现
├── protocol/            # 二进制协议编解码
├── scheduler/           # 定时任务调度
├── timer/               # 时间轮实现
├── WebSocketApplication.java   # 启动入口
├── WSConfig.java               # 配置类
└── WSSession.java              # 会话抽象
```

## 参考

- [Netty-SocketIO](https://github.com/mrniko/netty-socketio)

## License

[LICENSE](LICENSE)
