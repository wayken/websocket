package cloud.apposs.websocket.client;

import cloud.apposs.websocket.WebSocketApplication;
import cloud.apposs.websocket.ApplicationContext;
import cloud.apposs.websocket.protocol.*;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class TestWebSocketClient {

    // ===== WSClientConfig 测试 =====

    @Test
    public void testConfigDefaults() {
        WSClientConfig config = new WSClientConfig();
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(7010, config.getPort());
        assertEquals("/socket.io", config.getPath());
        assertTrue(config.isReconnectOn());
        assertEquals(5, config.getMaxReconnectAttempts());
        assertEquals(3000, config.getReconnectInterval());
        assertEquals("ws://127.0.0.1:7010/socket.io", config.getUri());
    }

    @Test
    public void testConfigSslUri() {
        WSClientConfig config = new WSClientConfig();
        config.setSslProtocol("TLSv1.2");
        assertEquals("wss://127.0.0.1:7010/socket.io", config.getUri());
    }

    // ===== ClientPacketDecoder 测试 =====

    @Test
    public void testDecodeCommandPacket() throws Exception {
        JsonSupport jsonSupport = new JacksonJsonSupport();
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(jsonSupport, "utf-8");

        // 构造一个COMMAND包并编码
        Packet sent = new Packet(PacketType.COMMAND);
        sent.setCommand("chat");
        sent.getParameter().setArguments(Arrays.asList("hello", "world"));
        byte[] encoded = PacketEncoder.encode(sent, jsonSupport);

        // 解码
        Packet decoded = decoder.decode(ByteBuffer.wrap(encoded));
        assertNotNull(decoded);
        assertEquals(PacketType.COMMAND, decoded.getType());
        assertEquals("chat", decoded.getCommand());
        List<Object> args = decoded.getParameter().getArguments();
        assertEquals(2, args.size());
    }

    @Test
    public void testDecodeDisconnectPacket() throws Exception {
        JsonSupport jsonSupport = new JacksonJsonSupport();
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(jsonSupport, "utf-8");

        Packet sent = new Packet(PacketType.DISCONNECT);
        byte[] encoded = PacketEncoder.encode(sent, jsonSupport);

        Packet decoded = decoder.decode(ByteBuffer.wrap(encoded));
        assertNotNull(decoded);
        assertEquals(PacketType.DISCONNECT, decoded.getType());
    }

    @Test
    public void testDecodeNullReturnsNull() throws Exception {
        JsonSupport jsonSupport = new JacksonJsonSupport();
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(jsonSupport, "utf-8");
        assertNull(decoder.decode(null));
        assertNull(decoder.decode(ByteBuffer.allocate(0)));
    }

    // ===== WSClient 生命周期测试（使用Mock子类） =====

    @Test
    public void testClientLifecycle() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setReconnectOn(false);
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        MockWSClient client = new MockWSClient(config, new WSClientListenerAdapter() {
            @Override
            public void onConnect(WSClient c) {
                connectLatch.countDown();
            }

            @Override
            public void onDisconnect(WSClient c) {
                disconnectLatch.countDown();
            }
        });

        assertFalse(client.isConnected());
        client.connect();
        // MockWSClient在doConnect中直接调用onConnected
        assertTrue(connectLatch.await(1, TimeUnit.SECONDS));
        assertTrue(client.isConnected());

        client.disconnect();
        assertTrue(disconnectLatch.await(1, TimeUnit.SECONDS));
        assertFalse(client.isConnected());
    }

    @Test
    public void testSendCommand() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setReconnectOn(false);
        MockWSClient client = new MockWSClient(config, new WSClientListenerAdapter());
        client.connect();

        client.sendCommand("chat", "hello");
        assertNotNull(client.lastSentData);
        // 验证发送的数据能被解码
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(config.getJsonSupport(), config.getCharset());
        Packet decoded = decoder.decode(ByteBuffer.wrap(client.lastSentData));
        assertEquals("chat", decoded.getCommand());
    }

    @Test
    public void testReconnectAttempts() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setReconnectOn(true);
        config.setMaxReconnectAttempts(2);
        config.setReconnectInterval(100);

        AtomicReference<Integer> reconnectScheduled = new AtomicReference<>(0);
        MockWSClient client = new MockWSClient(config, new WSClientListenerAdapter()) {
            @Override
            protected void handleScheduleReconnect(int delayMs) {
                reconnectScheduled.set(reconnectScheduled.get() + 1);
            }
        };
        client.connect();
        assertTrue(client.isConnected());

        // 模拟连接断开，不重新连接成功，只是连续断开
        client.simulateDisconnect();
        assertFalse(client.isConnected());
        assertEquals(Integer.valueOf(1), reconnectScheduled.get());

        // 假设重连失败又断开（不经过onConnected）
        client.simulateDisconnect();
        assertEquals(Integer.valueOf(2), reconnectScheduled.get());

        // 第三次不应再调度重连（已达上限）
        client.simulateDisconnect();
        assertEquals(Integer.valueOf(2), reconnectScheduled.get());
    }

    // ===== 端到端集成测试（需要服务端启动） =====

    @Test
    public void testEndToEndEchoCommand() throws Exception {
        // 启动服务端（配置文件中端口为7012）
        ApplicationContext server = WebSocketApplication.run(TestWebSocketClient.class, new String[]{});
        Thread.sleep(500);

        try {
            WSClientConfig config = new WSClientConfig();
            config.setPort(7012);
            config.setReconnectOn(false);

            CountDownLatch connectedLatch = new CountDownLatch(1);
            CountDownLatch messageLatch = new CountDownLatch(1);
            AtomicReference<Packet> receivedPacket = new AtomicReference<>();

            WSClient client = WebSocketClient.connect(config, new WSClientListenerAdapter() {
                @Override
                public void onConnect(WSClient c) {
                    connectedLatch.countDown();
                }

                @Override
                public void onCommand(WSClient c, Packet packet) {
                    // 忽略握手包，只关注COMMAND包
                    if (packet.getType() == PacketType.COMMAND) {
                        receivedPacket.set(packet);
                        messageLatch.countDown();
                    }
                }
            });

            assertTrue("Should connect within 5s", connectedLatch.await(5, TimeUnit.SECONDS));
            assertTrue(client.isConnected());

            // 发送echo指令，服务端会回复相同内容
            client.sendCommand("echo", "ping");
            assertTrue("Should receive reply within 5s", messageLatch.await(5, TimeUnit.SECONDS));

            Packet reply = receivedPacket.get();
            assertNotNull(reply);
            assertEquals("echo", reply.getCommand());

            WebSocketClient.shutdown(client);
        } finally {
            WebSocketApplication.shutdown(server);
        }
    }

    // ===== 附件接收测试 =====

    @Test
    public void testDecodeCommandWithAttachment() throws Exception {
        JsonSupport jsonSupport = new JacksonJsonSupport();
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(jsonSupport, "utf-8");

        // 构造带附件的COMMAND包
        Packet sent = new Packet(PacketType.COMMAND);
        sent.setCommand("upload");
        byte[] fileContent = "hello binary".getBytes("utf-8");
        sent.getParameter().setArguments(Arrays.asList(fileContent, "extra"));
        byte[] encoded = PacketEncoder.encode(sent, jsonSupport);

        // 第一帧：主包（有附件占位符），应返回null等待附件帧
        Packet decoded = decoder.decode(ByteBuffer.wrap(encoded));
        assertNull("Should wait for attachment frame", decoded);

        // 第二帧：附件数据
        List<ByteBuffer> attachments = sent.getAttachments();
        assertNotNull(attachments);
        assertEquals(1, attachments.size());
        decoded = decoder.decode(attachments.get(0).duplicate());
        assertNotNull("Should return complete packet after attachment received", decoded);
        assertEquals(PacketType.COMMAND, decoded.getType());
        assertEquals("upload", decoded.getCommand());
        assertEquals(2, ((List<?>) decoded.getParameter().getArguments()).size());
    }

    @Test
    public void testDecodeCommandWithMultipleAttachments() throws Exception {
        JsonSupport jsonSupport = new JacksonJsonSupport();
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(jsonSupport, "utf-8");

        Packet sent = new Packet(PacketType.COMMAND);
        sent.setCommand("multi");
        byte[] file1 = "file1data".getBytes("utf-8");
        byte[] file2 = "file2data".getBytes("utf-8");
        sent.getParameter().setArguments(Arrays.asList(file1, file2));
        byte[] encoded = PacketEncoder.encode(sent, jsonSupport);

        // 主包
        Packet decoded = decoder.decode(ByteBuffer.wrap(encoded));
        assertNull(decoded);

        // 第1个附件
        decoded = decoder.decode(sent.getAttachments().get(0).duplicate());
        assertNull("Should still wait for second attachment", decoded);

        // 第2个附件
        decoded = decoder.decode(sent.getAttachments().get(1).duplicate());
        assertNotNull(decoded);
        assertEquals("multi", decoded.getCommand());
        assertEquals(2, ((List<?>) decoded.getParameter().getArguments()).size());
    }

    // ===== 附件发送测试 =====

    @Test
    public void testSendCommandWithAttachment() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setReconnectOn(false);
        MockWSClient client = new MockWSClient(config, new WSClientListenerAdapter());
        client.connect();

        Packet packet = new Packet(PacketType.COMMAND);
        packet.setCommand("upload");
        byte[] fileContent = "binary data".getBytes("utf-8");
        packet.getParameter().setArguments(Arrays.asList(fileContent, "info"));
        client.send(packet);

        // 应发送主包 + 1个附件帧 = 2次
        assertEquals(2, client.allSentData.size());

        // 验证主包可解码
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(config.getJsonSupport(), config.getCharset());
        Packet decoded = decoder.decode(ByteBuffer.wrap(client.allSentData.get(0)));
        assertNull(decoded); // 等待附件

        // 发送附件帧后完成解码
        decoded = decoder.decode(ByteBuffer.wrap(client.allSentData.get(1)));
        assertNotNull(decoded);
        assertEquals("upload", decoded.getCommand());
    }

    // ===== sendResponse 测试 =====

    @Test
    public void testSendResponse() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setReconnectOn(false);
        MockWSClient client = new MockWSClient(config, new WSClientListenerAdapter());
        client.connect();

        client.sendResponse("req-123", "result-data");
        assertFalse(client.allSentData.isEmpty());

        // 验证响应包含commandId
        WSClientPacketDecoder decoder = new WSClientPacketDecoder(config.getJsonSupport(), config.getCharset());
        Packet decoded = decoder.decode(ByteBuffer.wrap(client.allSentData.get(0)));
        assertNotNull(decoded);
        assertEquals(PacketType.COMMAND, decoded.getType());
        assertEquals("req-123", decoded.getMetadata().getCommandId());
        assertEquals(1, ((List<?>) decoded.getParameter().getArguments()).size());
    }

    // ===== Mock WSClient =====

    private static class MockWSClient extends WSClient {
        byte[] lastSentData;
        List<byte[]> allSentData = new java.util.ArrayList<>();

        MockWSClient(WSClientConfig config, WSClientListener listener) {
            super(config, listener);
        }

        @Override
        protected void handleConnect() {
            onConnected();
        }

        @Override
        protected void handleDisconnect() {
            onDisconnected();
        }

        @Override
        protected void handleShutdown() {
        }

        @Override
        protected void handleSend(byte[] data) {
            lastSentData = data;
            allSentData.add(data);
        }

        @Override
        protected void handleScheduleReconnect(int delayMs) {
        }

        void simulateDisconnect() {
            onDisconnected();
        }
    }
}
