package dev.evaulx.core.commands.essential;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.managers.MailManager;
import dev.evaulx.core.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MailCommand implements CommandExecutor, TabCompleter {

    private static final SimpleDateFormat DATE = new SimpleDateFormat("MM/dd HH:mm");

    private final EvaulxCore plugin;

    public MailCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "players-only", "&cOnly players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("evaulx.mail")) {
            plugin.getMessageManager().send(sender, "no-permission", "&cNo permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("read")) return handleRead(player);

        String sub = args[0].toLowerCase();
        if (sub.equals("send")) return handleSend(player, args);
        if (sub.equals("clear")) {
            plugin.getMailManager().clearMail(player.getUniqueId());
            player.sendMessage(CC.color("&aMailbox cleared."));
            return true;
        }

        player.sendMessage(CC.color("&cUsage: /mail <read|send <player> <message>|clear>"));
        return true;
    }

    private boolean handleRead(Player player) {
        List<MailManager.MailMessage> messages = plugin.getMailManager().getMail(player.getUniqueId());
        plugin.getMailManager().markAllRead(player.getUniqueId());
        player.sendMessage(CC.color(CC.SEPARATOR));
        player.sendMessage(CC.color("  &aMailbox &8(&f" + messages.size() + " messages&8)"));
        if (messages.isEmpty()) {
            player.sendMessage(CC.color("  &7No mail. Use &f/mail send <player> <message>&7."));
        } else {
            int start = Math.max(0, messages.size() - 10);
            for (int i = messages.size() - 1; i >= start; i--) {
                MailManager.MailMessage m = messages.get(i);
                String date = DATE.format(new Date(m.timestamp()));
                String prefix = m.read() ? "&8" : "&a";
                player.sendMessage(CC.color("  " + prefix + "[" + date + "] &f" + m.from() + "&7: " + m.message()));
            }
            if (messages.size() > 10) {
                player.sendMessage(CC.color("  &8... and " + (messages.size() - 10) + " older messages."));
            }
        }
        player.sendMessage(CC.color(CC.SEPARATOR));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleSend(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(CC.color("&cUsage: /mail send <player> <message>")); return true; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore()) { player.sendMessage(CC.color("&cPlayer not found.")); return true; }

        String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (msg.length() > 256) { player.sendMessage(CC.color("&cMessage too long (max 256 chars).")); return true; }

        plugin.getMailManager().sendMail(target.getUniqueId(), player.getName(), msg);
        player.sendMessage(CC.color("&aMail sent to &f" + target.getName() + "&a."));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            online.sendMessage(CC.color("&7You have new mail from &f" + player.getName() + "&7. Use &a/mail read&7."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("evaulx.mail")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("read", "send", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
