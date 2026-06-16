package dev.evaulx.core;

import dev.evaulx.core.chat.ChatManager;
import dev.evaulx.core.commands.essential.*;
import dev.evaulx.core.commands.punishment.*;
import dev.evaulx.core.commands.rank.*;
import dev.evaulx.core.commands.staff.*;
import dev.evaulx.core.creator.ContentCreatorManager;
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
import dev.evaulx.core.managers.FriendManager;
import dev.evaulx.core.managers.PartyManager;
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
    private AfkManager afkManager;
    private AppealManager appealManager;
    private RedisSyncManager redisSyncManager;
    private EvaulxMCHubHook hubHook;
    private VaultHook vaultHook;
    private MessageManager messageManager;
    private ProtocolLibHook protocolLibHook;
    private ContentCreatorManager contentCreatorManager;
    private FriendManager friendManager;
    private PartyManager partyManager;
    private CoinsManager coinsManager;
    private DailyRewardManager dailyRewardManager;
    private HomeManager homeManager;
    private MailManager mailManager;
    private WarpManager warpManager;

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
        this.afkManager = new AfkManager(this);
        this.appealManager = new AppealManager(this);
        this.appealManager.load();
        this.guiManager = new GuiManager(this);
        this.contentCreatorManager = new ContentCreatorManager(this);
        this.friendManager = new FriendManager(this);
        this.partyManager = new PartyManager(this);
        this.coinsManager = new CoinsManager(this);
        this.coinsManager.load();
        this.dailyRewardManager = new DailyRewardManager(this);
        this.dailyRewardManager.load();
        this.homeManager = new HomeManager(this);
        this.mailManager = new MailManager(this);
        this.mailManager.load();
        this.warpManager = new WarpManager(this);
        this.warpManager.load();
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
        this.afkManager.start();

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
        if (afkManager != null) afkManager.shutdown();
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
        getServer().getPluginManager().registerEvents(new AfkListener(this), this);
        getServer().getPluginManager().registerEvents(new ContentCreatorListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatSlowModeListener(this), this);
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
        getCommand("alts").setExecutor(new AltsCommand(this));
        getCommand("evidence").setExecutor(new EvidenceCommand(this));

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
        NickHistoryCommand nickHistoryCommand = new NickHistoryCommand(this);
        getCommand("nickhistory").setExecutor(nickHistoryCommand);
        getCommand("nickhistory").setTabCompleter(nickHistoryCommand);
        ForcedisguiseCommand forcedisguiseCommand = new ForcedisguiseCommand(this);
        getCommand("forcedisguise").setExecutor(forcedisguiseCommand);
        getCommand("forcedisguise").setTabCompleter(forcedisguiseCommand);
        DisguiseCooldownCommand disguiseCooldownCommand = new DisguiseCooldownCommand(this);
        getCommand("disguisecooldown").setExecutor(disguiseCooldownCommand);
        getCommand("disguisecooldown").setTabCompleter(disguiseCooldownCommand);
        NickColorCommand nickColorCommand = new NickColorCommand(this);
        getCommand("nickcolor").setExecutor(nickColorCommand);
        getCommand("nickcolor").setTabCompleter(nickColorCommand);

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

        // AFK
        getCommand("afk").setExecutor(new AfkCommand(this));

        // Essential utility commands
        NickCommand nickCommand = new NickCommand(this);
        getCommand("nick").setExecutor(nickCommand);
        getCommand("nick").setTabCompleter(nickCommand);
        SeenCommand seenCommand = new SeenCommand(this);
        getCommand("seen").setExecutor(seenCommand);
        getCommand("seen").setTabCompleter(seenCommand);
        RepairCommand repairCommand = new RepairCommand(this);
        getCommand("repair").setExecutor(repairCommand);
        getCommand("repair").setTabCompleter(repairCommand);
        getCommand("more").setExecutor(new MoreCommand(this));
        ClearInventoryCommand clearCommand = new ClearInventoryCommand(this);
        getCommand("clearinv").setExecutor(clearCommand);
        getCommand("clearinv").setTabCompleter(clearCommand);
        AppealCommand appealCommand = new AppealCommand(this);
        getCommand("appeal").setExecutor(appealCommand);
        getCommand("appeal").setTabCompleter(appealCommand);
        getCommand("msgtoggle").setExecutor(new MsgToggleCommand(this));
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        getCommand("ignore").setExecutor(ignoreCommand);
        getCommand("ignore").setTabCompleter(ignoreCommand);
        getCommand("hat").setExecutor(new HatCommand(this));
        getCommand("kickall").setExecutor(new KickAllCommand(this));
        TimeCommand timeCommand = new TimeCommand(this);
        getCommand("time").setExecutor(timeCommand);
        getCommand("time").setTabCompleter(timeCommand);
        WeatherCommand weatherCommand = new WeatherCommand(this);
        getCommand("weather").setExecutor(weatherCommand);
        getCommand("weather").setTabCompleter(weatherCommand);

        // Content creator
        ContentCreatorCommand creatorCommand = new ContentCreatorCommand(this);
        getCommand("creator").setExecutor(creatorCommand);
        getCommand("creator").setTabCompleter(creatorCommand);
        getCommand("redeemcode").setExecutor(new RedeemCodeCommand(this));
        ShoutoutCommand shoutoutCommand = new ShoutoutCommand(this);
        getCommand("shoutout").setExecutor(shoutoutCommand);
        getCommand("shoutout").setTabCompleter(shoutoutCommand);
        getCommand("cchat").setExecutor(new CCChatCommand(this));
        GoLiveCommand goLiveCommand = new GoLiveCommand(this, false);
        getCommand("golive").setExecutor(goLiveCommand);
        getCommand("golive").setTabCompleter(goLiveCommand);
        getCommand("offair").setExecutor(new GoLiveCommand(this, true));
        getCommand("ccgiveaway").setExecutor(new CCGiveawayCommand(this));
        getCommand("milestone").setExecutor(new MilestoneCommand(this));
        getCommand("socials").setExecutor(new SocialsCommand(this));

        // Staff / admin / owner utilities
        getCommand("lockdown").setExecutor(new LockdownCommand(this));
        TempFlyCommand tempFlyCommand = new TempFlyCommand(this);
        getCommand("tempfly").setExecutor(tempFlyCommand);
        getCommand("tempfly").setTabCompleter(tempFlyCommand);
        getCommand("craft").setExecutor(new CraftCommand(this));
        EnderChestCommand ecCommand = new EnderChestCommand(this);
        getCommand("ec").setExecutor(ecCommand);
        getCommand("ec").setTabCompleter(ecCommand);
        GlobalFlyCommand globalFlyCommand = new GlobalFlyCommand(this);
        getCommand("globalfly").setExecutor(globalFlyCommand);
        getCommand("globalfly").setTabCompleter(globalFlyCommand);
        getCommand("ownerbc").setExecutor(new OwnerBroadcastCommand(this));
        ServerMsgCommand serverMsgCommand = new ServerMsgCommand(this);
        getCommand("servermsg").setExecutor(serverMsgCommand);
        getCommand("servermsg").setTabCompleter(serverMsgCommand);
        ResetRankCommand resetRankCommand = new ResetRankCommand(this);
        getCommand("resetrank").setExecutor(resetRankCommand);
        getCommand("resetrank").setTabCompleter(resetRankCommand);

        // Streamer mode
        StreamerModeCommand streamerModeCommand = new StreamerModeCommand(this);
        getCommand("streamermode").setExecutor(streamerModeCommand);
        getCommand("streamermode").setTabCompleter(streamerModeCommand);

        // Staff utilities
        PingCommand pingCommand = new PingCommand(this);
        getCommand("ping").setExecutor(pingCommand);
        getCommand("ping").setTabCompleter(pingCommand);
        IpCheckCommand ipCheckCommand = new IpCheckCommand(this);
        getCommand("ipcheck").setExecutor(ipCheckCommand);
        getCommand("ipcheck").setTabCompleter(ipCheckCommand);
        PlaytimeCommand playtimeCommand = new PlaytimeCommand(this);
        getCommand("playtime").setExecutor(playtimeCommand);
        getCommand("playtime").setTabCompleter(playtimeCommand);
        WhoIsCommand whoIsCommand = new WhoIsCommand(this);
        getCommand("whois").setExecutor(whoIsCommand);
        getCommand("whois").setTabCompleter(whoIsCommand);

        // Owner / admin power tools
        OwnerAlertCommand ownerAlertCommand = new OwnerAlertCommand(this);
        getCommand("owneralert").setExecutor(ownerAlertCommand);
        getCommand("owneralert").setTabCompleter(ownerAlertCommand);
        ServerFreezeCommand serverFreezeCommand = new ServerFreezeCommand(this);
        getCommand("serverfreeze").setExecutor(serverFreezeCommand);
        getCommand("serverfreeze").setTabCompleter(serverFreezeCommand);
        ForceChatCommand forceChatCommand = new ForceChatCommand(this);
        getCommand("forcechat").setExecutor(forceChatCommand);
        getCommand("forcechat").setTabCompleter(forceChatCommand);

        // Dev tools
        DevModeCommand devModeCommand = new DevModeCommand(this);
        getCommand("devmode").setExecutor(devModeCommand);
        getCommand("devmode").setTabCompleter(devModeCommand);
        getCommand("reloadplugin").setExecutor(new ReloadPluginCommand(this));
        TestEffectCommand testEffectCommand = new TestEffectCommand(this);
        getCommand("testeffect").setExecutor(testEffectCommand);
        getCommand("testeffect").setTabCompleter(testEffectCommand);
        getCommand("devbroadcast").setExecutor(new DevBroadcastCommand(this));

        // Builder tools
        BuilderAnnounceCommand builderAnnounceCommand = new BuilderAnnounceCommand(this);
        getCommand("builderannounce").setExecutor(builderAnnounceCommand);
        getCommand("builderannounce").setTabCompleter(builderAnnounceCommand);
        HeadCommand headCommand = new HeadCommand(this);
        getCommand("head").setExecutor(headCommand);
        getCommand("head").setTabCompleter(headCommand);

        // Store rank / player perks
        NightVisionCommand nightVisionCommand = new NightVisionCommand(this);
        getCommand("nightvision").setExecutor(nightVisionCommand);
        getCommand("nightvision").setTabCompleter(nightVisionCommand);
        HideAllCommand hideAllCommand = new HideAllCommand(this);
        getCommand("hideall").setExecutor(hideAllCommand);
        getCommand("hideall").setTabCompleter(hideAllCommand);
        ParticlesCommand particlesCommand = new ParticlesCommand(this);
        getCommand("particles").setExecutor(particlesCommand);
        getCommand("particles").setTabCompleter(particlesCommand);

        // Homes
        HomeCommand homeCommand = new HomeCommand(this);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("sethome").setTabCompleter(homeCommand);
        getCommand("delhome").setExecutor(homeCommand);
        getCommand("delhome").setTabCompleter(homeCommand);
        getCommand("homes").setExecutor(homeCommand);
        getCommand("homes").setTabCompleter(homeCommand);

        // Warps
        WarpCommand warpCommand = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warp").setTabCompleter(warpCommand);
        getCommand("setwarp").setExecutor(warpCommand);
        getCommand("setwarp").setTabCompleter(warpCommand);
        getCommand("delwarp").setExecutor(warpCommand);
        getCommand("delwarp").setTabCompleter(warpCommand);
        getCommand("warps").setExecutor(warpCommand);
        getCommand("warps").setTabCompleter(warpCommand);

        // TPA
        TpaCommand tpaCommand = new TpaCommand(this, false);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);
        TpaCommand tpaHereCommand = new TpaCommand(this, true);
        getCommand("tpahere").setExecutor(tpaHereCommand);
        getCommand("tpahere").setTabCompleter(tpaHereCommand);
        TpAcceptCommand tpAcceptCommand = new TpAcceptCommand(this);
        getCommand("tpaccept").setExecutor(tpAcceptCommand);
        getCommand("tpaccept").setTabCompleter(tpAcceptCommand);
        TpDenyCommand tpDenyCommand = new TpDenyCommand(this);
        getCommand("tpdeny").setExecutor(tpDenyCommand);
        getCommand("tpdeny").setTabCompleter(tpDenyCommand);

        // Item tools
        RenameCommand renameCommand = new RenameCommand(this);
        getCommand("rename").setExecutor(renameCommand);
        getCommand("rename").setTabCompleter(renameCommand);
        LoreCommand loreCommand = new LoreCommand(this);
        getCommand("lore").setExecutor(loreCommand);
        getCommand("lore").setTabCompleter(loreCommand);
        GlowCommand glowCommand = new GlowCommand(this);
        getCommand("glow").setExecutor(glowCommand);
        getCommand("glow").setTabCompleter(glowCommand);

        // Server stats
        getCommand("tps").setExecutor(new TpsCommand(this));
        getCommand("serverinfo").setExecutor(new ServerInfoCommand(this));

        // Economy
        CoinsCommand coinsCommand = new CoinsCommand(this);
        getCommand("coins").setExecutor(coinsCommand);
        getCommand("coins").setTabCompleter(coinsCommand);
        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);
        getCommand("daily").setExecutor(new DailyCommand(this));

        // Mail
        MailCommand mailCommand = new MailCommand(this);
        getCommand("mail").setExecutor(mailCommand);
        getCommand("mail").setTabCompleter(mailCommand);

        // Utility
        ExtinguishCommand extCommand = new ExtinguishCommand(this);
        getCommand("ext").setExecutor(extCommand);
        getCommand("ext").setTabCompleter(extCommand);
        NearCommand nearCommand = new NearCommand(this);
        getCommand("near").setExecutor(nearCommand);
        getCommand("near").setTabCompleter(nearCommand);
        SmiteCommand smiteCommand = new SmiteCommand(this);
        getCommand("smite").setExecutor(smiteCommand);
        getCommand("smite").setTabCompleter(smiteCommand);
        getCommand("coords").setExecutor(new CoordsCommand(this));

        // Social — Friends
        FriendCommand friendCommand = new FriendCommand(this);
        getCommand("friend").setExecutor(friendCommand);
        getCommand("friend").setTabCompleter(friendCommand);
        FriendMessageCommand fmCommand = new FriendMessageCommand(this);
        getCommand("fm").setExecutor(fmCommand);
        getCommand("fm").setTabCompleter(fmCommand);

        // Party
        PartyCommand partyCommand = new PartyCommand(this);
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(partyCommand);

        // Content creator perks
        CreatorAnnounceCommand creatorAnnounceCommand = new CreatorAnnounceCommand(this);
        getCommand("ccannounce").setExecutor(creatorAnnounceCommand);
        getCommand("ccannounce").setTabCompleter(creatorAnnounceCommand);
        WatchPartyCommand watchPartyCommand = new WatchPartyCommand(this);
        getCommand("watchparty").setExecutor(watchPartyCommand);
        getCommand("watchparty").setTabCompleter(watchPartyCommand);

        // ── New rank perks & power tools ──────────────────────────────────────

        // Staff / Mod tools
        ClearLagCommand clearLagCommand = new ClearLagCommand(this);
        getCommand("clearlag").setExecutor(clearLagCommand);
        getCommand("clearlag").setTabCompleter(clearLagCommand);
        SlowChatCommand slowChatCommand = new SlowChatCommand(this);
        getCommand("slowchat").setExecutor(slowChatCommand);
        getCommand("slowchat").setTabCompleter(slowChatCommand);

        // Admin diagnostics
        EntityCountCommand entityCountCommand = new EntityCountCommand(this);
        getCommand("entitycount").setExecutor(entityCountCommand);
        getCommand("entitycount").setTabCompleter(entityCountCommand);
        KillEntitiesCommand killEntitiesCommand = new KillEntitiesCommand(this);
        getCommand("killentities").setExecutor(killEntitiesCommand);
        getCommand("killentities").setTabCompleter(killEntitiesCommand);
        ChunkInfoCommand chunkInfoCommand = new ChunkInfoCommand(this);
        getCommand("chunkinfo").setExecutor(chunkInfoCommand);
        getCommand("chunkinfo").setTabCompleter(chunkInfoCommand);

        // Builder tools
        TopCommand topCommand = new TopCommand(this);
        getCommand("top").setExecutor(topCommand);
        getCommand("top").setTabCompleter(topCommand);
        UpCommand upCommand = new UpCommand(this);
        getCommand("up").setExecutor(upCommand);
        getCommand("up").setTabCompleter(upCommand);

        // Developer tools
        GcCommand gcCommand = new GcCommand(this);
        getCommand("gc").setExecutor(gcCommand);
        getCommand("gc").setTabCompleter(gcCommand);
        ThreadsCommand threadsCommand = new ThreadsCommand(this);
        getCommand("threads").setExecutor(threadsCommand);
        getCommand("threads").setTabCompleter(threadsCommand);
        PluginInfoCommand pluginInfoCommand = new PluginInfoCommand(this);
        getCommand("plugininfo").setExecutor(pluginInfoCommand);
        getCommand("plugininfo").setTabCompleter(pluginInfoCommand);

        // Owner tools
        ShutdownCommand shutdownCommand = new ShutdownCommand(this);
        getCommand("shutdown").setExecutor(shutdownCommand);
        getCommand("shutdown").setTabCompleter(shutdownCommand);

        // Content creator perks
        SpotlightCommand spotlightCommand = new SpotlightCommand(this);
        getCommand("spotlight").setExecutor(spotlightCommand);
        getCommand("spotlight").setTabCompleter(spotlightCommand);
        RecordingCommand recordingCommand = new RecordingCommand(this);
        getCommand("recording").setExecutor(recordingCommand);
        getCommand("recording").setTabCompleter(recordingCommand);

        // Store-rank (donor) perks
        FireworkCommand fireworkCommand = new FireworkCommand(this);
        getCommand("firework").setExecutor(fireworkCommand);
        getCommand("firework").setTabCompleter(fireworkCommand);
        LaunchCommand launchCommand = new LaunchCommand(this);
        getCommand("launch").setExecutor(launchCommand);
        getCommand("launch").setTabCompleter(launchCommand);

        // Admin & Owner control panels
        getCommand("adminpanel").setExecutor(new AdminPanelCommand(this));
        getCommand("ownerpanel").setExecutor(new OwnerPanelCommand(this));

        // Role control panels
        getCommand("creatorpanel").setExecutor(new CreatorPanelCommand(this));
        getCommand("builderpanel").setExecutor(new BuilderPanelCommand(this));
        getCommand("developerpanel").setExecutor(new DeveloperPanelCommand(this));
        getCommand("modpanel").setExecutor(new ModPanelCommand(this));
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
    public AfkManager getAfkManager() { return afkManager; }
    public AppealManager getAppealManager() { return appealManager; }
    public RedisSyncManager getRedisSyncManager() { return redisSyncManager; }
    public EvaulxMCHubHook getHubHook() { return hubHook; }
    public VaultHook getVaultHook() { return vaultHook; }
    public MessageManager getMessageManager() { return messageManager; }
    public ProtocolLibHook getProtocolLibHook() { return protocolLibHook; }
    public ContentCreatorManager getContentCreatorManager() { return contentCreatorManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public CoinsManager getCoinsManager() { return coinsManager; }
    public DailyRewardManager getDailyRewardManager() { return dailyRewardManager; }
    public HomeManager getHomeManager() { return homeManager; }
    public MailManager getMailManager() { return mailManager; }
    public WarpManager getWarpManager() { return warpManager; }
}
