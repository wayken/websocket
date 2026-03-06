package cloud.apposs.websocket.scheduler;

import cloud.apposs.websocket.timer.HashedWheelTimer;
import cloud.apposs.websocket.timer.Timeout;
import cloud.apposs.websocket.timer.TimerTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HashedWheelScheduler implements CancelableScheduler {
    private final HashedWheelTimer executorService;
    private final Map<SchedulerKey, Timeout> scheduledFutures = new ConcurrentHashMap<>();
    
    public HashedWheelScheduler() {
        executorService = new HashedWheelTimer();
    }
    
    public HashedWheelScheduler(ThreadFactory threadFactory) {
        executorService = new HashedWheelTimer(threadFactory);
    }

    @Override
    public void cancel(SchedulerKey key) {
        Timeout timeout = scheduledFutures.remove(key);
        if (timeout != null) {
            timeout.cancel();
        }
    }

    @Override
    public void schedule(final Runnable runnable, long delay, TimeUnit unit) {
        executorService.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                runnable.run();
            }
        }, delay, unit);
    }

    @Override
    public void schedule(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        Timeout timeout = executorService.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                try {
                    runnable.run();
                } finally {
                    scheduledFutures.remove(key);
                }
            }
        }, delay, unit);

        if (!timeout.isExpired()) {
            scheduledFutures.put(key, timeout);
        }
    }

    @Override
    public void shutdown() {
        executorService.stop();
    }
}
