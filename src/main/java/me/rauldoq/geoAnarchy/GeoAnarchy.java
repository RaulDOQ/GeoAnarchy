package me.rauldoq.geoAnarchy;

import me.rauldoq.geoAnarchy.alerts.AlertService;
import me.rauldoq.geoAnarchy.alerts.Prefs;
import me.rauldoq.geoAnarchy.bot.ActionCommands;
import me.rauldoq.geoAnarchy.bot.Bot;
import me.rauldoq.geoAnarchy.bot.InfoCommands;
import me.rauldoq.geoAnarchy.bot.LinkCommands;
import me.rauldoq.geoAnarchy.bot.NotifyCommand;
import me.rauldoq.geoAnarchy.bot.PainelCommand;
import me.rauldoq.geoAnarchy.bot.PublishUpdateCommand;
import me.rauldoq.geoAnarchy.bot.UpdateNotifications;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.config.Settings;
import me.rauldoq.geoAnarchy.country.Countries;
import me.rauldoq.geoAnarchy.country.LandsHook;
import me.rauldoq.geoAnarchy.economy.Economy;
import me.rauldoq.geoAnarchy.economy.GeoEconomyBridge;
import me.rauldoq.geoAnarchy.chat.ChatTags;
import me.rauldoq.geoAnarchy.chat.GeoPlaceholders;
import me.rauldoq.geoAnarchy.game.Coffee;
import me.rauldoq.geoAnarchy.game.GameCommands;
import me.rauldoq.geoAnarchy.game.JoinListener;
import me.rauldoq.geoAnarchy.game.Ranks;
import me.rauldoq.geoAnarchy.game.RunOnce;
import me.rauldoq.geoAnarchy.game.VipService;
import me.rauldoq.geoAnarchy.link.LinkService;
import me.rauldoq.geoAnarchy.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * GeoAnarchy: the server's Discord bot and small utilities.
 * <ul>
 *   <li><b>link</b>: a Minecraft account and a Discord account become one player.</li>
 *   <li><b>alerts</b>: private messages in Discord about your country (war, upkeep, invites).</li>
 *   <li><b>bot</b>: the Discord commands, buttons and panels.</li>
 *   <li><b>game</b>: the Minecraft commands (/vincular, /discord, /mapa, /ranks, VIP).</li>
 *   <li><b>country</b> and <b>economy</b>: the only places that talk to Lands and GeoEconomy.</li>
 * </ul>
 * Lands, GeoEconomy and even Discord are optional: whatever is missing switches its part off and the rest runs.
 */
public final class GeoAnarchy extends JavaPlugin {

    private volatile Settings settings;
    private volatile Messages messages;
    private Database database;
    private LinkService links;
    private Prefs prefs;
    private AlertService alerts;
    private Bot bot;
    private Ranks ranks;
    private VipService vips;
    private Coffee coffee;
    private volatile Countries countries = Countries.NONE;
    private volatile Economy economy = Economy.NONE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTexts();

