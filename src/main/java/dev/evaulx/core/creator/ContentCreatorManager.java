package dev.evaulx.core.creator;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Rank;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ContentCreatorManager {

    private final EvaulxCore plugin;
    private final File dataFile;
    private YamlConfiguration yaml;

    private final Map<UUID, ContentCreatorProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, UUID> codeIndex = new ConcurrentHashMap<>();

    // Feature state
    private final Set<UUID> liveCreators = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> goLiveCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> giveawayCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> socialsCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shoutoutCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new ConcurrentHashMap<>();
    private Object ccBossBar; // BossBar via reflection (API added 1.9)

    public ContentCreatorManager(EvaulxCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getEvaulxDataFolder(), "content_creators.yml");
        load();
        try {
            initBossBar();
        } catch (Throwable ignored) {
            // BossBar API unavailable (pre-1.9 server); CC boss bar disabled
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        yaml = YamlConfiguration.loadConfiguration(dataFile);
        profiles.clear();
        codeIndex.clear();
        if (!yaml.isConfigurationSection("creators")) return;
        for (String key : yaml.getConfigurationSection("creators").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "creators." + key + ".";
                ContentCreatorProfile p = new ContentCreatorProfile(uuid);
                p.setName(yaml.getString(base + "name", ""));
                p.setDisplayName(yaml.getString(base + "display-name", ""));
                p.setYoutube(yaml.getString(base + "youtube", ""));
                p.setTwitch(yaml.getString(base + "twitch", ""));
                p.setTwitter(yaml.getString(base + "twitter", ""));
                p.setRewardCode(yaml.getString(base + "reward-code", ""));
                p.setDescription(yaml.getString(base + "description", ""));
                p.setStreamTitle(yaml.getString(base + "stream-title", ""));
                p.setTiktok(yaml.getString(base + "tiktok", ""));
                p.setInstagram(yaml.getString(base + "instagram", ""));
                p.setDiscord(yaml.getString(base + "discord", ""));
                profiles.put(uuid, p);
                if (!p.getRewardCode().isEmpty()) codeIndex.put(p.getRewardCode(), uuid);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void write(ContentCreatorProfile p) {
        String base = "creators." + p.getUuid() + ".";
        yaml.set(base + "name", p.getName());
        yaml.set(base + "display-name", p.getDisplayName());
        yaml.set(base + "youtube", p.getYoutube());
        yaml.set(base + "twitch", p.getTwitch());
        yaml.set(base + "twitter", p.getTwitter());
        yaml.set(base + "reward-code", p.getRewardCode());
        yaml.set(base + "description", p.getDescription());
        yaml.set(base + "stream-title", p.getStreamTitle());
        yaml.set(base + "tiktok", p.getTiktok());
        yaml.set(base + "instagram", p.getInstagram());
        yaml.set(base + "discord", p.getDiscord());
        flush();
    }

    private void flush() {
        TaskUtil.async(() -> {
            try { yaml.save(dataFile); } catch (IOException ignored) {}
        });
    }

    // ── Profile management ────────────────────────────────────────────────────

    public boolean isCreator(UUID uuid) { return profiles.containsKey(uuid); }
    public ContentCreatorProfile getProfile(UUID uuid) { return profiles.get(uuid); }
    public Collection<ContentCreatorProfile> getAllProfiles() { return profiles.values(); }
    public boolean isLive(UUID uuid) { return liveCreators.contains(uuid); }

    public ContentCreatorProfile grant(UUID uuid, String name) {
        ContentCreatorProfile p = profiles.computeIfAbsent(uuid, ContentCreatorProfile::new);
        if (name != null && !name.isEmpty()) p.setName(name);
        write(p);
        return p;
    }

    public void revoke(UUID uuid) {
        ContentCreatorProfile p = profiles.remove(uuid);
        if (p != null && !p.getRewardCode().isEmpty()) codeIndex.remove(p.getRewardCode());
        liveCreators.remove(uuid);
        stopTrail(uuid);
        yaml.set("creators." + uuid, null);
        flush();
        updateBossBar();
    }

    public void updateProfile(ContentCreatorProfile profile) {
        codeIndex.values().removeIf(u -> u.equals(profile.getUuid()));
        if (!profile.getRewardCode().isEmpty()) codeIndex.put(profile.getRewardCode(), profile.getUuid());
        write(profile);
    }

    public String getCCChatTag() {
        return plugin.getConfig().getString("content-creators.chat-tag", "&c[CC] &r");
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void initBossBar() {
        try {
            Class<?> colorClass = Class.forName("org.bukkit.boss.BarColor");
            Class<?> styleClass = Class.forName("org.bukkit.boss.BarStyle");
            Object yellow = Enum.valueOf((Class<Enum>) colorClass, "YELLOW");
            Object solid  = Enum.valueOf((Class<Enum>) styleClass, "SOLID");
            ccBossBar = Bukkit.class
                    .getMethod("createBossBar", String.class, colorClass, styleClass)
                    .invoke(null, CC.color("&6&l✦ &eContent Creators Online &8» &7/creator list"), yellow, solid);
            bbCall("setVisible", boolean.class, false);
            for (Player p : Bukkit.getOnlinePlayers()) bbCall("addPlayer", Player.class, p);
        } catch (Throwable ignored) {
            ccBossBar = null;
        }
    }

    private void bbCall(String method, Class<?> paramType, Object arg) {
        if (ccBossBar == null) return;
        try { ccBossBar.getClass().getMethod(method, paramType).invoke(ccBossBar, arg); } catch (Throwable ignored) {}
    }

    public void addPlayerToBossBar(Player player) {
        bbCall("addPlayer", Player.class, player);
    }

    public void removePlayerFromBossBar(Player player) {
        bbCall("removePlayer", Player.class, player);
    }

    public void updateBossBar() {
        if (ccBossBar == null) return;
        try {
            if (!plugin.getConfig().getBoolean("content-creators.bossbar", true)) {
                bbCall("setVisible", boolean.class, false);
                return;
            }
            List<String> parts = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!isCreator(p.getUniqueId())) continue;
                ContentCreatorProfile prof = getProfile(p.getUniqueId());
                String entry = "&e" + prof.effectiveDisplayName();
                if (liveCreators.contains(p.getUniqueId())) entry += " &c[LIVE]";
                parts.add(entry);
            }
            if (parts.isEmpty()) {
                bbCall("setVisible", boolean.class, false);
                return;
            }
            String title = CC.color("&6&l✦ &7CCs: " + String.join(" &8| ", parts) + " &8» &7/creator list");
            if (title.length() > 64) title = title.substring(0, 64);
            bbCall("setTitle", String.class, title);
            bbCall("setVisible", boolean.class, true);
        } catch (Throwable ignored) {}
    }

    // ── Particle trails ───────────────────────────────────────────────────────

    public void startTrail(Player player) {
        if (!plugin.getConfig().getBoolean("content-creators.particle-trail", true)) return;
        stopTrail(player.getUniqueId());
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { stopTrail(player.getUniqueId()); return; }
            try {
                Location loc = player.getLocation().add(0, 0.15, 0);
                Class<?> particleClass = Class.forName("org.bukkit.Particle");
                Object endRod = particleClass.getField("END_ROD").get(null);
                loc.getWorld().getClass().getMethod("spawnParticle",
                        particleClass, Location.class, int.class,
                        double.class, double.class, double.class, double.class)
                    .invoke(loc.getWorld(), endRod, loc, 3, 0.25, 0.4, 0.25, 0.02);
            } catch (Throwable ignored) {}
        }, 0L, 5L);
        particleTasks.put(player.getUniqueId(), task);
    }

    public void stopTrail(UUID uuid) {
        BukkitTask t = particleTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    // ── Join / Quit handling ──────────────────────────────────────────────────

    public void onCreatorJoin(Player player) {
        updateBossBar();
        startTrail(player);
    }

    public void onCreatorQuit(Player player) {
        liveCreators.remove(player.getUniqueId());
        stopTrail(player.getUniqueId());
        updateBossBar();
    }

    // ── Join announcement ─────────────────────────────────────────────────────

    public void announceJoin(Player player) {
        ContentCreatorProfile p = profiles.get(player.getUniqueId());
        if (p == null) return;
        if (!plugin.getConfig().getBoolean("content-creators.announce-join", true)) return;

        String name = p.effectiveDisplayName();
        List<String> lines = new ArrayList<>();
        lines.add(CC.color(" "));
        lines.add(CC.color("&8&m          &r &6&l✦ CONTENT CREATOR &8&m          "));
        lines.add(CC.color("  &e" + name + " &7has joined the server!"));
        if (!p.getDescription().isEmpty())
            lines.add(CC.color("  &7" + p.getDescription()));
        if (!p.getStreamTitle().isEmpty())
            lines.add(CC.color("  &f\"" + p.getStreamTitle() + "\""));
        if (!p.getYoutube().isEmpty())
            lines.add(CC.color("  &cYouTube  &8» &7youtube.com/" + p.getYoutube()));
        if (!p.getTwitch().isEmpty())
            lines.add(CC.color("  &5Twitch   &8» &7twitch.tv/" + p.getTwitch()));
        if (!p.getTiktok().isEmpty())
            lines.add(CC.color("  &fTikTok   &8» &7tiktok.com/@" + p.getTiktok()));
        if (!p.getTwitter().isEmpty())
            lines.add(CC.color("  &9Twitter  &8» &7twitter.com/" + p.getTwitter()));
        if (!p.getInstagram().isEmpty())
            lines.add(CC.color("  &dInstagram &8» &7instagram.com/" + p.getInstagram()));
        if (!p.getDiscord().isEmpty())
            lines.add(CC.color("  &bDiscord  &8» &7" + p.getDiscord()));
        if (!p.getRewardCode().isEmpty())
            lines.add(CC.color("  &7Use &b/redeemcode " + p.getRewardCode().toUpperCase(Locale.ENGLISH) + " &7for free rewards!"));
        lines.add(CC.color("&8&m                                                "));
        lines.add(CC.color(" "));
        for (String line : lines) Bukkit.broadcastMessage(line);
    }

    // ── Go Live / Off Air ─────────────────────────────────────────────────────

    /**
     * Returns null on success, or an error message on failure.
     */
    public String goLive(Player player, String platform) {
        if (!isCreator(player.getUniqueId())) return "&cYou are not a registered content creator.";
        if (!plugin.getConfig().getBoolean("content-creators.golive.enabled", true))
            return "&cThe go-live feature is disabled.";

        long cooldownSec = plugin.getConfig().getLong("content-creators.golive.cooldown-seconds", 300);
        Long lastUsed = goLiveCooldowns.get(player.getUniqueId());
        if (lastUsed != null) {
            long remaining = cooldownSec - (System.currentTimeMillis() - lastUsed) / 1000;
            if (remaining > 0) return "&cYou must wait &e" + remaining + "s &cbefore going live again.";
        }

        goLiveCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        liveCreators.add(player.getUniqueId());
        updateBossBar();

        ContentCreatorProfile p = profiles.get(player.getUniqueId());
        String name = p.effectiveDisplayName();
        String streamUrl = resolvePlatformUrl(p, platform);

        // Chat broadcast
        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m        &r &c&l🔴 CONTENT CREATOR IS LIVE &8&m        "));
        Bukkit.broadcastMessage(CC.color("  &e" + name + " &7is now streaming live!"));
        if (!p.getStreamTitle().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &f\"" + p.getStreamTitle() + "\""));
        if (!streamUrl.isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Stream at: &b" + streamUrl));
        if (!p.getRewardCode().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Use code &b" + p.getRewardCode().toUpperCase(Locale.ENGLISH) + " &7for free rewards!"));
        Bukkit.broadcastMessage(CC.color("&8&m                                              "));
        Bukkit.broadcastMessage(CC.color(" "));

        // Title + sound for all players
        String finalTitle = CC.color("&c&l🔴 NOW LIVE");
        String finalSub   = CC.color("&e" + name + " &7is streaming!");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(finalTitle, finalSub);
            try { online.playSound(online.getLocation(), Sound.valueOf("UI_TOAST_CHALLENGE_COMPLETE"), 0.6f, 1.0f); }
            catch (Throwable ignored) {
                try { online.playSound(online.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.6f, 1.0f); }
                catch (Throwable ignored2) {}
            }
        }

        // Fireworks at CC's location
        spawnWinFireworks(player.getLocation());
        return null;
    }

    public void goOffAir(Player player) {
        liveCreators.remove(player.getUniqueId());
        updateBossBar();
        if (!isCreator(player.getUniqueId())) return;
        ContentCreatorProfile p = profiles.get(player.getUniqueId());
        String name = p.effectiveDisplayName();
        Bukkit.broadcastMessage(CC.color("&8[&7CC&8] &e" + name + " &7has ended their stream. Thanks for watching!"));
    }

    // ── Giveaway (rank-based: VIP or Evaulx) ─────────────────────────────────

    /**
     * Returns null on success, or an error message on failure.
     * Picks randomly between VIP and Evaulx ranks (configurable in content-creators.giveaway.prizes).
     */
    public String runGiveaway(Player cc) {
        if (!isCreator(cc.getUniqueId())) return "&cYou are not a registered content creator.";

        long cooldownSec = plugin.getConfig().getLong("content-creators.giveaway.cooldown-seconds", 600);
        Long lastUsed = giveawayCooldowns.get(cc.getUniqueId());
        if (lastUsed != null) {
            long remaining = cooldownSec - (System.currentTimeMillis() - lastUsed) / 1000;
            if (remaining > 0) return "&cGiveaway cooldown: &e" + remaining + "s &cremaining.";
        }

        List<Player> eligible = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(cc.getUniqueId()))
                .filter(p -> !p.hasPermission("evaulx.vanish"))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) return "&cNo eligible players online to win!";

        // Build prize pool — defaults to VIP (7d) and Evaulx (30d)
        List<Object[]> prizePool = new ArrayList<>(); // [rankName, durationDays]
        List<Map<?, ?>> configured = plugin.getConfig().getMapList("content-creators.giveaway.prizes");
        if (configured.isEmpty()) {
            prizePool.add(new Object[]{"VIP", 7});
            prizePool.add(new Object[]{"Evaulx", 30});
        } else {
            for (Map<?, ?> prize : configured) {
                Object rankObj = prize.get("rank");
                Object durObj = prize.get("duration-days");
                String rankName = rankObj != null ? String.valueOf(rankObj) : "VIP";
                int dur;
                try { dur = durObj != null ? Integer.parseInt(String.valueOf(durObj)) : 7; }
                catch (NumberFormatException e) { dur = 7; }
                prizePool.add(new Object[]{rankName, dur});
            }
        }

        Object[] chosen = prizePool.get(new Random().nextInt(prizePool.size()));
        String rankName = (String) chosen[0];
        int durationDays = (Integer) chosen[1];

        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) return "&cGiveaway rank '&e" + rankName + "&c' doesn't exist. Check config.";

        Player winner = eligible.get(new Random().nextInt(eligible.size()));
        giveawayCooldowns.put(cc.getUniqueId(), System.currentTimeMillis());

        long expiresAt = durationDays > 0
                ? System.currentTimeMillis() + (long) durationDays * 86_400_000L : -1L;
        String durationStr = durationDays > 0
                ? durationDays + " day" + (durationDays == 1 ? "" : "s") : "permanently";

        ContentCreatorProfile p = profiles.get(cc.getUniqueId());
        String ccName = p.effectiveDisplayName();

        // Grant the rank via GrantManager (handles permissions, name tags, and expiry)
        plugin.getGrantManager().grant(
                Bukkit.getConsoleSender(), winner, rank, expiresAt,
                "CC Giveaway by " + ccName);

        // Broadcast
        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m        &r &6&l🎉 CC GIVEAWAY &8&m        "));
        Bukkit.broadcastMessage(CC.color("  &e" + ccName + " &7just ran a giveaway!"));
        Bukkit.broadcastMessage(CC.color("  &6&l" + winner.getName()
                + " &7won &6" + rank.getName() + " &7(" + durationStr + ")&7!"));
        Bukkit.broadcastMessage(CC.color("  &7Watch &e" + ccName
                + (!p.getTwitch().isEmpty() ? " &7live at &5twitch.tv/" + p.getTwitch()
                   : !p.getYoutube().isEmpty() ? " &7at &cyoutube.com/" + p.getYoutube() : "")));
        Bukkit.broadcastMessage(CC.color("&8&m                                        "));
        Bukkit.broadcastMessage(CC.color(" "));

        winner.sendTitle(CC.color("&6&l🎉 YOU WON!"),
                CC.color("&e" + ccName + "&7's giveaway &8— &6" + rank.getName()));
        spawnBurst(winner.getLocation(), Color.fromRGB(0xFF, 0xD7, 0x00), Color.WHITE);
        return null;
    }

    // ── Milestone ─────────────────────────────────────────────────────────────

    public String broadcastMilestone(Player cc, String message) {
        if (!isCreator(cc.getUniqueId())) return "&cYou are not a registered content creator.";
        ContentCreatorProfile p = profiles.get(cc.getUniqueId());
        String name = p.effectiveDisplayName();
        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m      &r &d&l★ CC MILESTONE &8&m      "));
        Bukkit.broadcastMessage(CC.color("  &e" + name + " &7reached a milestone!"));
        Bukkit.broadcastMessage(CC.color("  &f\"" + message + "\""));
        if (!p.getYoutube().isEmpty() || !p.getTwitch().isEmpty()) {
            String link = !p.getTwitch().isEmpty() ? "twitch.tv/" + p.getTwitch() : "youtube.com/" + p.getYoutube();
            Bukkit.broadcastMessage(CC.color("  &7Support them at &b" + link));
        }
        Bukkit.broadcastMessage(CC.color("&8&m                                    "));
        Bukkit.broadcastMessage(CC.color(" "));
        spawnWinFireworks(cc.getLocation());
        return null;
    }

    // ── Subscriber codes ──────────────────────────────────────────────────────

    public boolean redeemCode(Player player, String rawCode) {
        String code = rawCode.toLowerCase(Locale.ENGLISH).trim();
        UUID ccUuid = codeIndex.get(code);
        if (ccUuid == null) return false;

        ContentCreatorProfile cc = profiles.get(ccUuid);
        if (cc == null) return false;

        String redeemedKey = "redeemed." + code + "." + player.getUniqueId();
        if (yaml.getBoolean(redeemedKey, false)) {
            player.sendMessage(CC.color("&cYou have already redeemed code &e" + code.toUpperCase(Locale.ENGLISH) + "&c."));
            return true;
        }

        yaml.set(redeemedKey, true);
        flush();

        List<String> commands = plugin.getConfig().getStringList("content-creators.code-reward-commands");
        if (!commands.isEmpty()) {
            TaskUtil.sync(() -> {
                for (String cmd : commands) {
                    String resolved = cmd
                            .replace("{player}", player.getName())
                            .replace("{code}", code)
                            .replace("{cc}", cc.effectiveDisplayName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                }
            });
        }

        String ccName = cc.effectiveDisplayName();
        player.sendMessage(CC.color(" "));
        player.sendMessage(CC.color("&a&l✔ Code Redeemed! &7Code &b" + code.toUpperCase(Locale.ENGLISH)));
        player.sendMessage(CC.color("  &7Thanks for supporting &e" + ccName + "&7! Your rewards are on their way."));
        player.sendMessage(CC.color(" "));
        return true;
    }

    // ── Socials broadcast ─────────────────────────────────────────────────────

    public String broadcastSocials(Player cc) {
        if (!isCreator(cc.getUniqueId())) return "&cYou are not a registered content creator.";
        long cooldownSec = plugin.getConfig().getLong("content-creators.socials-cooldown-seconds", 300);
        Long last = socialsCooldowns.get(cc.getUniqueId());
        if (last != null) {
            long remaining = cooldownSec - (System.currentTimeMillis() - last) / 1000;
            if (remaining > 0) return "&cSocials cooldown: &e" + remaining + "s &cremaining.";
        }
        socialsCooldowns.put(cc.getUniqueId(), System.currentTimeMillis());

        ContentCreatorProfile p = profiles.get(cc.getUniqueId());
        String name = p.effectiveDisplayName();
        boolean live = liveCreators.contains(cc.getUniqueId());

        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m    &r &6&l✦ " + name + "'s Socials"
                + (live ? " &c[LIVE]" : "") + " &8&m    "));
        if (!p.getDescription().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7" + p.getDescription()));
        if (!p.getStreamTitle().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &fStreaming: &e\"" + p.getStreamTitle() + "\""));
        if (!p.getYoutube().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &cYouTube   &8» &7youtube.com/" + p.getYoutube()));
        if (!p.getTwitch().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &5Twitch    &8» &7twitch.tv/" + p.getTwitch()));
        if (!p.getTiktok().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &fTikTok    &8» &7tiktok.com/@" + p.getTiktok()));
        if (!p.getTwitter().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &9Twitter   &8» &7twitter.com/" + p.getTwitter()));
        if (!p.getInstagram().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &dInstagram &8» &7instagram.com/" + p.getInstagram()));
        if (!p.getDiscord().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &bDiscord   &8» &7" + p.getDiscord()));
        if (!p.getRewardCode().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Subscriber code: &b/redeemcode "
                    + p.getRewardCode().toUpperCase(Locale.ENGLISH)));
        Bukkit.broadcastMessage(CC.color("&8&m                                  "));
        Bukkit.broadcastMessage(CC.color(" "));
        return null;
    }

    // ── CC chat channel ───────────────────────────────────────────────────────

    public void sendCCChat(Player sender, String message) {
        String formatted = CC.color("&6[CC Chat] &e" + sender.getName() + " &8» &f" + message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isCreator(online.getUniqueId()) || online.hasPermission("evaulx.cchat.see")) {
                online.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolvePlatformUrl(ContentCreatorProfile p, String platform) {
        if (platform != null) {
            String pl = platform.toLowerCase(Locale.ENGLISH);
            if ((pl.equals("twitch") || pl.equals("t")) && !p.getTwitch().isEmpty())
                return "twitch.tv/" + p.getTwitch();
            if ((pl.equals("youtube") || pl.equals("yt") || pl.equals("y")) && !p.getYoutube().isEmpty())
                return "youtube.com/" + p.getYoutube();
            if ((pl.equals("tiktok") || pl.equals("tt")) && !p.getTiktok().isEmpty())
                return "tiktok.com/@" + p.getTiktok();
        }
        // Prefer live-stream platforms first, then others
        if (!p.getTwitch().isEmpty()) return "twitch.tv/" + p.getTwitch();
        if (!p.getYoutube().isEmpty()) return "youtube.com/" + p.getYoutube();
        if (!p.getTiktok().isEmpty()) return "tiktok.com/@" + p.getTiktok();
        return "";
    }

    private String buildShortPlatformLine(ContentCreatorProfile p) {
        List<String> parts = new ArrayList<>();
        if (!p.getYoutube().isEmpty()) parts.add("&cYT");
        if (!p.getTwitch().isEmpty()) parts.add("&5Twitch");
        if (!p.getTiktok().isEmpty()) parts.add("&fTikTok");
        if (!p.getTwitter().isEmpty()) parts.add("&9Twitter");
        if (!p.getInstagram().isEmpty()) parts.add("&dIG");
        if (!p.getDiscord().isEmpty()) parts.add("&bDiscord");
        return String.join(" &8| ", parts);
    }

    public String shoutout(Player cc, Player target) {
        if (!isCreator(cc.getUniqueId())) return "&cYou are not a registered content creator.";
        if (target == null || !target.isOnline()) return "&cThat player is not online.";

        long cooldownSec = plugin.getConfig().getLong("content-creators.shoutout-cooldown-seconds", 120);
        Long lastUsed = shoutoutCooldowns.get(cc.getUniqueId());
        if (lastUsed != null) {
            long remaining = cooldownSec - (System.currentTimeMillis() - lastUsed) / 1000;
            if (remaining > 0) return "&cShoutout cooldown: &e" + remaining + "s &cremaining.";
        }
        shoutoutCooldowns.put(cc.getUniqueId(), System.currentTimeMillis());

        ContentCreatorProfile p = profiles.get(cc.getUniqueId());
        String ccName = p.effectiveDisplayName();
        boolean live = liveCreators.contains(cc.getUniqueId());

        Bukkit.broadcastMessage(CC.color(" "));
        Bukkit.broadcastMessage(CC.color("&8&m      &r &6&l★ SHOUTOUT &8&m      "));
        Bukkit.broadcastMessage(CC.color("  &e" + ccName + (live ? " &c[LIVE]" : "") + " &7gives a shoutout to &f&l" + target.getName() + "&7!"));
        if (!p.getStreamTitle().isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Stream: &f\"" + p.getStreamTitle() + "\""));
        String link = resolvePlatformUrl(p, null);
        if (!link.isEmpty())
            Bukkit.broadcastMessage(CC.color("  &7Watch &e" + ccName + " &7at &b" + link));
        Bukkit.broadcastMessage(CC.color("&8&m                              "));
        Bukkit.broadcastMessage(CC.color(" "));

        target.sendTitle(CC.color("&6&l★ SHOUTOUT!"), CC.color("&7from &e" + ccName));
        try {
            target.playSound(target.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.8f, 1.2f);
        } catch (Throwable ignored) {}
        return null;
    }

    private void spawnWinFireworks(final Location loc) {
        Color[][] pairs = {
            { Color.fromRGB(0xFF, 0xD7, 0x00), Color.WHITE },
            { Color.fromRGB(0xFF, 0x55, 0xFF), Color.fromRGB(0x55, 0xFF, 0xFF) },
            { Color.fromRGB(0x55, 0xFF, 0x55), Color.fromRGB(0xFF, 0xFF, 0x55) }
        };
        for (int i = 0; i < pairs.length; i++) {
            final Color primary = pairs[i][0];
            final Color fade = pairs[i][1];
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> spawnBurst(loc, primary, fade), i * 8L);
        }
    }

    private void spawnBurst(Location loc, Color primary, Color fade) {
        try {
            EntityType fireworkType;
            try { fireworkType = EntityType.valueOf("FIREWORK_ROCKET"); }
            catch (Throwable e) { fireworkType = EntityType.FIREWORK; }
            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, fireworkType);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(primary)
                    .withFade(fade)
                    .flicker(true)
                    .trail(false)
                    .build());
            meta.setPower(0);
            fw.setFireworkMeta(meta);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try { if (!fw.isDead()) fw.detonate(); } catch (Throwable ignored) {}
            }, 1L);
        } catch (Throwable ignored) {}
    }
}














