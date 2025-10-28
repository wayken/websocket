package cloud.apposs.websocket;

import cloud.apposs.websocket.timer.HashedWheelTimer;
import cloud.apposs.websocket.timer.Timeout;
import cloud.apposs.websocket.timer.TimerTask;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestHashedWheelTimer {
    /**
     * 构建HashedWheelTimer时间轮
     * <p>
     * threadFactory：创建处理任务的线程工厂
     * tickDuration：100 ，表示每个时间格代表当前时间轮的基本时间跨度，这里是100ms，也就是指针100ms跳动一次，每次跳动一个窗格
     * ticksPerWheel：512，表示时间轮上一共有多少个时间格，分配的时间格越多，占用内存空间就越大
     * leakDetection：是否开启内存泄漏检测
     * maxPendingTimeouts[可选参数]，最大允许等待的任务数，默认没有限制
     * <p>
     * 最后通过newTimeout()把需要延迟执行的任务添加到时间轮中
     */
    private static final HashedWheelTimer HASHED_WHEEL_TIMER = new HashedWheelTimer(
            Executors.defaultThreadFactory(),
            100,
            TimeUnit.MILLISECONDS,
            512,
            true
    );

    @Test
    public void testNewTimeout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        long now = System.currentTimeMillis();
        System.out.println("延时任务提交：" + now);
        // 延时多久执行
        long delay = 2000L;
        HASHED_WHEEL_TIMER.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("延时任务触发：" + (System.currentTimeMillis() - now));
                latch.countDown();
            }
        }, delay, TimeUnit.MILLISECONDS);
        latch.await();
    }
}
