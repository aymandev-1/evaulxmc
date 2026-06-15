package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Chunk;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Shows diagnostics for the chunk the player is standing in.
 * Usage: /chunkinfo
 */
public class ChunkInfoCommand implements CommandExecutor, TabCompleter {

    private final EvaulxCore plugin;

    public ChunkInfoCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("evaulx.admin.chunkinfo")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        player.sendMessage(CC.color("&8&m----------------------------------------"));
        player.sendMessage(CC.color("  &c&lChunk Info"));
        player.sendMessage(CC.color("  &7World: &f" + chunk.getWorld().getName()));
        player.sendMessage(CC.color("  &7Chunk: &f" + chunk.getX() + ", " + chunk.getZ()));
        player.sendMessage(CC.color("  &7Entities here: &e" + chunk.getEntities().length));
        player.sendMessage(CC.color("  &7Tile entities: &e" + chunk.getTileEntities().length));
        player.sendMessage(CC.color("  &7Loaded chunks (world): &b" + chunk.getWorld().getLoadedChunks().length));
        player.sendMessage(CC.color("  &7World entities: &b" + chunk.getWorld().getEntities().size()));
        player.sendMessage(CC.color("&8&m----------------------------------------"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
