package dev.evaulx.core.managers;

import dev.evaulx.core.EvaulxCore;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class GrantTemplateManager {

    private final EvaulxCore plugin;

    public GrantTemplateManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public List<GrantTemplate> getTemplates() {
        List<GrantTemplate> templates = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("grant-templates");
        if (section == null) return templates;

        for (String key : section.getKeys(false)) {
            ConfigurationSection templateSection = section.getConfigurationSection(key);
            if (templateSection == null) continue;
            GrantTemplate template = fromSection(key, templateSection);
            if (template != null) templates.add(template);
        }
        templates.sort((first, second) -> {
            int order = Integer.compare(first.getOrder(), second.getOrder());
            if (order != 0) return order;
            return first.getId().compareToIgnoreCase(second.getId());
        });
        return templates;
    }

    public GrantTemplate getTemplate(String id) {
        if (id == null) return null;
        for (GrantTemplate template : getTemplates()) {
            if (template.getId().equalsIgnoreCase(id)) return template;
        }
        return null;
    }

    public List<String> getTemplateIds() {
        List<String> ids = new ArrayList<>();
        for (GrantTemplate template : getTemplates()) ids.add(template.getId());
        return ids;
    }

    private GrantTemplate fromSection(String id, ConfigurationSection section) {
        String rank = section.getString("rank", "");
        if (rank.trim().isEmpty()) return null;
        return new GrantTemplate(
                id,
                section.getString("display-name", id),
                section.getString("rank", rank),
                section.getString("duration", "perm"),
                section.getString("reason", "Grant template: " + id),
                section.getString("permission", ""),
                section.getString("material", "GOLD_INGOT"),
                section.getInt("order", 100)
        );
    }

    public static class GrantTemplate {
        private final String id;
        private final String displayName;
        private final String rankName;
        private final String duration;
        private final String reason;
        private final String permission;
        private final String material;
        private final int order;

        public GrantTemplate(String id, String displayName, String rankName, String duration, String reason,
                             String permission, String material, int order) {
            this.id = id;
            this.displayName = displayName;
            this.rankName = rankName;
            this.duration = duration;
            this.reason = reason;
            this.permission = permission == null ? "" : permission;
            this.material = material == null ? "GOLD_INGOT" : material;
            this.order = order;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getRankName() { return rankName; }
        public String getDuration() { return duration; }
        public String getReason() { return reason; }
        public String getPermission() { return permission; }
        public String getMaterial() { return material; }
        public int getOrder() { return order; }
    }
}
