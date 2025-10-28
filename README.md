# WebSocket Server Framework

[![Java](https://img.shields.io/badge/Java-8+-green.svg)](https://www.oracle.com/java/)
[![Netty](https://img.shields.io/badge/Netty-4.x-blue.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A high-performance WebSocket server framework based on binary protocol, featuring annotation-driven development with enterprise-grade features including multi-endpoint support, room management, and interceptors.

## 📚 Project Overview

This framework is inspired by Socket.IO's design philosophy, but unlike Socket.IO's verbose text protocol, it uses a custom binary protocol for communication, significantly reducing network transmission bytes while dramatically improving communication performance.

### 🚀 Core Features

- **🔥 High-Performance Binary Protocol** - Reduces transmission bytes and improves communication efficiency compared to traditional text protocols
- **📡 Multi-Endpoint Support** - Supports multiple WebSocket endpoints running on the same server
- **🏠 Room Management** - Built-in room mechanism supporting broadcasting and group communication
- **🛡️ Interceptor Mechanism** - Provides interceptor support for authentication, authorization, rate limiting, etc.
- **📝 Annotation-Driven** - Annotation-based development approach that simplifies the programming model
- **⚡ Asynchronous Non-Blocking** - Built on Netty for high-concurrency connection support
- **🔧 Dependency Injection** - Integrated IoC container with auto-wiring support
- **📊 Extensibility** - Modular design for easy extension and customization

## 🏗️ Architecture Design

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

## 📦 Binary Protocol Format

The framework uses a custom binary protocol with the following packet format:

```
+---------+--------------+------------+------------+--------+------+
| VERSION | PKG_LEN_SIZE | EVENT_TYPE | EVENT_NAME | STATUS | DATA |
+---------+--------------+------------+------------+--------+------+
| 1 byte  | 8 byte       | 1 byte     | 2 byte     | 2 byte | bytes|
+---------+--------------+------------+------------+--------+------+
```

**Protocol Field Description:**
- `VERSION`: Protocol version number, current version is 1
- `PKG_LEN_SIZE`: Total packet length, 8 bytes
- `EVENT_TYPE`: Event type, supports connection, disconnection, message, etc.
- `EVENT_NAME`: Event name, supports up to 65535 different events
- `STATUS`: Status code indicating processing result
- `DATA`: Business data, supports any format

## 🚀 Quick Start

### Requirements

- Java 8+
- Maven 3.6+
- [Teambeit Cloud Microservices Framework 1.0+](https://github.com/wayken/cloud)

### 1. Add Dependency

```xml
<dependency>
    <groupId>cloud.apposs</groupId>
    <artifactId>teambeit-websocket</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Create WebSocket Endpoint

```java
@ServerEndpoint("/chat")
public class ChatEndpoint {
    @OnConnect
    public void onConnect(WSSession session) {
        System.out.println("User connected: " + session.getSessionId());
        session.joinRoom("lobby");
    }
    
    @OnEvent(1001) // Chat message event
    public void onChatMessage(WSSession session, String message) {
        // Broadcast message to all users in the room
        session.getNamespace().getRoomOperations("lobby")
                .sendEvent((short) 1002, "User message: " + message);
    }
    
    @OnDisconnect
    public void onDisconnect(WSSession session) {
        System.out.println("User disconnected: " + session.getSessionId());
        session.leaveRoom("lobby");
    }
    
    @OnError
    public void onError(WSSession session, Throwable throwable) {
        System.err.println("Connection error: " + throwable.getMessage());
    }
}
```

### 3. Start Server

```java
@Component
public class WebSocketServer {
    public static void main(String[] args) throws Exception {
        // Start WebSocket service
        WebSocketApplication.run(WebSocketServer.class, args);
    }
}
```

## 📋 Core Annotations

### @ServerEndpoint
Defines WebSocket server endpoint and specifies the URL path for client connections.

```java
@ServerEndpoint(value = {"/chat", "/game"}, host = "localhost")
public class MultiPathEndpoint {
    // Supports multiple paths and host binding
}
```

### @OnConnect
Callback method when WebSocket connection is established.

```java
@OnConnect
public void onConnect(WSSession session) {
    // Connection establishment logic
}
```

### @OnEvent
Handles event messages sent by clients.
```java
@OnEvent(1001)
public void handleUserLogin(WSSession session, LoginRequest request) {
    // Handle user login event
}
```

### @OnDisconnect
Callback method when WebSocket connection is closed.
```java
@OnDisconnect
public void onDisconnect(WSSession session) {
    // Connection cleanup logic
}
```

### @OnError
Error callback during connection or processing.
```java
@OnError
public void onError(WSSession session, Throwable error) {
    // Error handling logic
}
```

### @Order
Controls execution order of multiple handlers of the same type.

```java
@OnConnect
@Order(1)
public void firstConnect(WSSession session) {
    // First connection handler to execute
}

@OnConnect  
@Order(2)
public void secondConnect(WSSession session) {
    // Second connection handler to execute
}
```

## 🏠 Room Management

The framework includes powerful built-in room management functionality, supporting user join/leave operations and room broadcasting.

```java
// Join room
session.joinRoom("game-room-1");

// Leave room
session.leaveRoom("game-room-1");

// Room broadcast
session.getNamespace().getRoomOperations("game-room-1")
       .sendEvent((short) 2001, "Game started");

// Get all users in room
Collection<WSSession> clients = session.getNamespace()
                                      .getRoomOperations("game-room-1")
                                      .getClients();
```

## 🛡️ Interceptor Mechanism

By implementing the `CommandarInterceptor` interface, you can add global interceptors for authentication, authorization, rate limiting, and other features.

```java
@Component
public class AuthInterceptor implements CommandarInterceptor {
    
    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        // Authentication check before connection
        String token = data.getHttpHeaders().get("Authorization");
        return validateToken(token);
    }
    
    @Override
    public boolean onEvent(Commandar commandar, WSSession session, Object argument) {
        // Interceptor check before event processing
        return checkPermission(session, commandar.getEvent());
    }
    
    @Override
    public void afterCompletion(Commandar commandar, WSSession session, Throwable throwable) {
        // Cleanup work after event processing completion
        if (throwable != null) {
            logger.error("Event processing exception", throwable);
        }
    }
}
```

## 📡 Broadcast Operations

The framework provides flexible broadcasting mechanisms, supporting global and room broadcasts.

```java
// Global broadcast - Send to all connected clients
session.getNamespace().getBroadcastOperations()
       .sendEvent((short) 3001, "System announcement", "Server maintenance in 5 minutes");

// Room broadcast - Send to clients in specific room
session.getNamespace().getRoomOperations("vip-room")
       .sendEvent((short) 3002, "VIP exclusive message");

// Broadcast excluding sender
session.getNamespace().getBroadcastOperations()
       .sendEventExclude(session, (short) 3003, "Message to other users");
```

## ⚙️ Configuration Options

```java
WSConfig config = new WSConfig();
config.setHost("0.0.0.0");                    // Bind host
config.setPort(8080);                         // Listen port
config.setNumOfGroup(1);                      // Boss thread count
config.setWorkerCount(0);                     // Worker thread count (0 = CPU cores)
config.setMaxFramePayloadLength(65536);       // Max frame payload length
config.setMaxHttpContentLength(65536);        // Max HTTP content length
config.setHeartbeatInterval(60);              // Heartbeat interval (seconds)
config.setHeartbeatTimeout(180);              // Heartbeat timeout (seconds)
// Start with custom configuration
ApplicationContext context = WebSocketApplication.run(Application.class, config);
```

## 🎯 Use Cases

- **💬 Real-time Chat Systems** - Private chat, group chat, room chat support
- **🎮 Online Games** - Real-time game state synchronization, multiplayer battles
- **📊 Real-time Data Push** - Stock quotes, monitoring data real-time updates
- **🔔 Message Notification Systems** - Real-time message push, system notifications
- **👥 Collaborative Office** - Online document collaboration, real-time editing
- **📹 Live Streaming Interaction** - Bullet screen systems, gift interactions

## 📈 Performance Characteristics

- **High Concurrency Support**: Based on Netty NIO, supports tens of thousands of concurrent connections
- **Low Latency Communication**: Binary protocol reduces serialization overhead
- **Memory Optimization**: Zero-copy technology and object pooling
- **Scalability**: Supports horizontal scaling and load balancing

## 🔧 Extension Development

### Custom Protocol Encoder/Decoder

```java
@Component
public class CustomJsonSupport implements JsonSupport {
    @Override
    public <T> T toObject(String json, Class<T> clazz) {
        // Custom JSON deserialization
        return customParser.parse(json, clazz);
    }
    
    @Override
    public String toJson(Object object) {
        // Custom JSON serialization
        return customParser.toJson(object);
    }
}
```

### Custom Scheduler

```java
@Component
public class CustomScheduler implements CancelableScheduler {
    @Override
    public void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit) {
        // Custom task scheduling logic
    }
}
```

## 📊 Monitoring & Operations

The framework provides rich runtime information access interfaces:

```java
// Get namespace information
Namespace namespace = context.getNamespace("/chat");
int clientCount = namespace.getAllClients().size();

// Get room information
Set<String> rooms = namespace.getRooms();
Collection<WSSession> roomClients = namespace.getRoomClients("game-1");

// Get session information
WSSession session = namespace.getSession(sessionId);
boolean isConnected = session.isConnected();
```

## 🛠️ Development Tools

The project provides client testing tools for convenient development and debugging:

```bash
# Extract client tools
unzip src/main/resources/websocket-client.zip

# Run test client
node client.js ws://localhost:8080/chat
```

## 🤝 Contributing

We welcome Issues and Pull Requests to improve the project:

1. Fork this project
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the [Apache 2.0](LICENSE) License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Netty](https://netty.io/) - High-performance network application framework
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing library
- [Socket.IO](https://socket.io/) - Design inspiration source

## 📞 Contact

For questions or suggestions, please contact us through:

- Submit [Issue](https://github.com/your-org/websocket/issues)
- Send email to: [your-email@example.com]

---

**⭐ If this project helps you, please give it a Star!**
