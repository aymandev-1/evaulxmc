package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.staff.StaffRequestManager.StaffRequest;
import dev.evaulx.core.utils.CC;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HelpOpCommand implements CommandExecutor {

    private static final int MAX_MESSAGE_LENGTH = 200;

    private final EvaulxCore plugin;

    public HelpOpCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /helpop <message>"));
            return true;
        }

        Player player = (Player) sender;
        String message = clean(join(args), MAX_MESSAGE_LENGTH);

        if (message.isEmpty()) {
            sender.sendMessage(CC.color("&cUsage: /helpop <message>"));
            return true;
        }

        long cooldown = plugin.getStaffRequestManager().getHelpOpCooldownRemaining(player);
        if (cooldown > 0L && !player.hasPermission("evaulx.staff")) {
            sender.sendMessage(CC.color("&cPlease wait " + formatSeconds(cooldown) + "s before sending another helpop."));
            return true;
        }

        StaffRequest request = plugin.getStaffRequestManager().submitHelpOp(player, message);
        sender.sendMessage(CC.color("&7Your helpop was sent to staff. &8(#" + request.getId() + ")"));
        return true;
    }

    private String join(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(arg);
        }
        return builder.toString();
    }

    private String clean(String input, int maxLength) {
        String cleaned = CC.strip(input).replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private long formatSeconds(long millis) {
        return (millis + 999L) / 1000L;
    }
}
