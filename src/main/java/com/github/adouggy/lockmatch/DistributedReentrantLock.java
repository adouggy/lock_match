package com.github.adouggy.lockmatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class DistributedReentrantLock {

    public interface LockRevokeCallback {

        void lockRevoked() throws Throwable;
    }

    private static Logger logger = LoggerFactory.getLogger(DistributedReentrantLock.class);

    private static Map<String, InterProcessMutex> locks = new ConcurrentHashMap<>();

    private static Map<String, CuratorFramework> clients = new ConcurrentHashMap<>();

    public static final InterProcessMutex getLock(String path) {
        CuratorFramework client = ZkClientHolder.get();
        clients.computeIfAbsent(path, i -> client);
        return locks.computeIfAbsent(path, k -> new InterProcessMutex(client, k));
    }

    public static final <T> T callWithLock(String path, Supplier<T> call,
            LockRevokeCallback lockRevokeCallback) {
        InterProcessMutex lock = getLock(path);
        CuratorFramework curator = clients.get(path);
        ConnectionStateListener lostListener = (client, newState) -> {
            if (newState == ConnectionState.LOST || newState == ConnectionState.SUSPENDED) {
                logger.warn("lock:[{}] is revoked because connection state: {}", path, newState);
                if (lockRevokeCallback != null) {
                    try {
                        lockRevokeCallback.lockRevoked();
                    } catch (Throwable e) {
                        logger.error("Ops. fail to revoke lock:{}", lockRevokeCallback, e);
                    }
                }
            }
        };
        boolean getLock = false;
        do {
            try {
                lock.acquire();
                getLock = true;
                curator.getConnectionStateListenable().addListener(lostListener);
                logger.info("acquired lock:{}", path);
            } catch (Throwable e) {
                logger.error("Ops. fail to aqurire lock.", e);
            }
        } while (!getLock);

        try {
            return call.get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                lock.release();
                curator.getConnectionStateListenable().removeListener(lostListener);
                logger.info("released lock:{}", path);
            } catch (Throwable e) {
                logger.error("fail to release lock.", e);
            }
        }
    }
}