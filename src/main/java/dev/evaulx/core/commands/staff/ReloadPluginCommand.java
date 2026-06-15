package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;

import java.util.Collections;
import java.util.List;

public class ReloadPluginCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ReloadPluginCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.reload")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        long start = System.currentTimeMillis();

        sender.sendMessage(CC.color("&8[&9Dev&8] &7Reloading EvaulxMC..."));

        try {
            plugin.reloadConfig();
            sender.sendMessage(CC.color("  &a✔ &7Config reloaded."));
        } catch (Exception e) {
            sender.sendMessage(CC.color("  &c✘ &7Config reload failed: &f" + e.getMessage()));
        }

        try {
            plugin.getMessageManager().reload();
            sender.sendMessage(CC.color("  &a✔ &7Messages reloaded."));
        } catch (Exception e) {
            sender.sendMessage(CC.color("  &c✘ &7Message reload failed: &f" + e.getMessage()));
        }

        try {
            if (plugin.getRankManager() != null) {
                plugin.getRankManager().loadRanks();
                sender.sendMessage(CC.color("  &a✔ &7Ranks reloaded."));
            }
        } catch (Exception e) {
            sender.sendMessage(CC.color("  &c✘ &7Rank reload failed: &f" + e.getMessage()));
        }

        try {
            if (plugin.getProtocolLibHook() != null) {
                plugin.getProtocolLibHook().load();
                sender.sendMessage(CC.color("  &a✔ &7ProtocolLib hook reloaded."));
            }
        } catch (Exception e) {
            sender.sendMessage(CC.color("  &c✘ &7ProtocolLib reload failed: &f" + e.getMessage()));
        }

        long elapsed = System.currentTimeMillis() - start;
        sender.sendMessage(CC.color("&8[&9Dev&8] &aReload complete &8(&f" + elapsed + "ms&8)&a."));

        String senderName = sender instanceof org.bukkit.entity.Player ? sender.getName() : "Console";
        plugin.getStaffRequestManager().logAction(senderName, "RELOAD_PLUGIN", "all", "Full plugin reload in " + elapsed + "ms");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
