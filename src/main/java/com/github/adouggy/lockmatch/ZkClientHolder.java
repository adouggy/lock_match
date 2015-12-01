package com.github.adouggy.lockmatch;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;

public final class ZkClientHolder {
    private static final String LOCAL_DOMAIN = "localhost:2181,localhost:2182,localhost:2183";
    private static final String NAMESPACE = "lockTest";

    private static final int RETRY_INTERVAL = (int) SECONDS.toMillis(10);
    private static CuratorFramework tmp = null;

    static {
        tmp = CuratorFrameworkFactory.newClient(LOCAL_DOMAIN, new RetryForever(RETRY_INTERVAL));
        tmp.start();
        tmp = tmp.usingNamespace(NAMESPACE);
    }

    public static CuratorFramework get() {
        return tmp;
    }

}
