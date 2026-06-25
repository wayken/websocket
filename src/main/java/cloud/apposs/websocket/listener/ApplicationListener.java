package cloud.apposs.websocket.listener;

import cloud.apposs.websocket.WSConfig;

import java.util.EventListener;

public interface ApplicationListener extends EventListener {
    /**
     * 在服务初始化时监听
     *
     * @param config WebSocket配置
     */
    void onInitialize(WSConfig config);

    /**
     * 在服务启动后监听
     *
     * @param config WebSocket配置
     */
    void onStartup(WSConfig config);

    /**
     * 在服务关闭时监听
     */
    void onShutdown();
}
