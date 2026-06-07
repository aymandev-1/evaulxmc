package dev.evaulx.core.database.redis;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.configuration.ConfigurationSection;
import redis.clients.jedis.*;

import java.util.function.Consumer;

public class RedisManager {

    private static final String CHANNEL = "evaulxmc:sync";

    private final EvaulxCore plugin;
    private JedisPool pool;
    private boolean enabled;
    private PubSubThread pubSubThread;

    public RedisManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        enabled = plugin.getConfig().getBoolean("redis.enabled", false);
        if (!enabled) return true;
        try {
            String host = getRedisIp();
            int port = plugin.getConfig().getInt("redis.port", 6379);

            JedisPoolConfig cfg = new JedisPoolConfig();
            cfg.setMaxTotal(8);
            pool = new JedisPool(cfg, host, port, 2000);
            try (Jedis j = pool.getResource()) { j.ping(); }
            plugin.getLogger().info("Connected to Redis at " + host + ":" + port + ".");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Redis connection failed: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public void disconnect() {
        if (pubSubThread != null) pubSubThread.stop();
        if (pool != null && !pool.isClosed()) pool.close();
    }

    public void publish(String message) {
        if (!enabled || pool == null) return;
        TaskUtil.async(() -> {
            try (Jedis j = pool.getResource()) {
                j.publish(CHANNEL, message);
            } catch (Exception ignored) {}
        });
    }

    public void subscribe(Consumer<String> handler) {
        if (!enabled || pool == null) return;
        pubSubThread = new PubSubThread(pool, CHANNEL, handler);
        Thread t = new Thread(pubSubThread, "EvaulxMC-Redis-PubSub");
        t.setDaemon(true);
        t.start();
    }

    public boolean isEnabled() { return enabled; }

    private String getRedisIp() {
        ConfigurationSection redis = plugin.getConfig().getConfigurationSection("redis");
        if (redis != null && redis.getKeys(false).contains("ip")) {
            return redis.getString("ip", "localhost");
        }
        return plugin.getConfig().getString("redis.host", "localhost");
    }

    private static class PubSubThread implements Runnable {
        private final JedisPool pool;
        private final String channel;
        private final Consumer<String> handler;
        private JedisPubSub pubSub;
        private volatile boolean running = true;

        PubSubThread(JedisPool pool, String channel, Consumer<String> handler) {
            this.pool = pool;
            this.channel = channel;
            this.handler = handler;
        }

        @Override
        public void run() {
            while (running) {
                try (Jedis j = pool.getResource()) {
                    pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String ch, String msg) {
                            handler.accept(msg);
                        }
                    };
                    j.subscribe(pubSub, channel);
                } catch (Exception ignored) {
                    if (!running) break;
                    try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }

        void stop() {
            running = false;
            if (pubSub != null) try { pubSub.unsubscribe(); } catch (Exception ignored) {}
        }
    }
}
