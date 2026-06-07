package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.Punishment;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class PunishmentPresetManager {

    private final EvaulxCore plugin;

    public PunishmentPresetManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public List<Preset> getPresets() {
        List<Preset> presets = new ArrayList<>();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("punishment-presets");
        if (root == null) return presets;

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            presets.add(new Preset(
                    key,
                    section.getString("display-name", "&c" + key),
                    section.getString("material", "PAPER"),
                    section.getStringList("lore"),
                    getActions(section)
            ));
        }
        return presets;
    }

    public Preset getPreset(String key) {
        for (Preset preset : getPresets()) {
            if (preset.getKey().equalsIgnoreCase(key)) return preset;
        }
        return null;
    }

    public boolean executePreset(CommandSender sender, OfflinePlayer target, String key, Integer forcedStep) {
        Preset preset = getPreset(key);
        if (preset == null || preset.getActions().isEmpty()) {
            sender.sendMessage(CC.color("&cPunishment preset not found."));
            return false;
        }

        int step = forcedStep != null ? forcedStep : getNextStep(target, preset);
        step = Math.max(1, Math.min(step, preset.getActions().size()));
        String action = preset.getActions().get(step - 1)
                .replace("{player}", target.getName())
                .replace("{preset}", preset.getKey())
                .replace("{step}", String.valueOf(step));

        Bukkit.dispatchCommand(sender, action);
        plugin.getStaffRequestManager().logAction(sender.getName(), "PUNISH_PRESET", target.getName(),
                preset.getKey() + " step " + step);
        return true;
    }

    private int getNextStep(OfflinePlayer target, Preset preset) {
        int previous = 0;
        for (Punishment punishment : plugin.getPunishmentManager().getHistory(target.getUniqueId())) {
            String reason = punishment.getReason() == null ? "" : punishment.getReason().toLowerCase();
            if (reason.contains(preset.getKey().toLowerCase())) previous++;
        }
        return previous + 1;
    }

    private List<String> getActions(ConfigurationSection section) {
        List<String> actions = new ArrayList<>();
        if (section.isList("actions")) actions.addAll(section.getStringList("actions"));
        addIfPresent(actions, section, "first");
        addIfPresent(actions, section, "second");
        addIfPresent(actions, section, "third");
        addIfPresent(actions, section, "fourth");
        if (actions.isEmpty()) {
            String command = section.getString("command");
            if (command != null && !command.isEmpty()) actions.add(command);
        }
        return actions;
    }

    private void addIfPresent(List<String> actions, ConfigurationSection section, String key) {
        String value = section.getString(key);
        if (value != null && !value.isEmpty()) actions.add(value);
    }

    public static class Preset {
        private final String key;
        private final String displayName;
        private final String material;
        private final List<String> lore;
        private final List<String> actions;

        public Preset(String key, String displayName, String material, List<String> lore, List<String> actions) {
            this.key = key;
            this.displayName = displayName;
            this.material = material;
            this.lore = lore == null ? Collections.emptyList() : lore;
            this.actions = actions == null ? Collections.emptyList() : actions;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getMaterial() { return material; }
        public List<String> getLore() { return lore; }
        public List<String> getActions() { return actions; }
    }
}
