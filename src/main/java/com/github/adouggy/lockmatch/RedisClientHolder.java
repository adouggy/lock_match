package com.github.adouggy.lockmatch;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public final class RedisClientHolder {

    private static Logger logger = LoggerFactory.getLogger(RedisClientHolder.class);

    private static final String DEFAULT_TYPE = "test";
    private static final ConcurrentHashMap<String, JedisCluster> map = new ConcurrentHashMap<>();

    public static JedisCluster get(String type) {
        return map.computeIfAbsent(type, typeName -> {
            Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
            jedisClusterNodes.add(new HostAndPort("127.0.0.1", 6488));
            jedisClusterNodes.add(new HostAndPort("127.0.0.1", 6489));
            jedisClusterNodes.add(new HostAndPort("127.0.0.1", 6490));
            logger.info("jedis cluster created:" + typeName);
            return new JedisCluster(jedisClusterNodes);
        });
    }

    public static JedisCluster get() {
        return get(DEFAULT_TYPE);
    }

}
