package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public final class LockdownCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public LockdownCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.lockdown")) {
            sender.sendMessage(CC.color("&cYou don't have permission to do that."));
            return true;
        }

        boolean currentlyLocked = plugin.getEssentialsManager().isLockdown();

        if (!currentlyLocked) {
            String reason = args.length > 0
                    ? String.join(" ", Arrays.copyOfRange(args, 0, args.length))
                    : "Server locked down by staff.";
            plugin.getEssentialsManager().setLockdown(true, reason);
            Bukkit.broadcastMessage(CC.color("&8[&c⚠ Lockdown&8] &cThe server is now in &lLOCKDOWN&r&c."));
            Bukkit.broadcastMessage(CC.color("&8[&c⚠ Lockdown&8] &7Reason: &f" + reason));
            sender.sendMessage(CC.color("&a✔ Server locked. Players cannot join until lockdown is lifted."));
        } else {
            plugin.getEssentialsManager().setLockdown(false, null);
            Bukkit.broadcastMessage(CC.color("&8[&aLockdown&8] &aThe server lockdown has been &l&aLIFTED&r&a."));
            sender.sendMessage(CC.color("&a✔ Lockdown lifted. Players can now join normally."));
        }
        return true;
    }
}
