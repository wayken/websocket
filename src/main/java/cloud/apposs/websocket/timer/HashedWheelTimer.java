package cloud.apposs.websocket.timer;

import cloud.apposs.logger.Logger;
import cloud.apposs.util.StrUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h1>介绍</h1>
 * 时间轮是一种高效利用线程资源进行批量化调度的一种调度模型。
 * 通过把大批量的调度任务全部绑定到同一个调度器上，使用这一个调度器来进行所有任务的管理、触发、以及运行。
 * 时间轮是以时间作为刻度组成的一个环形队列，所以叫做时间轮。这个环形队列采用数组来实现 {@link HashedWheelBucket}，
 * 数组的每个元素称为槽，每个槽可以存放一个定时任务列表，叫 {@link HashedWheelBucket}，
 * 它是一个双向链表，链表的每个节点表示一个定时任务项 {@link HashedWheelTimeout}，其中封装了真正的定时任务 {@link TimerTask}
 * 时间轮由多个时间格组成，每个时间格代表当前时间轮的基本时间跨度（tickDuration），其中时间轮的时间格的个数是固定的。
 *
 * <h1>主要应用场景：</h1>
 * <pre>
 *     1. 如果一个系统存在大量的任务调度，时间轮可以高效的利用线程资源来进行批量化调度
 *     2. 把大批量的调度任务全部都绑定时间轮上，通过时间轮进行所有任务的管理，触发以及运行，能够高效地管理各种延时任务，周期任务，通知任务等
 * </pre>
 * <h1>参考链接</h1>
 * <pre>
 *     <a href="https://www.cnblogs.com/yangyongjie/p/15839713.html">Netty时间轮-HashedWheelTimer</a>
 *     <a href="https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/Netty%20%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E5%89%96%E6%9E%90%E4%B8%8E%20RPC%20%E5%AE%9E%E8%B7%B5-%E5%AE%8C/21%20%20%E6%8A%80%E5%B7%A7%E7%AF%87%EF%BC%9A%E5%BB%B6%E8%BF%9F%E4%BB%BB%E5%8A%A1%E5%A4%84%E7%90%86%E7%A5%9E%E5%99%A8%E4%B9%8B%E6%97%B6%E9%97%B4%E8%BD%AE%20HashedWheelTimer.md">技巧篇：延迟任务处理神器之时间轮 HashedWheelTimer</a>
 * </pre>
 */
public class HashedWheelTimer implements Timer {
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
    private static final int INSTANCE_COUNT_LIMIT = 64;
    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    // 指针转动和延时任务执行的线程
    private final Worker worker = new Worker();

    // worker任务封装的工作线程，用于指针转动和触发时间格里的延时任务的执行
    private final Thread workerThread;

    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState; // 0 - init, 1 - started, 2 - shut down

    // 每个时间格的时间跨度，默认为100ms
    private final long tickDuration;

    // 时间轮（环形数组），HashedWheelBucket 为每个时间格的槽
    private final HashedWheelBucket[] wheel;

    private final int mask;

    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    // 延时任务队列，队列中为等待被添加到时间轮的延时任务
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingQueue<HashedWheelTimeout>();

    // 保存已经取消的延时任务的队列
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new LinkedBlockingQueue<HashedWheelTimeout>();

    // 记录当前的任务数
    private final AtomicLong pendingTimeouts = new AtomicLong(0);

    // 最大的任务数
    private final long maxPendingTimeouts;

    // 执行延时任务的线程池
    private final Executor taskExecutor;

    // 工作线程启动时间
    private volatile long startTime;

