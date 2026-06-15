package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MailManager {

    public static final class MailMessage {
        private final String from;
        private final String message;
        private final long timestamp;
        private final boolean read;

        public MailMessage(String from, String message, long timestamp, boolean read) {
            this.from = from;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }

        public String from() { return from; }
        public String message() { return message; }
        public long timestamp() { return timestamp; }
        public boolean read() { return read; }
    }

    private final EvaulxCore plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, List<MailMessage>> cache = new HashMap<>();

    public MailManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/mail.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection section = config.getConfigurationSection(uuidStr);
                List<MailMessage> msgs = new ArrayList<>();
                if (section != null) {
                    for (String idx : section.getKeys(false)) {
                        msgs.add(new MailMessage(
                                section.getString(idx + ".from", "Server"),
                                section.getString(idx + ".message", ""),
                                section.getLong(idx + ".timestamp"),
                                section.getBoolean(idx + ".read")));
                    }
                }
                cache.put(uuid, msgs);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public List<MailMessage> getMail(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    public int getUnreadCount(UUID uuid) {
        return (int) getMail(uuid).stream().filter(m -> !m.read()).count();
    }

    public void sendMail(UUID to, String from, String message) {
        getMail(to).add(new MailMessage(from, message, System.currentTimeMillis(), false));
        save(to);
    }

    public void markAllRead(UUID uuid) {
        List<MailMessage> old = getMail(uuid);
        List<MailMessage> updated = new ArrayList<>();
        for (MailMessage m : old) updated.add(new MailMessage(m.from(), m.message(), m.timestamp(), true));
        cache.put(uuid, updated);
        save(uuid);
    }

    public void clearMail(UUID uuid) {
        cache.put(uuid, new ArrayList<>());
        config.set(uuid.toString(), null);
        saveAsync();
    }

    private void save(UUID uuid) {
        List<MailMessage> msgs = cache.get(uuid);
        config.set(uuid.toString(), null);
        if (msgs != null) {
            for (int i = 0; i < msgs.size(); i++) {
                MailMessage m = msgs.get(i);
                String base = uuid + "." + i;
                config.set(base + ".from", m.from());
                config.set(base + ".message", m.message());
                config.set(base + ".timestamp", m.timestamp());
                config.set(base + ".read", m.read());
            }
        }
        saveAsync();
    }

    private void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try { config.save(file); } catch (IOException e) {
                plugin.getLogger().warning("Failed to save mail: " + e.getMessage());
            }
        });
    }
}
