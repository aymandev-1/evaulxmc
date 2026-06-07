package dev.evaulx.core.commands.staff;

import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerNote;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.util.List;

public class NoteCommand implements CommandExecutor {

    private final EvaulxCore plugin;

    public NoteCommand(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evaulx.notes")) {
            sender.sendMessage(CC.color("&cNo permission."));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("search")) {
            search(sender, args);
            return true;
        }

        if (sub.equals("remove") || sub.equals("delete")) {
            remove(sender, args[1]);
            return true;
        }

        OfflinePlayer target = PlayerUtil.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(CC.color("&cPlayer not found."));
            return true;
        }

        if (sub.equals("add")) {
            add(sender, target, args);
            return true;
        }

        if (sub.equals("list") || sub.equals("view")) {
            list(sender, target);
            return true;
        }

        if (sub.equals("clear")) {
            plugin.getNoteManager().clearNotes(target.getUniqueId());
            plugin.getStaffRequestManager().logAction(sender.getName(), "CLEAR_NOTES", target.getName(), "");
            sender.sendMessage(CC.color("&7Cleared notes for &f" + target.getName() + "&7."));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void add(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /note add <player> <note>"));
            return;
        }

        String text = clean(join(args, 2), 240);
        PlayerNote note = plugin.getNoteManager().addNote(sender, target, text);
        sender.sendMessage(CC.color("&aAdded note &f" + note.getId() + " &afor &f" + target.getName() + "&a."));
    }

    private void list(CommandSender sender, OfflinePlayer target) {
        List<PlayerNote> notes = plugin.getNoteManager().getNotes(target.getUniqueId());
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cNotes for &f" + target.getName()));
        if (notes.isEmpty()) {
            sender.sendMessage(CC.color("&7No notes found."));
        } else {
            for (PlayerNote note : notes) {
                sender.sendMessage(CC.color("&8" + note.getId() + " &7[" + note.getFormattedTime()
                        + "] &f" + note.getIssuerName() + " &8- &7" + note.getNote()));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void remove(CommandSender sender, String id) {
        if (!plugin.getNoteManager().removeNote(id)) {
            sender.sendMessage(CC.color("&cNote not found."));
            return;
        }
        plugin.getStaffRequestManager().logAction(sender.getName(), "REMOVE_NOTE", id, "");
        sender.sendMessage(CC.color("&aNote removed."));
    }

    private void search(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /note search <query>"));
            return;
        }

        List<PlayerNote> notes = plugin.getNoteManager().searchNotes(join(args, 1), 10);
        sender.sendMessage(CC.color(CC.SEPARATOR));
        sender.sendMessage(CC.color("&cNote Search"));
        if (notes.isEmpty()) {
            sender.sendMessage(CC.color("&7No matching notes found."));
        } else {
            for (PlayerNote note : notes) {
                sender.sendMessage(CC.color("&8" + note.getId() + " &f" + note.getTargetName()
                        + " &7by &f" + note.getIssuerName()
                        + " &8- &7" + note.getNote()));
            }
        }
        sender.sendMessage(CC.color(CC.SEPARATOR));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(CC.color("&cUsage: /note <add|list|search|remove|clear> <player|id|query> [note]"));
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String clean(String input, int maxLength) {
        String cleaned = CC.strip(input).replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
