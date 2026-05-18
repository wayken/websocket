package cloud.apposs.websocket.listener;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link CommandarListener}管理器
 */
public final class CommandarListenerSupport {
    private final List<CommandarListener> listenerList = new CopyOnWriteArrayList<CommandarListener>();

    public void addListener(CommandarListener listener) {
        listenerList.add(listener);
    }

    public void removeListener(CommandarListener listener) {
        listenerList.remove(listener);
    }

    public void commandarStart(Commandar commandar, WSSession session, List<Object> argument) {
        for (int i = 0; i < listenerList.size(); i++) {
            CommandarListener listener = listenerList.get(i);
            listener.commandarStart(commandar, session, argument);
        }
    }

    public void commandarCompletion(Commandar commandar, WSSession session, Throwable cause) {
        for (int i = 0; i < listenerList.size(); i++) {
            CommandarListener listener = listenerList.get(i);
            listener.commandarCompletion(commandar, session, cause);
        }
    }
}