        try {
            database = new Database(getDataFolder().toPath().resolve("geoanarchy.db"));
        } catch (SQLException error) {
            getLogger().log(Level.SEVERE, "Could not open geoanarchy.db. The plugin is switching itself off.", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        links = new LinkService(database, System::currentTimeMillis,
                settings.codeMinutes(), settings.maxAttempts(), settings.lockMinutes());
        prefs = new Prefs(database);
        alerts = new AlertService(links, prefs, System::currentTimeMillis);
        ranks = new Ranks();
        vips = new VipService(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));

        bot = new Bot(this);
        alerts.setSender(bot);
        new LinkCommands(this, bot).register();
        new InfoCommands(this, bot).register();
        new ActionCommands(this, bot).register();
        new NotifyCommand(this, bot).register();
        bot.add(new PainelCommand());
        UpdateNotifications updates = new UpdateNotifications(getDataFolder().toPath(), getLogger());
        bot.add(updates);
        bot.add(new PublishUpdateCommand(updates));
        bot.addListener(updates);

        connectToOtherPlugins();

        GameCommands commands = new GameCommands(this);
        for (String name : getDescription().getCommands().keySet()) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(commands);
                command.setTabCompleter(commands);
            }
        }
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        coffee = new Coffee(this);
        getServer().getPluginManager().registerEvents(coffee, this);

        // Chat placeholders (%geoanarchy_country% and the rest), read by VentureChat.
        ChatTags chatTags = new ChatTags(this, () -> countries);
        chatTags.start();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                if (GeoPlaceholders.register(this, chatTags)) {
                    getLogger().info("Chat placeholders ready: %geoanarchy_country%, %geoanarchy_name%, %geoanarchy_prefix%.");
                }
            } catch (Throwable error) {
                getLogger().log(Level.WARNING, "Could not register the chat placeholders.", error);
            }
        }

        // run-once.txt: console commands prepared as a file. Scheduled tasks start once the server has finished
        // starting, so LuckPerms and the rest are ready by then.
        Bukkit.getScheduler().runTaskLater(this, () -> RunOnce.run(getDataFolder().toPath(),
                command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), getLogger()), 40L);

        // Once an hour: use up VIP time. Every five minutes: forget link codes that ran out.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, links::tidy, 20L * 300, 20L * 300);

        bot.start(settings);
        getLogger().info("Loaded " + links.size() + " linked account(s). Lands: "
                + (countries.available() ? "connected" : "not found") + ". GeoEconomy: "
                + (economy != Economy.NONE ? "connected" : "not found") + ".");
    }

    /** Each connection is tried on its own, so one missing or changed plugin never stops the others. */
    private void connectToOtherPlugins() {
        Plugin lands = getServer().getPluginManager().getPlugin("Lands");
        if (lands != null) {
            try {
                LandsHook hook = new LandsHook(this, alerts, this::settings, this::messages, this::economy,
                        text -> bot.announce(settings.warChannel(), text));
                hook.listen();
                countries = hook;
            } catch (Throwable error) {
                getLogger().log(Level.WARNING, "Could not connect to Lands. Country alerts and /pais are off.", error);
            }
        }
        Plugin geoEconomy = getServer().getPluginManager().getPlugin("GeoEconomy");
        if (geoEconomy != null) {
            try {
                economy = GeoEconomyBridge.connect(geoEconomy);
            } catch (Throwable error) {
                getLogger().log(Level.WARNING, "Could not connect to GeoEconomy. /saldo, /preco, /depositar and /pagar are off.", error);
            }
        }
    }

    @Override
    public void onDisable() {
        if (bot != null) {
            bot.stop();
        }
        if (database != null) {
            database.close();
        }
    }

    /** Reads config.yml and messages.yml again. A new token or new channels need a restart. */
    public void reload() {
        reloadConfig();
        loadTexts();
    }

    private void loadTexts() {
        settings = Settings.load(getConfig());
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        YamlConfiguration defaults = null;
        try (InputStream stream = getResource("messages.yml")) {
            if (stream != null) {
                defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException ignored) {
            // the file on disk is enough
        }
        messages = new Messages(YamlConfiguration.loadConfiguration(file), defaults);
    }

    /**
     * Runs something on the server thread and hands back its result. Discord's commands arrive on Discord's
     * own threads, and the game may only be touched from the server thread.
     */
    public <T> CompletableFuture<T> onMain(Supplier<T> work) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (!isEnabled()) {
            result.completeExceptionally(new IllegalStateException("The plugin is switched off."));
            return result;
        }
        if (Bukkit.isPrimaryThread()) {
            try {
                result.complete(work.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
            return result;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                result.complete(work.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    /** Notes down something a player did through Discord, to show them at their next login. */
    public void rememberAction(UUID player, String text) {
        database.log(player, text, System.currentTimeMillis());
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Database database() {
        return database;
    }

    public LinkService links() {
        return links;
    }

    public Prefs prefs() {
        return prefs;
    }

    public AlertService alerts() {
        return alerts;
    }

    public Bot bot() {
        return bot;
    }

    public Ranks ranks() {
        return ranks;
    }

    public VipService vips() {
        return vips;
    }

    public Coffee coffee() {
        return coffee;
    }

    public Countries countries() {
        return countries;
    }

    public Economy economy() {
        return economy;
    }
}
