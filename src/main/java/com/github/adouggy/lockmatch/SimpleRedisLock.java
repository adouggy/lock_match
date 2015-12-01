package com.github.adouggy.lockmatch;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Throwables;

import redis.clients.jedis.JedisCluster;

public class SimpleRedisLock {

    public static class RedisLock implements Lock {

        private JedisCluster jedis = null;
        /**
         * resource name
         */
        private String resourceKey = null;
        /**
         * a unique value, to identity who can release this lock
         * can replace by a cheaper way
         */
        private String uuid = null;
        /**
         * a 30 seconds time windows to finish the job.
         */
        final long ttl = 30000;

        public RedisLock(JedisCluster jedis, String resourceKey) {
            this.jedis = jedis;
            this.resourceKey = resourceKey;
            this.uuid = UUID.randomUUID().toString();
        }

        @Override
        public void lock() {
            throw new NotImplementedException("lock not implement");
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new NotImplementedException("lock not implement");
        }

        @Override
        public boolean tryLock() {
            String s = jedis.set(this.resourceKey, this.uuid, "nx", "px", ttl);
            if (s == null) {
                return false;
            }
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new NotImplementedException("lock not implement");
        }

        /**
         * if the value changed, may the time window already passed
         * DON"T DEL if that happened
         */
        @Override
        public void unlock() {
            String oldValue = jedis.get(this.resourceKey);
            if( StringUtils.equals(oldValue, this.uuid) ){
                jedis.del(this.resourceKey);
            }
        }

        @Override
        public Condition newCondition() {
            throw new NotImplementedException("lock not implement");
        }

    }

    public static RedisLock getLock(String key) {
        return new RedisLock(RedisClientHolder.get(), key);
    }

    public static final <T> T callWithLock(String key, Supplier<T> call) {
        RedisLock lock = getLock(key);
        boolean getLock = false;
        do {
            try {
                if (lock.tryLock()) {
                    getLock = true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } while (!getLock);

        try {
            return call.get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                lock.unlock();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
