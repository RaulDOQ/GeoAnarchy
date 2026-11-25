package me.rauldoq.geoAnarchy.bot;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.alerts.AlertService;
import me.rauldoq.geoAnarchy.config.Settings;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The Discord bot: logs in, registers the slash commands, hands each command, button and window to the
 * class that deals with it, and delivers private messages.
 * <p>
 * Discord calls everything here on its own threads. Code that needs the game goes through
 * {@link GeoAnarchy#onMain}.
 */
public final class Bot extends ListenerAdapter implements AlertService.Sender {

    public static final int COLOR = 0x49DB8A;

    private final GeoAnarchy plugin;
    private final List<ICommand> commands = new ArrayList<>();
    private final Map<String, Consumer<ButtonInteractionEvent>> buttons = new LinkedHashMap<>();
    private final Map<String, Consumer<ModalInteractionEvent>> modals = new LinkedHashMap<>();
    private final List<Object> listeners = new ArrayList<>();
    private volatile JDA jda;

    public Bot(GeoAnarchy plugin) {
        this.plugin = plugin;
    }

    public void add(ICommand command) {
        commands.add(command);
    }

    /** Buttons whose id starts with this prefix go to this handler. */
    public void onButton(String prefix, Consumer<ButtonInteractionEvent> handler) {
        buttons.put(prefix, handler);
    }

    /** Windows (modals) whose id starts with this prefix go to this handler. */
    public void onModal(String prefix, Consumer<ModalInteractionEvent> handler) {
        modals.put(prefix, handler);
    }

    /** For the parts that listen to Discord by themselves, like the update notifications. */
    public void addListener(Object listener) {
        listeners.add(listener);
    }

    /** Logs in. Returns at once; the bot is usable a few seconds later, when Discord says it is ready. */
    public void start(Settings settings) {
        if (!settings.hasToken()) {
            plugin.getLogger().warning("discord.token is empty in config.yml, so the Discord bot is off. "
                    + "Linking, alerts and Discord commands need it.");
            return;
        }
        try {
            List<Object> all = new ArrayList<>(listeners);
            all.add(this);
            // MESSAGE_CONTENT lets the bot see staff posts in the updates channel. It must be switched on
            // for the bot in the Discord Developer Portal (Bot > Privileged Gateway Intents).
            jda = JDABuilder.createLight(settings.token(), GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing(settings.activity()))
                    .addEventListeners(all.toArray())
                    .build();
        } catch (RuntimeException error) {
            jda = null;
            plugin.getLogger().log(Level.SEVERE, "The Discord bot could not start. Check discord.token in config.yml, "
                    + "and that Message Content Intent is on for the bot in the Discord Developer Portal.", error);
        }
    }

    public void stop() {
        JDA current = jda;
        jda = null;
        if (current != null) {
            current.shutdown();
        }
    }

    public boolean online() {
        JDA current = jda;
        return current != null && current.getStatus() == JDA.Status.CONNECTED;
    }

    public String status() {
        JDA current = jda;
        return current == null ? "desligado" : current.getStatus().name().toLowerCase();
    }

    @Override
    public void onReady(ReadyEvent event) {
        List<CommandData> definitions = new ArrayList<>();
        for (ICommand command : commands) {
            var definition = Commands.slash(command.getName(), command.getDescription())
                    .setDefaultPermissions(command.getDefaultPermissions());
            if (command.getOptions() != null) {
                definition.addOptions(command.getOptions());
            }
            definitions.add(definition);
        }
        for (Guild guild : event.getJDA().getGuilds()) {
            // Replaces the whole list, so commands from older versions of the plugin disappear.
            guild.updateCommands().addCommands(definitions).queue(null, error ->
                    plugin.getLogger().log(Level.WARNING, "Could not register the slash commands in " + guild.getName(), error));
        }
        Settings settings = plugin.settings();
        plugin.getLogger().info("Discord bot ready as " + event.getJDA().getSelfUser().getName()
                + " in " + event.getJDA().getGuilds().size() + " server(s).");
        if (settings.statusChannel() != 0) {
            announce(settings.statusChannel(), plugin.messages().text("status.online",
                    "version", Bukkit.getMinecraftVersion(), "players", String.valueOf(Bukkit.getOnlinePlayers().size())));
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        for (ICommand command : commands) {
            if (command.getName().equals(event.getName())) {
                try {
                    command.execute(event);
                } catch (RuntimeException error) {
                    failed(error);
                    if (!event.isAcknowledged()) {
                        event.reply(plugin.messages().text("error")).setEphemeral(true).queue();
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        for (Map.Entry<String, Consumer<ButtonInteractionEvent>> entry : buttons.entrySet()) {
            if (id.startsWith(entry.getKey())) {
                try {
                    entry.getValue().accept(event);
                } catch (RuntimeException error) {
                    failed(error);
                    if (!event.isAcknowledged()) {
                        event.reply(plugin.messages().text("error")).setEphemeral(true).queue();
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        for (Map.Entry<String, Consumer<ModalInteractionEvent>> entry : modals.entrySet()) {
            if (id.startsWith(entry.getKey())) {
                try {
                    entry.getValue().accept(event);
                } catch (RuntimeException error) {
                    failed(error);
                    if (!event.isAcknowledged()) {
                        event.reply(plugin.messages().text("error")).setEphemeral(true).queue();
                    }
                }
                return;
            }
        }
    }

    void failed(Throwable error) {
        plugin.getLogger().log(Level.WARNING, "A Discord command did not finish.", error);
    }

    // ---- Private messages ----

    /** Sends an alert as a private message. If the player has them closed, that is remembered and they are told in game. */
    @Override
    public void send(Link to, AlertService.Alert alert) {
        JDA current = jda;
        if (current == null) {
            return;
        }
        var embed = new EmbedBuilder().setColor(COLOR).setTitle(alert.title()).setDescription(alert.text())
                .setFooter(plugin.messages().text("alert.footer")).build();
        current.retrieveUserById(to.discord())
                .flatMap(User::openPrivateChannel)
                .flatMap(channel -> channel.sendMessageEmbeds(embed))
                .queue(sent -> {
                    if (plugin.prefs().dmClosed(to.minecraft())) {
                        plugin.prefs().setDmClosed(to.minecraft(), false);
                    }
                }, error -> {
                    if (error instanceof ErrorResponseException discord
                            && discord.getErrorResponse() == ErrorResponse.CANNOT_SEND_TO_USER) {
                        plugin.prefs().setDmClosed(to.minecraft(), true);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Could not send a private message to " + to.name(), error);
                    }
                });
    }

    /** Posts a text in a channel of the server. Does nothing if the channel is gone or the bot is off. */
    public void announce(long channelId, String text) {
        JDA current = jda;
        if (current == null || channelId == 0) {
            return;
        }
        TextChannel channel = current.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Discord channel " + channelId + " from config.yml was not found.");
            return;
        }
        channel.sendMessage(text).setAllowedMentions(List.of()).queue(null, error ->
                plugin.getLogger().log(Level.WARNING, "Could not post in channel " + channelId, error));
    }

    // ---- What linking changes on the Discord server: a role and the nickname ----

    private Guild guild() {
        JDA current = jda;
        if (current == null) {
            return null;
        }
        long wanted = plugin.settings().guildId();
        Guild guild = wanted == 0 ? null : current.getGuildById(wanted);
        if (guild == null && !current.getGuilds().isEmpty()) {
            guild = current.getGuilds().get(0);
        }
        return guild;
    }

    /** Gives the "linked" role and sets the nickname. Both are extras: failing at them never undoes a link. */
    public void markLinked(Link link) {
        Guild guild = guild();
        if (guild == null) {
            return;
        }
        Settings settings = plugin.settings();
        String roleName = settings.linkRole();
        if (!roleName.isEmpty() && guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            List<Role> found = guild.getRolesByName(roleName, true);
            if (!found.isEmpty()) {
                giveRole(guild, found.get(0), link);
            } else {
                guild.createRole().setName(roleName).setPermissions(0L).queue(role -> giveRole(guild, role, link), this::quiet);
            }
        }
        if (settings.syncNickname() && guild.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)) {
            guild.retrieveMemberById(link.discord()).queue(member -> {
                if (guild.getSelfMember().canInteract(member) && !link.name().equals(member.getNickname())) {
                    member.modifyNickname(link.name()).queue(null, this::quiet);
                }
            }, this::quiet);
        }
    }

    private void giveRole(Guild guild, Role role, Link link) {
        if (guild.getSelfMember().canInteract(role)) {
            guild.addRoleToMember(UserSnowflake.fromId(link.discord()), role).queue(null, this::quiet);
        }
    }

    /** Takes the "linked" role away again. The nickname is left as it is. */
    public void markUnlinked(String discordId) {
        Guild guild = guild();
        String roleName = plugin.settings().linkRole();
        if (guild == null || roleName.isEmpty()) {
            return;
        }
        for (Role role : guild.getRolesByName(roleName, true)) {
            if (guild.getSelfMember().canInteract(role)) {
                guild.removeRoleFromMember(UserSnowflake.fromId(discordId), role).queue(null, this::quiet);
            }
        }
    }

    private void quiet(Throwable error) {
        plugin.getLogger().log(Level.FINE, "A Discord extra (role or nickname) did not apply.", error);
    }
}
