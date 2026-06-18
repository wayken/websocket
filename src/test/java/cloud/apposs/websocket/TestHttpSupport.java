package cloud.apposs.websocket;

import cloud.apposs.websocket.client.WSClient;
import cloud.apposs.websocket.client.WSClientConfig;
import cloud.apposs.websocket.client.WSClientListenerAdapter;
import cloud.apposs.websocket.client.WebSocketClient;
import cloud.apposs.websocket.protocol.Packet;
import cloud.apposs.websocket.protocol.PacketType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * HTTP协议支持测试用例，验证HTTP和WebSocket在同一端口上共存
 */
public class TestHttpSupport {
    private static ApplicationContext server;
    private static final int PORT = 7012;
    private static final String BASE_URL = "http://127.0.0.1:" + PORT;

    @BeforeClass
    public static void startServer() throws Exception {
        server = WebSocketApplication.run(TestHttpSupport.class, new String[]{});
        Thread.sleep(500);
    }

    @AfterClass
    public static void stopServer() {
        WebSocketApplication.shutdown(server);
    }

    // ===== HTTP GET 请求测试 =====

    @Test
    public void testHttpGetSimple() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/hello").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        String body = readResponse(conn);
        assertTrue(body.contains("\"success\":true"));
        assertTrue(body.contains("hello"));
        conn.disconnect();
    }

    @Test
    public void testHttpGetWithPathVariable() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/user/123").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        String body = readResponse(conn);
        assertTrue(body.contains("\"success\":true"));
        assertTrue(body.contains("user-123"));
        conn.disconnect();
    }

    // ===== HTTP POST 请求测试 =====

    @Test
    public void testHttpPost() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/echo").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write("test".getBytes());
        assertEquals(200, conn.getResponseCode());
        String body = readResponse(conn);
        assertTrue(body.contains("\"success\":true"));
        assertTrue(body.contains("echo"));
        conn.disconnect();
    }

    // ===== 404 测试 =====

    @Test
    public void testHttpNotFound() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/not_exist").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(404, conn.getResponseCode());
        conn.disconnect();
    }

    // ===== WebSocket 在同一端口仍然可用 =====

    @Test
    public void testWebSocketStillWorks() throws Exception {
        WSClientConfig config = new WSClientConfig();
        config.setPort(PORT);
        config.setReconnectOn(false);

        CountDownLatch connLatch = new CountDownLatch(1);
        CountDownLatch msgLatch = new CountDownLatch(1);
        AtomicReference<Packet> received = new AtomicReference<>();

        WSClient client = WebSocketClient.connect(config, new WSClientListenerAdapter() {
            @Override
            public void onConnect(WSClient c) {
                connLatch.countDown();
            }

            @Override
            public void onCommand(WSClient c, Packet packet) {
                if (packet.getType() == PacketType.COMMAND) {
                    received.set(packet);
                    msgLatch.countDown();
                }
            }
        });

        assertTrue("WS should connect", connLatch.await(5, TimeUnit.SECONDS));
        client.sendCommand("echo", "ping");
        assertTrue("WS should receive reply", msgLatch.await(5, TimeUnit.SECONDS));

        Packet reply = received.get();
        assertNotNull(reply);
        assertEquals("echo", reply.getCommand());

        WebSocketClient.shutdown(client);
    }

    // ===== HTTP Content-Type 测试 =====

    @Test
    public void testHttpResponseContentType() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/hello").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        String contentType = conn.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        conn.disconnect();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
