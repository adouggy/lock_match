package com.github.adouggy.lockmatch;

import static java.util.concurrent.TimeUnit.DAYS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.adouggy.lockmatch.DistributedReentrantLock.LockRevokeCallback;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Hello world!
 *
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        //        testRedis();
        testZookeeper();
    }

    private static void testRedis() {
        long time = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            final int ii = i;
            pool.execute(() -> {
                String s = SimpleRedisLock.callWithLock("lock1234", () -> {
                    counter.incrementAndGet();
                    return "running no." + ii;
                });
                logger.info(s);
            });
        }
        MoreExecutors.shutdownAndAwaitTermination(pool, 1, DAYS);
        logger.info("redis final counter:" + counter);
        logger.info("redis lock interval:" + (System.currentTimeMillis() - time));
    }

    private static void testZookeeper() {
        long time = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);

        //        try {
        //            byte[] bytes = ZkClientHolder.get().getData().forPath("/t2");
        //            System.out.println(new String(bytes));
        //        } catch (Exception e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }

        ExecutorService pool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            final int ii = i;
            pool.execute(() -> {
                String s = DistributedReentrantLock.callWithLock("/l1", () -> {
                    counter.incrementAndGet();
                    return "running no." + ii;
                } , new LockRevokeCallback() {

                    @Override
                    public void lockRevoked() throws Throwable {
                        logger.info("lockRevoked");
                    }
                });

                logger.info(s);
            });
        }
        MoreExecutors.shutdownAndAwaitTermination(pool, 1, DAYS);
        logger.info("zk final counter:" + counter);
        logger.info("zookeeper lock interval:" + (System.currentTimeMillis() - time));
    }
}
