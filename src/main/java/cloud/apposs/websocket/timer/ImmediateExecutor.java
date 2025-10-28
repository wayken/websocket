package cloud.apposs.websocket.timer;

import java.util.concurrent.Executor;

/**
 * {@link Executor} which execute tasks in the callers thread.
 */
public final class ImmediateExecutor implements Executor {
    public static final ImmediateExecutor INSTANCE = new ImmediateExecutor();

    private ImmediateExecutor() {
        // use static instance
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException("command");
        }
        command.run();
    }
}
