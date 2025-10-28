package cloud.apposs.websocket.scheduler;

import java.util.concurrent.TimeUnit;

public interface CancelableScheduler {
    void cancel(SchedulerKey key);

    void schedule(Runnable runnable, long delay, TimeUnit unit);

    void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    void shutdown();
}