    /**
     * 用默认线程池 ({@link Executors#defaultThreadFactory()}) 构建HashedWheelTimer 时间轮
     */
    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }

    /**
     * 用默认线程池 ({@link Executors#defaultThreadFactory()}) 构建HashedWheelTimer 时间轮
     *
     * @param tickDuration 每格的时间间隔，默认100ms，0.1秒
     * @param unit         时间单位，默认为毫秒
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }

    /**
     * 用默认线程池 ({@link Executors#defaultThreadFactory()}) 构建HashedWheelTimer 时间轮
     *
     * @param tickDuration  每格的时间间隔，默认100ms，0.1秒
     * @param unit          时间单位，默认为毫秒
     * @param ticksPerWheel 时间轮的格子数，默认为512；如果传入的不是2的N次方，则会调整为大于等于该参数的第一个2的N次方，好处是可以优化hash值的计算
     */
    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory 创建处理任务 ({@link TimerTask}) 的线程工厂
     */
    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory 创建处理任务 ({@link TimerTask}) 的线程工厂
     * @param tickDuration  每格的时间间隔，默认100ms，0.1秒
     * @param unit          时间单位，默认为毫秒
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory 创建处理任务 ({@link TimerTask}) 的线程工厂
     * @param tickDuration  每格的时间间隔，默认100ms，0.1秒
     * @param unit          时间单位，默认为毫秒
     * @param ticksPerWheel 时间轮的格子数，默认为512；如果传入的不是2的N次方，则会调整为大于等于该参数的第一个2的N次方，好处是可以优化hash值的计算
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, true);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory 创建处理任务 ({@link TimerTask}) 的线程工厂
     * @param tickDuration  每格的时间间隔，默认100ms，0.1秒
     * @param unit          时间单位，默认为毫秒
     * @param ticksPerWheel 时间轮的格子数，默认为512；如果传入的不是2的N次方，则会调整为大于等于该参数的第一个2的N次方，好处是可以优化hash值的计算
     * @param leakDetection 如果false，那么只有工作线程不是后台线程时才会追踪资源泄露，这个参数可以忽略
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit,
                            int ticksPerWheel, boolean leakDetection) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, leakDetection, -1);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory        创建处理任务 ({@link TimerTask}) 的线程工厂
     * @param tickDuration         每格的时间间隔，默认100ms，0.1秒
     * @param unit                 时间单位，默认为毫秒
     * @param ticksPerWheel        时间轮的格子数，默认为512；如果传入的不是2的N次方，则会调整为大于等于该参数的第一个2的N次方，好处是可以优化hash值的计算
     * @param leakDetection        如果false，那么只有工作线程不是后台线程时才会追踪资源泄露，这个参数可以忽略
     * @param  maxPendingTimeouts  最大的pending数量（时间轮中任务的最大数量），超过这个值之后调用将抛出异常，0或者负数表示没有限制，默认为-1
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel,
                            boolean leakDetection, long maxPendingTimeouts) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, leakDetection,
                maxPendingTimeouts, ImmediateExecutor.INSTANCE);
    }

    /**
     * 构建 HashedWheelTimer 时间轮
     *
     * @param threadFactory         创建处理任务 ({@link TimerTask}) 的线程工厂
     * @param tickDuration          每格的时间间隔，默认100ms，0.1秒
     * @param unit                  时间单位，默认为毫秒
     * @param ticksPerWheel         时间轮的格子数，默认为512；如果传入的不是2的N次方，则会调整为大于等于该参数的第一个2的N次方，好处是可以优化hash值的计算
     * @param leakDetection         如果false，那么只有工作线程不是后台线程时才会追踪资源泄露，这个参数可以忽略
     * @param maxPendingTimeouts    最大的pending数量（时间轮中任务的最大数量），超过这个值之后调用将抛出异常，0或者负数表示没有限制，默认为-1
     * @param taskExecutor          任务线程池，用于执行提交的任务，调用者负责在不需要时关闭它
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel,
                            boolean leakDetection, long maxPendingTimeouts, Executor taskExecutor) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration : " + tickDuration + " (expected: > 0)");
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel : " + tickDuration + " (expected: > 0)");
        }
        if (taskExecutor == null) {
            throw new NullPointerException("taskExecutor");
        }

        this.taskExecutor = taskExecutor;

        // 将ticksPerWheel（轮子上的时间格数）向上取值为2的次幂，方便进行求商和取余计算，并初始化时间轮
        this.wheel = createWheel(ticksPerWheel);

        // mask 的设计和HashMap一样，通过限制数组的大小为2的次方，利用位运算来替代取模运算，提高性能
        this.mask = wheel.length - 1;

        // Convert tickDuration to nanos.
        long duration = unit.toNanos(tickDuration);

        // 防止溢出
        // tickDuration * ticksPerWheel 必须小于Long.MAX_VALUE
        if (duration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format("tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                    tickDuration, Long.MAX_VALUE / wheel.length));
        }

        // tickDuration 不能小于 1ms
        if (duration < MILLISECOND_NANOS) {
            Logger.warn("Configured tickDuration {} smaller than {}, using 1ms.", tickDuration, MILLISECOND_NANOS);
            this.tickDuration = MILLISECOND_NANOS;
        } else {
            this.tickDuration = duration;
        }

        // 创建工作线程，用于指针转动和触发时间格里的延时任务的执行
        this.workerThread = threadFactory.newThread(worker);

        // 时间轮中任务的最大数量
        this.maxPendingTimeouts = maxPendingTimeouts;

        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
                WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            reportTooManyInstances();
        }
    }

    /**
     * 添加延时任务
     * @param task  任务
     * @param delay 延时时间
     * @param unit  延时时间单位
     */
    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        // 任务数+1
        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();
        // 如果任务数超过最大限制，那么则抛出异常
        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }
        // 启动时间轮开启任务工作线程workerThread
        start();
        // 将延时任务添加到延时队列中，该队列将在下一个滴答声中处理（指针的下一次转动）
        // 在处理过程中，所有排队的hashedwheeltimeout将被添加到正确的HashedWheelBucket
        // 计算延时任务的延时时间，值为当前的时间+当前任务执行的延迟时间-时间轮启动的时间
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        // 防止溢出
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        // 封装延时任务
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        // 将延时任务保存到延时任务队列中
        timeouts.add(timeout);
        return timeout;
    }

    /**
     * 显式启动后台线程，即使没有调用此方法，后台线程也会按需自动启动
     */
    public void start() {
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }

        // Wait until the startTime is initialized by the worker.
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - it will be ready very soon.
            }
        }
    }

    @Override
    public Set<Timeout> stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(HashedWheelTimer.class.getSimpleName() +
                    ".stop() cannot be called from " + TimerTask.class.getSimpleName());
        }

        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // workerState can be 0 or 2 at this moment - let it always be 2.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }

            return Collections.emptySet();
        }

        try {
            boolean interrupted = false;
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            INSTANCE_COUNTER.decrementAndGet();
        }
        return worker.unprocessedTimeouts();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            // This object is going to be GCed and it is assumed the ship has sailed to do a proper shutdown. If
            // we have not yet shutdown then we want to make sure we decrement the active instance count.
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    /**
     * 初始化时间轮环形数组
     */
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        // ticksPerWheel不能大于2^30
        if (ticksPerWheel < 1 || ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException("ticksPerWheel: " + ticksPerWheel + " (expected: " + 1 + "-" + 1073741824 + ")");
        }
        // 将ticksPerWheel（轮子上的时间格数）向上取值为2的次幂
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        // 创建时间轮环形数组
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i ++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 将ticksPerWheel（轮子上的时间格数）向上取值为2的次幂
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }

    /**
     * Returns the number of pending timeouts of this {@link Timer}.
     */
    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    private static void reportTooManyInstances() {
        String resourceType = StrUtil.simpleClassName(HashedWheelTimer.class);
        Logger.error("You are creating too many " + resourceType + " instances. " + resourceType +
                " is a shared resource that must be reused across the JVM, " + "so that only a few instances are created.");
    }

    /**
     * 指针转动和延时任务执行的线程
     */
    private final class Worker implements Runnable {
        // 用于记录未执行的延时任务
        private final Set<Timeout> unprocessedTimeouts = new HashSet<Timeout>();

        // 总的tick数（指针嘀嗒的次数）
        private long tick;

        @Override
        public void run() {
            // 工作线程（时间轮）启动时间
            startTime = System.nanoTime();
            if (startTime == 0) {
                // 这里使用0作为未初始化值的指示符，所以要确保初始化时它不是0
                startTime = 1;
            }

            // 唤醒被阻塞的start()方法，通知时间轮已经启动完毕
            startTimeInitialized.countDown();

            do {
                // 这里会休眠tick的时间，模拟指针走动
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    // 计算时间轮的槽位
                    int idx = (int) (tick & mask);
                    // 清理已经取消的任务
                    processCancelledTasks();
                    // 得到当前指针位置的时间槽
                    HashedWheelBucket bucket = wheel[idx];
                    // 将newTimeout()方法中加入到待处理定时任务队列中的任务加入到指定的格子中
                    transferTimeoutsToBuckets();
                    // 运行目前指针指向的槽中的bucket链表中的任务，执行到期的延时任务，交给taskExecutor线程池去执行
                    bucket.expireTimeouts(deadline);
                    tick++;
                }
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

            // Fill the unprocessedTimeouts so we can return them from stop() method.
            for (HashedWheelBucket bucket: wheel) {
                // 清除时间轮中不需要处理的任务
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            for (;;) {
                // 遍历任务队列，发现如果有任务被取消，则添加到unprocessedTimeouts，也就是不需要处理的队列中
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    // 如果延时任务没被取消，记录到未执行的任务Set集合中
                    unprocessedTimeouts.add(timeout);
                }
            }
            // 处理被取消的任务
            processCancelledTasks();
        }

        /**
         * 将延时任务队列中等待添加到时间轮中的延时任务转移到时间轮的指定位置
         */
        private void transferTimeoutsToBuckets() {
            // 每次转移10w个延时任务
            for (int i = 0; i < 100000; i++) {
                // 从队列中出队一个延时任务
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    // Was cancelled in the meantime.
                    continue;
                }
                // 到期一共需要走多少时间格（tick次数），deadline表示当前任务的延迟时间（从时间轮启动时计算），tickDuration表示时间格的时间间隔
                long calculated = timeout.deadline / tickDuration;
                // tick已经走了的时间格，到期一共还需要需要走多少圈
                timeout.remainingRounds = (calculated - tick) / wheel.length;
                // 如果延时任务在队列中等待太久已经过了执行时间，那么这个时候就使用当前tick，也就是放在当前的bucket，此方法调用完后就会被执行
                final long ticks = Math.max(calculated, tick);
                // 槽的索引，stopIndex = tick 次数 & mask, mask = wheel.length - 1
                int stopIndex = (int) (ticks & mask);
                // 根据索引该任务应该放到的槽
                HashedWheelBucket bucket = wheel[stopIndex];
                // 将任务添加到槽中，链表末尾
                bucket.addTimeout(timeout);
            }
        }

        /**
         * 处理取消掉的延时任务
         */
        private void processCancelledTasks() {
            for (;;) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                try {
                    timeout.remove();
                } catch (Throwable t) {
                    Logger.warn("An exception was thrown while process a cancellation task", t);
                }
            }
        }

        /**
         * 从时间轮的启动时间startTime和当前的tick数（指针转动次数）计算下一次指针转动的时间，然后休眠等待下一次指针转动时间到来
         */
        private long waitForNextTick() {
            // deadline返回的是下一次时间轮指针转动的时间与时间格启动的时间间隔
            long deadline = tickDuration * (tick + 1);
            for (;;) {
                // 计算当前时间距离启动时间的时间间隔
                final long currentTime = System.nanoTime() - startTime;
                // 距离下一次指针转动还需休眠多长时间
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;
                // 到了指针调到下一个槽位的时间
                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }

                try {
                    // 表示距离下一次指针转动还需要一段时间，所以休眠等待时间的到来
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        /**
         * 记录未执行的延时任务
         */
        public Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    /**
     * HashedWheelBucket 是一个｛@link HashedWheelTimeout} 的双向链表，
     * 链表的每个节点表示一个定时任务项（HashedWheelTimeout），其中封装了真正的定时任务 {@link TimerTask}
     */
    private static final class HashedWheelBucket {
        // 任务头结点和尾节点，方便对任务进行提取和插入
        private HashedWheelTimeout head;
        private HashedWheelTimeout tail;

        /**
         * Add {@link HashedWheelTimeout} to this bucket.
         */
        public void addTimeout(HashedWheelTimeout timeout) {
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        /**
         * Expire all {@link HashedWheelTimeout}s for the given {@code deadline}.
         */
        public void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;

            // 遍历当前时间槽中的所有任务
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    // 从链表中移除
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        timeout.expire();
                    } else {
                        // The timeout was placed into a wrong slot. This should never happen.
                        throw new IllegalStateException(String.format(
                                "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (timeout.isCancelled()) {
                    next = remove(timeout);
                } else {
                    // 任务还没到期，剩余的轮数-1
                    timeout.remainingRounds --;
                }
                // 将指针放置到下一个延时任务上
                timeout = next;
            }
        }

        /**
         * 删除槽中链表中的延时任务
         */
        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            // remove timeout that was either processed or cancelled by updating the linked-list
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }

            if (timeout == head) {
                // if timeout is also the tail we need to adjust the entry too
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                // if the timeout is the tail modify the tail to be the prev node.
                tail = timeout.prev;
            }
            // null out prev, next and bucket to allow for GC.
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        /**
         * Clear this bucket and return all not expired / cancelled {@link Timeout}s.
         */
        public void clearTimeouts(Set<Timeout> set) {
            for (;;) {
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                tail = this.head =  null;
            } else {
                this.head = next;
                next.prev = null;
            }

            // null out prev and next to allow for GC.
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }

    /**
     * 定时任务项，其中封装了真正的定时任务 {@link TimerTask}
     */
    private static final class HashedWheelTimeout implements Timeout, Runnable {
        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");

        private final HashedWheelTimer timer;
        private final TimerTask task;
        // 任务执行的截止时间，值为当前时间 + 延时任务延时时间 - 时间轮启动时间
        private final long deadline;

        @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization" })
        private volatile int state = ST_INIT;

        // 剩下的圈（轮）数
        // remainingRounds 将由 Worker.transferTimeoutsToBuckets() 在
        // HashedWheelTimeout 被添加到正确的 HashedWheelBucket 之前计算和设置
        long remainingRounds;

        // HashedWheelTimerBucket 槽中的延时任务列表是一个双向链表
        // 因为只有 workerThread 会对它进行操作，所以不需要 synchronization / volatile.
        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        // 当前延时任务所插入时间轮的哪个槽
        HashedWheelBucket bucket;

        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean cancel() {
            // only update the state it will be removed from HashedWheelBucket on next tick.
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            // If a task should be canceled we put this to another queue which will be processed on each tick.
            // So this means that we will have a GC latency of max. 1 tick duration which is good enough. This way
            // we can make again use of our MpscLinkedQueue and so minimize the locking / overhead as much as possible.
            timer.cancelledTimeouts.add(this);
            return true;
        }

        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        public void expire() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }

            try {
                timer.taskExecutor.execute(this);
            } catch (Throwable t) {
                Logger.warn("An exception was thrown while submit " + TimerTask.class.getSimpleName() + " for execution.", t);
            }
        }

        @Override
        public void run() {
            try {
                task.run(this);
            } catch (Throwable t) {
                Logger.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
            }
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;
            StringBuilder buffer = new StringBuilder(192)
                    .append(StrUtil.simpleClassName(this))
                    .append('(')
                    .append("deadline: ");
            if (remaining > 0) {
                buffer.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buffer.append(-remaining).append(" ns ago");
            } else {
                buffer.append("now");
            }
            if (isCancelled()) {
                buffer.append(", cancelled");
            }
            return buffer.append(", task: ").append(task()).append(')').toString();
        }
    }
}
