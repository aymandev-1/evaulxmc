package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Reports per-world entity counts for lag diagnostics.
 * Usage: /entitycount
 */
public class EntityCountCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public EntityCountCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.admin.entities")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        sender.sendMessage(CC.color("  &c&lEntity Report"));
        int grandTotal = 0;
        for (World world : Bukkit.getWorlds()) {
            int total = 0, living = 0, items = 0;
            for (Entity entity : world.getEntities()) {
                total++;
                if (entity instanceof Item) items++;
                else if (entity instanceof LivingEntity && !(entity instanceof Player)) living++;
            }
            grandTotal += total;
            sender.sendMessage(CC.color("  &7" + world.getName() + ": &f" + total
                    + " &7total &8(&aliving:" + living + " &eitems:" + items
                    + " &bchunks:" + world.getLoadedChunks().length + "&8)"));
        }
        sender.sendMessage(CC.color("  &7Grand total: &e" + grandTotal + " &7entities"));
        sender.sendMessage(CC.color("&8&m----------------------------------------"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
