package cloud.apposs.websocket.listener;

import cloud.apposs.websocket.WSConfig;

import java.util.LinkedList;
import java.util.List;

public final class ApplicationListenerSupport {
    private static final List<ApplicationListener> listenerList = new LinkedList<ApplicationListener>();

    public void addListener(ApplicationListener listener) {
        if (listener != null) {
            listenerList.add(listener);
        }
    }

    public void removeListener(ApplicationListener listener) {
        if (listener != null) {
            listenerList.remove(listener);
        }
    }

    public void onStartup(WSConfig config) {
        for (int i = 0; i < listenerList.size(); i++) {
            ApplicationListener listener = listenerList.get(i);
            listener.onStartup(config);
        }
    }

    /**
     * Web容器关闭时的插件销毁
     */
    public void onShutdown() {
        for (int i = 0; i < listenerList.size(); i++) {
            ApplicationListener listener = listenerList.get(i);
            listener.onShutdown();
        }
    }
}
