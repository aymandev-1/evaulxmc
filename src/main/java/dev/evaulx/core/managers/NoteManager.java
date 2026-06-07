package dev.evaulx.core.managers;

import com.google.gson.*;
import dev.evaulx.core.EvaulxCore;
import dev.evaulx.core.models.PlayerNote;
import dev.evaulx.core.utils.CC;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NoteManager {

    private final EvaulxCore plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, List<PlayerNote>> notes = new ConcurrentHashMap<>();
    private File notesFile;

    public NoteManager(EvaulxCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File dataDir = plugin.getEvaulxDataFolder();
        if (!dataDir.exists()) dataDir.mkdirs();
        notesFile = new File(dataDir, "notes.json");
        loadNotes();
    }

    public void shutdown() {
        saveNotes();
    }

    public PlayerNote addNote(CommandSender issuer, OfflinePlayer target, String text) {
        PlayerNote note = new PlayerNote(target.getUniqueId(), target.getName(), issuer.getName(), text);
        notes.computeIfAbsent(target.getUniqueId(), key -> Collections.synchronizedList(new ArrayList<>())).add(0, note);
        saveNotes();
        plugin.getStaffRequestManager().logAction(issuer.getName(), "ADD_NOTE", target.getName(), text);
        return note;
    }

    public boolean removeNote(String id) {
        for (List<PlayerNote> playerNotes : notes.values()) {
            synchronized (playerNotes) {
                Iterator<PlayerNote> iterator = playerNotes.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getId().equalsIgnoreCase(id)) {
                        iterator.remove();
                        saveNotes();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clearNotes(UUID uuid) {
        notes.remove(uuid);
        saveNotes();
    }

    public List<PlayerNote> getNotes(UUID uuid) {
        List<PlayerNote> playerNotes = notes.get(uuid);
        if (playerNotes == null) return Collections.emptyList();
        synchronized (playerNotes) {
            return new ArrayList<>(playerNotes);
        }
    }

    public List<PlayerNote> searchNotes(String query, int max) {
        String lowered = query.toLowerCase(Locale.ENGLISH);
        List<PlayerNote> matches = new ArrayList<>();
        for (List<PlayerNote> playerNotes : notes.values()) {
            synchronized (playerNotes) {
                for (PlayerNote note : playerNotes) {
                    if (contains(note.getTargetName(), lowered)
                            || contains(note.getIssuerName(), lowered)
                            || contains(note.getNote(), lowered)
                            || contains(note.getId(), lowered)) {
                        matches.add(note);
                        if (matches.size() >= max) return matches;
                    }
                }
            }
        }
        matches.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return matches;
    }

    public int getNoteCount(UUID uuid) {
        return getNotes(uuid).size();
    }

    public int getStoredNoteCount() {
        int count = 0;
        for (List<PlayerNote> playerNotes : notes.values()) count += playerNotes.size();
        return count;
    }

    public void alertStaffOnJoin(Player player) {
        int count = getNoteCount(player.getUniqueId());
        if (count <= 0) return;

        String message = plugin.getConfig().getString("notes.join-alert-format",
                "&8[&eNotes&8] &f{player} &7joined with &e{count} &7staff note(s).");
        plugin.getStaffRequestManager().broadcastStaff(message
                .replace("{player}", player.getName())
                .replace("{count}", String.valueOf(count)), "evaulx.notes");
    }

    private void loadNotes() {
        if (notesFile == null || !notesFile.exists()) return;
        notes.clear();
        try (Reader reader = new InputStreamReader(new FileInputStream(notesFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                PlayerNote note = fromJson(element.getAsJsonObject());
                notes.computeIfAbsent(note.getTarget(), key -> Collections.synchronizedList(new ArrayList<>())).add(note);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load notes: " + e.getMessage());
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(query);
    }

    private void saveNotes() {
        if (notesFile == null) return;
        JsonArray array = new JsonArray();
        for (List<PlayerNote> playerNotes : notes.values()) {
            synchronized (playerNotes) {
                for (PlayerNote note : playerNotes) {
                    array.add(toJson(note));
                }
            }
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(notesFile), StandardCharsets.UTF_8)) {
            gson.toJson(array, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save notes: " + e.getMessage());
        }
    }

    private JsonObject toJson(PlayerNote note) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", note.getId());
        obj.addProperty("target", note.getTarget().toString());
        obj.addProperty("targetName", note.getTargetName());
        obj.addProperty("issuerName", note.getIssuerName());
        obj.addProperty("note", note.getNote());
        obj.addProperty("createdAt", note.getCreatedAt());
        return obj;
    }

    private PlayerNote fromJson(JsonObject obj) {
        PlayerNote note = new PlayerNote(
                UUID.fromString(obj.get("target").getAsString()),
                getString(obj, "targetName", "Unknown"),
                getString(obj, "issuerName", "CONSOLE"),
                getString(obj, "note", "")
        );
        note.setId(getString(obj, "id", note.getId()));
        note.setCreatedAt(getLong(obj, "createdAt", System.currentTimeMillis()));
        return note;
    }

    private String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return CC.strip(obj.get(key).getAsString());
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }
}
