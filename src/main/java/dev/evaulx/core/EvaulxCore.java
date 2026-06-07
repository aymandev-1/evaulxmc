package dev.evaulx.core;

import dev.evaulx.core.chat.ChatManager;
import dev.evaulx.core.commands.essential.*;
import dev.evaulx.core.commands.punishment.*;
import dev.evaulx.core.commands.rank.*;
import dev.evaulx.core.commands.staff.*;
import dev.evaulx.core.database.DatabaseManager;
import dev.evaulx.core.discord.DiscordManager;
import dev.evaulx.core.disguise.DisguiseManager;
import dev.evaulx.core.gui.GuiManager;
import dev.evaulx.core.hooks.EvaulxMCHubHook;
import dev.evaulx.core.hooks.PlaceholderHook;
import dev.evaulx.core.hooks.ProtocolLibHook;
import dev.evaulx.core.hooks.VaultHook;
import dev.evaulx.core.listeners.*;
import dev.evaulx.core.managers.*;
import dev.evaulx.core.network.RedisSyncManager;
import dev.evaulx.core.nametags.NameTagManager;
import dev.evaulx.core.staff.StaffRequestManager;
import dev.evaulx.core.utils.CC;
import dev.evaulx.core.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EvaulxCore extends JavaPlugin {

    private static EvaulxCore instance;

    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private RankManager rankManager;
    private PunishmentManager punishmentManager;
    private DisguiseManager disguiseManager;
    private ChatManager chatManager;
    private NameTagManager nameTagManager;
    private DiscordManager discordManager;
    private TipsManager tipsManager;
    private StaffRequestManager staffRequestManager;
    private EssentialsManager essentialsManager;
    private GrantManager grantManager;
    private NoteManager noteManager;
    private PunishmentPresetManager punishmentPresetManager;
    private PlayerLookupManager playerLookupManager;
    private GrantTemplateManager grantTemplateManager;
    private GuiManager guiManager;
    private RedisSyncManager redisSyncManager;
    private EvaulxMCHubHook hubHook;
    private VaultHook vaultHook;
    private MessageManager messageManager;
    private ProtocolLibHook protocolLibHook;

    @Override
    public void onEnable() {
        instance = this;
        prepareDataFolders();
        saveDefaultConfig();
        new ConfigMigrationManager(this).apply();
        this.messageManager = new MessageManager(this);
        this.messageManager.load();

        printBanner();

        // Init database
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling EvaulxMC.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Init managers
        this.rankManager = new RankManager(this);
        this.playerManager = new PlayerManager(this);
        this.playerLookupManager = new PlayerLookupManager(this);
        this.playerLookupManager.load();
        this.punishmentManager = new PunishmentManager(this);
        this.disguiseManager = new DisguiseManager(this);
        this.chatManager = new ChatManager(this);
        this.nameTagManager = new NameTagManager(this);
        this.discordManager = new DiscordManager(this);
        this.tipsManager = new TipsManager(this);
        this.essentialsManager = new EssentialsManager(this);
        this.staffRequestManager = new StaffRequestManager(this);
        this.staffRequestManager.load();
        this.grantManager = new GrantManager(this);
        this.grantManager.load();
        this.grantTemplateManager = new GrantTemplateManager(this);
        this.noteManager = new NoteManager(this);
        this.noteManager.load();
        this.punishmentPresetManager = new PunishmentPresetManager(this);
        this.guiManager = new GuiManager(this);
        this.hubHook = new EvaulxMCHubHook(this);
        this.hubHook.load();
        this.protocolLibHook = new ProtocolLibHook(this);
        this.protocolLibHook.load();

        rankManager.loadRanks();
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            this.vaultHook = new VaultHook(this);
            this.vaultHook.load();
        }
        warnStartupIssues();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        this.redisSyncManager = new RedisSyncManager(this);
        this.redisSyncManager.start();
        this.messageManager.startActionBarTask();

        getLogger().info(CC.color("&aEvaulxMC &fv" + getDescription().getVersion() + " &aenabled successfully."));
    }

    @Override
    public void onDisable() {
        if (vaultHook != null) vaultHook.unload();
        if (redisSyncManager != null) redisSyncManager.shutdown();
        if (grantManager != null) grantManager.shutdown();
        if (noteManager != null) noteManager.shutdown();
        if (playerLookupManager != null) playerLookupManager.shutdown();
        if (staffRequestManager != null) staffRequestManager.shutdown();
        if (messageManager != null) messageManager.shutdown();
        if (playerManager != null) playerManager.saveAll();
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("EvaulxMC disabled.");
    }

    private void printBanner() {
        Bukkit.getConsoleSender().sendMessage(CC.color("&c"));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c  _____                _      __  __  _____ "));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c | ____|_   ____ _  _| |_  _|  \\/  |/ ____|"));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c |  _| \\ \\ / / _` || | | | | |\\/| | |     "));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c | |___ \\ V / (_| || |_| |_| |  | | |____ "));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c |_____| \\_/ \\__,_||_|\\___/|_|  |_|\\_____|"));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c"));
        Bukkit.getConsoleSender().sendMessage(CC.color("&7  Core Manager &cv" + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(CC.color("&c"));
    }

    private void prepareDataFolders() {
        createFolder(getDataFolder());
        createFolder(getEvaulxDataFolder());
        createFolder(new File(getEvaulxDataFolder(), "players"));
        createFolder(new File(getEvaulxDataFolder(), "punishments"));
        createFolder(new File(getEvaulxDataFolder(), "ranks"));
        createFolder(new File(getEvaulxDataFolder(), "logs"));
        createFolder(new File(getEvaulxDataFolder(), "cache"));
        createFolder(new File(getEvaulxDataFolder(), "backups"));
        createFolder(new File(getDataFolder(), "imports"));

        // Customizable server-core folders (for multiple servers/gamemodes)
        File custom = new File(getDataFolder(), "custom");
        createFolder(custom);
        createFolder(new File(custom, "modules"));
        createFolder(new File(custom, "messages"));
        createFolder(new File(custom, "templates"));
        createFolder(new File(custom, "gamemodes"));
        createFolder(new File(custom, "configs"));

        // Save default resources into the plugin folder if missing
        saveResourceIfMissing("messages.yml");
        // Some tooling/packagers may not allow creating nested resource folders easily.
        // Save top-level modules_example.yml and move it into custom/modules/example.yml if needed.
        saveResourceIfMissing("modules_example.yml");
        File savedModule = new File(getDataFolder(), "modules_example.yml");
        File destModule = new File(new File(getDataFolder(), "custom/modules"), "example.yml");
        if (savedModule.exists() && !destModule.exists()) {
            // attempt to move into the modules folder
            if (!destModule.getParentFile().exists()) destModule.getParentFile().mkdirs();
            boolean ok = savedModule.renameTo(destModule);
            if (!ok) getLogger().warning("Could not move default module to " + destModule.getPath());
        }
        saveResourceIfMissing("servercore.yml");
    }

    private void createFolder(File folder) {
        if (folder.exists()) return;
        if (!folder.mkdirs()) {
            getLogger().warning("Could not create folder: " + folder.getPath());
        }
    }

    private void saveResourceIfMissing(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try {
            saveResource(resourcePath, false);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Default resource not found in jar: " + resourcePath);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffModeListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffToolListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandSpyListener(this), this);
        getServer().getPluginManager().registerEvents(new DisguiseListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
    }

    private void registerCommands() {
        // Core
        EvaulxCommand evaulxCommand = new EvaulxCommand(this);
        getCommand("evaulxmc").setExecutor(evaulxCommand);
        getCommand("evaulxmc").setTabCompleter(evaulxCommand);

        // Punishment commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("blacklist").setExecutor(new BlacklistCommand(this));
        getCommand("unblacklist").setExecutor(new UnblacklistCommand(this));
        getCommand("checkpunishments").setExecutor(new CheckPunishmentsCommand(this));
        getCommand("punish").setExecutor(new PunishCommand(this));

        // Rank commands
        RankCommand rankCommand = new RankCommand(this);
        registerRankCommand(rankCommand, "rank");
        registerRankCommand(rankCommand, "listranks");
        registerRankCommand(rankCommand, "rankladder");
        registerRankCommand(rankCommand, "createrank");
        registerRankCommand(rankCommand, "deleterank");
        registerRankCommand(rankCommand, "rankdisplay");
        registerRankCommand(rankCommand, "rankpermission");
        registerRankCommand(rankCommand, "rankprefix");
        registerRankCommand(rankCommand, "ranksuffix");
        registerRankCommand(rankCommand, "rankcolor");
        registerRankCommand(rankCommand, "ranknamecolor");
        registerRankCommand(rankCommand, "rankweight");
        registerRankCommand(rankCommand, "rankdefault");
        registerRankCommand(rankCommand, "rankstaff");
        registerRankCommand(rankCommand, "rankperm");
        registerRankCommand(rankCommand, "rankinherit");
        registerRankCommand(rankCommand, "rankclone");
        registerRankCommand(rankCommand, "rankreload");
        registerRankCommand(rankCommand, "playerrank");
        getCommand("setrank").setExecutor(new SetRankCommand(this));
        getCommand("addrank").setExecutor(new AddRankCommand(this));
        getCommand("removerank").setExecutor(new RemoveRankCommand(this));
        getCommand("rankinfo").setExecutor(new RankInfoCommand(this));
        PermCommand permCommand = new PermCommand(this);
        getCommand("perm").setExecutor(permCommand);
        getCommand("perm").setTabCompleter(permCommand);
        GrantCommand grantCommand = new GrantCommand(this);
        getCommand("grant").setExecutor(grantCommand);
        getCommand("grant").setTabCompleter(grantCommand);
        getCommand("grants").setExecutor(new GrantsCommand(this));
        getCommand("removegrant").setExecutor(new RemoveGrantCommand(this));

        // Disguise commands
        DisguiseCommand disguiseCommand = new DisguiseCommand(this);
        getCommand("disguise").setExecutor(disguiseCommand);
        getCommand("disguise").setTabCompleter(disguiseCommand);
        SkinCommand skinCommand = new SkinCommand(this);
        getCommand("skin").setExecutor(skinCommand);
        getCommand("skin").setTabCompleter(skinCommand);
        getCommand("undisguise").setExecutor(new UndisguiseCommand(this));
        getCommand("disguiseinfo").setExecutor(new DisguiseInfoCommand(this));
        getCommand("realname").setExecutor(new RealNameCommand(this));
        getCommand("nicklist").setExecutor(new NickListCommand(this));
        getCommand("nickhistory").setExecutor(new NickHistoryCommand(this));

        // Essential commands
        getCommand("gamemode").setExecutor(new GamemodeCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("staffmode").setExecutor(new StaffModeCommand(this));
        getCommand("staffstatus").setExecutor(new StaffStatusCommand(this));
        getCommand("staffchat").setExecutor(new StaffChatCommand(this));
        getCommand("commandspy").setExecutor(new CommandSpyCommand(this));
        getCommand("staffrecover").setExecutor(new StaffRecoverCommand(this));
        getCommand("freeze").setExecutor(new FreezeCommand(this));
        getCommand("unfreeze").setExecutor(new FreezeCommand(this));
        getCommand("stafflist").setExecutor(new StaffListCommand(this));
        getCommand("staffpanel").setExecutor(new StaffPanelCommand(this));
        getCommand("staffdashboard").setExecutor(new StaffDashboardCommand(this));
        StaffSessionsCommand staffSessionsCommand = new StaffSessionsCommand(this);
        getCommand("staffsessions").setExecutor(staffSessionsCommand);
        getCommand("staffsessions").setTabCompleter(staffSessionsCommand);
        MaintenanceCommand maintenanceCommand = new MaintenanceCommand(this);
        getCommand("maintenance").setExecutor(maintenanceCommand);
        getCommand("maintenance").setTabCompleter(maintenanceCommand);
        PermissionAuditCommand permissionAuditCommand = new PermissionAuditCommand(this);
        getCommand("permaudit").setExecutor(permissionAuditCommand);
        getCommand("permaudit").setTabCompleter(permissionAuditCommand);
        LookupCommand lookupCommand = new LookupCommand(this);
        getCommand("lookup").setExecutor(lookupCommand);
        getCommand("lookup").setTabCompleter(lookupCommand);
        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("modlogs").setExecutor(new ModLogsCommand(this));
        getCommand("stafflogs").setExecutor(new StaffLogsCommand(this));
        getCommand("note").setExecutor(new NoteCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("tppos").setExecutor(new TeleportPositionCommand(this));
        getCommand("list").setExecutor(new ListCommand(this));
        getCommand("sudo").setExecutor(new SudoCommand(this));
        getCommand("announce").setExecutor(new AnnounceCommand(this));
        getCommand("enchant").setExecutor(new EnchantCommand(this));
        getCommand("teleport").setExecutor(new TeleportCommand(this));
        getCommand("teleporthere").setExecutor(new TeleportHereCommand(this));
        getCommand("teleportall").setExecutor(new TeleportAllCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("speed").setExecutor(new SpeedCommand(this));
        getCommand("invsee").setExecutor(new InvseeCommand(this));
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        getCommand("alert").setExecutor(new AlertCommand(this));
        getCommand("helpop").setExecutor(new HelpOpCommand(this));
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));
        getCommand("msg").setExecutor(new MessageCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("socialspy").setExecutor(new SocialSpyCommand(this));
        getCommand("mutechat").setExecutor(new MuteChatCommand(this));
        getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        NameTagCommand nameTagCommand = new NameTagCommand(this);
        getCommand("nametag").setExecutor(nameTagCommand);
        getCommand("nametag").setTabCompleter(nameTagCommand);
        getCommand("chatcolor").setExecutor(new ChatColorCommand(this));
        getCommand("namecolor").setExecutor(new NameColorCommand(this));
        TagCommand tagCommand = new TagCommand(this);
        getCommand("tag").setExecutor(tagCommand);
        getCommand("tag").setTabCompleter(tagCommand);
        getCommand("buildmode").setExecutor(new BuildModeCommand(this));
        LobbyProtectionCommand lobbyProtectionCommand = new LobbyProtectionCommand(this);
        getCommand("lobbyprotect").setExecutor(lobbyProtectionCommand);
        getCommand("lobbyprotect").setTabCompleter(lobbyProtectionCommand);
    }

    private void registerRankCommand(RankCommand executor, String name) {
        PluginCommand command = getCommand(name);
        if (command == null) return;
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void warnStartupIssues() {
        if (rankManager.getDefaultRank() == null) {
            getLogger().warning("No default rank is loaded. Run /rank presets confirm or set a default rank.");
        }
        if (!rankManager.getMissingPresetRanks().isEmpty() || !rankManager.getUnexpectedPresetRanks().isEmpty()) {
            getLogger().warning("Loaded ranks do not exactly match the EvaulxMC preset rank set. Run /rank presets confirm to enforce the requested rank list.");
        }
        if (getConfig().getBoolean("redis.enabled", false)) {
            String ip = getConfig().getString("redis.ip", "");
            int port = getConfig().getInt("redis.port", -1);
            if (ip == null || ip.trim().isEmpty() || port <= 0 || port > 65535) {
                getLogger().warning("Redis is enabled but redis.ip or redis.port is invalid.");
            }
        }
        if (getGrantManager().countInvalidRankReferences(rankManager.getRankNameSet()) > 0) {
            getLogger().warning("Some grants reference missing ranks. Run /rank cleanup confirm.");
        }
    }

    public static EvaulxCore getInstance() { return instance; }
    public File getEvaulxDataFolder() { return new File(getDataFolder(), "data"); }
    public File getCustomFolder() { return new File(getDataFolder(), "custom"); }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public RankManager getRankManager() { return rankManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public ChatManager getChatManager() { return chatManager; }
    public NameTagManager getNameTagManager() { return nameTagManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public TipsManager getTipsManager() { return tipsManager; }
    public EssentialsManager getEssentialsManager() { return essentialsManager; }
    public StaffRequestManager getStaffRequestManager() { return staffRequestManager; }
    public GrantManager getGrantManager() { return grantManager; }
    public PlayerLookupManager getPlayerLookupManager() { return playerLookupManager; }
    public GrantTemplateManager getGrantTemplateManager() { return grantTemplateManager; }
    public NoteManager getNoteManager() { return noteManager; }
    public PunishmentPresetManager getPunishmentPresetManager() { return punishmentPresetManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public RedisSyncManager getRedisSyncManager() { return redisSyncManager; }
    public EvaulxMCHubHook getHubHook() { return hubHook; }
    public VaultHook getVaultHook() { return vaultHook; }
    public MessageManager getMessageManager() { return messageManager; }
    public ProtocolLibHook getProtocolLibHook() { return protocolLibHook; }
}
