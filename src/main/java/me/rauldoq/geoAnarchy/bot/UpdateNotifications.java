package me.rauldoq.geoAnarchy.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpdateNotifications extends ListenerAdapter implements ICommand {
    private static final String JOIN = "geo:updates:subscribe";
    private static final String LEAVE = "geo:updates:unsubscribe";
    private static final String ROLE = "Updates";
    private static final String ROLES_CHANNEL = "🎭・cargos";
    private static final String UPDATES_CHANNEL = "📢・atualizações";
    private final UpdateSettings settings;
    private final Logger logger;
    private final Set<Long> configuring = ConcurrentHashMap.newKeySet();

    public UpdateNotifications(Path dataDirectory, Logger logger) {
        this.settings = new UpdateSettings(dataDirectory.resolve("discord-updates.properties"), logger);
        this.logger = logger;
    }

    @Override public String getName() { return "configurar-updates"; }
    @Override public String getDescription() { return "Prepara os canais de cargos e atualizações do servidor."; }
    @Override public List<OptionData> getOptions() { return List.of(); }
    @Override public DefaultMemberPermissions getDefaultPermissions() {
        return DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    static boolean staff(SlashCommandInteractionEvent event) {
        return event.isFromGuild() && event.getMember() != null && event.getMember().hasPermission(Permission.MANAGE_SERVER);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!staff(event)) {
            event.reply("Somente quem tem Gerenciar Servidor pode configurar os canais.").setEphemeral(true).queue();
            return;
        }
        Guild guild = event.getGuild();
        event.deferReply(true).queue(hook -> {
            if (!configuring.add(guild.getIdLong())) {
                hook.editOriginal("A configuração já está em andamento.").queue();
                return;
            }
            respond(hook, () -> CompletableFuture.completedFuture(guild).thenCompose(this::configure)
                    .whenComplete((message, error) -> configuring.remove(guild.getIdLong())));
        });
    }

    private CompletableFuture<String> configure(Guild guild) {
        settings.requireAvailable();
        var old = settings.get(guild.getIdLong());
        Role saved = old == null ? null : guild.getRoleById(old.role());
        Role role = saved != null ? saved : unique(guild.getRolesByName(ROLE, true), ROLE);
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            throw new IllegalStateException("Conceda Gerenciar Cargos ao bot e tente novamente.");
        }
        if (role != null) requireNotificationRole(guild, role);
        return (role != null ? CompletableFuture.completedFuture(role)
                : guild.createRole().setName(ROLE).setPermissions(0L).setMentionable(false).submit())
                .thenCompose(target -> ensureChannel(guild, old == null ? 0 : old.rolesChannel(), ROLES_CHANNEL)
                .thenCompose(roles -> ensureChannel(guild, old == null ? 0 : old.updatesChannel(), UPDATES_CHANNEL)
                .thenCompose(updates -> {
                    var panel = panel(updates);
                    long prior = old != null && old.rolesChannel() == roles.getIdLong() ? old.panel() : 0;
                    return writePanel(roles, prior, panel).thenApply(message -> {
                        try {
                            settings.save(guild.getIdLong(), new UpdateSettings.Binding(target.getIdLong(),
                                    roles.getIdLong(), updates.getIdLong(), message.getIdLong()));
                        } catch (RuntimeException error) {
                            if (message.getIdLong() != prior) message.delete().queue();
                            throw error;
                        }
                        String result = "Painel disponível em " + roles.getAsMention()
                                + ". Publique as novidades em " + updates.getAsMention() + ".";
                        if (!canPing(guild, updates, target)) {
                            result += " Para enviar os avisos, conceda ao bot a permissão de mencionar todos os cargos nesse canal.";
                        }
                        return result;
                    });
                })));
    }

    private static <T> T unique(List<T> items, String name) {
        if (items.size() > 1) throw new IllegalStateException("Há mais de um item com o nome " + name + ". Diferencie os nomes e tente novamente.");
        return items.isEmpty() ? null : items.get(0);
    }

    private static CompletableFuture<TextChannel> ensureChannel(Guild guild, long savedId, String name) {
        TextChannel channel = savedId == 0 ? null : guild.getTextChannelById(savedId);
        if (channel == null) channel = unique(guild.getTextChannelsByName(name, true), name);
        if (channel != null) {
            if (!guild.getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                throw new IllegalStateException("Permita que o bot veja, envie mensagens e insira links em " + channel.getAsMention() + ".");
            }
            return CompletableFuture.completedFuture(channel);
        }
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            throw new IllegalStateException("Crie o canal " + name + " ou conceda Gerenciar Canais ao bot e tente novamente.");
        }
        return guild.createTextChannel(name)
                .addPermissionOverride(guild.getPublicRole(), List.of(Permission.VIEW_CHANNEL), List.of(Permission.MESSAGE_SEND))
                .addPermissionOverride(guild.getSelfMember(), List.of(Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY), List.of())
                .submit();
    }

    private static MessageCreateData panel(TextChannel updates) {
        var embed = new EmbedBuilder().setColor(0x49DB8A).setTitle("🎭・Escolha seus cargos")
                .setDescription("🔔 **Updates**\nReceba uma menção em " + updates.getAsMention()
                        + " quando a equipe publicar uma novidade do GeoAnarchy.")
                .setFooter("Você pode mudar de ideia a qualquer momento.").build();
        return new MessageCreateBuilder().setEmbeds(embed).setAllowedMentions(List.of())
                .setComponents(ActionRow.of(Button.success(JOIN, "Receber atualizações"),
                        Button.secondary(LEAVE, "Parar de receber"))).build();
    }

    private static CompletableFuture<Message> writePanel(TextChannel channel, long prior, MessageCreateData panel) {
        if (prior == 0) return channel.sendMessage(panel).submit();
        return channel.editMessageById(prior, MessageEditData.fromCreateData(panel)).submit()
                .exceptionallyCompose(error -> {
                    if (cause(error) instanceof ErrorResponseException discord
                            && discord.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                        return channel.sendMessage(panel).submit();
                    }
                    return CompletableFuture.failedFuture(error);
                });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        boolean join = JOIN.equals(event.getComponentId());
        if (!join && !LEAVE.equals(event.getComponentId())) return;
        Guild guild = event.getGuild();
        if (guild == null) return;
        event.deferReply(true).queue(hook -> respond(hook, () -> setSubscription(guild, event.getUser(),
                event.getChannelIdLong(), event.getMessageIdLong(), join)));
    }

    CompletableFuture<String> setSubscription(Guild guild, UserSnowflake user, long channel, long message, boolean join) {
        var binding = settings.get(guild.getIdLong());
        if (binding == null || binding.panel() != message || binding.rolesChannel() != channel) {
            throw new IllegalStateException("Use o painel de cargos atual do servidor.");
        }
        Role role = guild.getRoleById(binding.role());
        requireNotificationRole(guild, role);
        var change = join ? guild.addRoleToMember(user, role) : guild.removeRoleFromMember(user, role);
        return change.submit().thenApply(ignored -> join ? "🔔 Você recebe os avisos de atualização."
                : "🔕 Você saiu dos avisos de atualização.");
    }

    private static void requireNotificationRole(Guild guild, Role role) {
        if (role == null || role.isPublicRole() || role.isManaged() || role.getPermissionsRaw() != 0
                || !guild.getSelfMember().canInteract(role)) {
            throw new IllegalStateException("Use um cargo Updates sem permissões, abaixo do cargo do bot.");
        }
        for (var channel : guild.getChannels()) {
            if (channel instanceof IPermissionContainer container && container.getPermissionOverride(role) != null) {
                throw new IllegalStateException("O cargo Updates deve servir apenas para avisos. Remova as permissões de canal específicas desse cargo.");
            }
        }
    }

    private static boolean canPing(Guild guild, TextChannel channel, Role role) {
        return role.isMentionable() || guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_MENTION_EVERYONE);
    }

    CompletableFuture<String> publish(Guild guild, String text) {
        if (text.isBlank()) throw new IllegalStateException("Escreva o texto da atualização.");
        var binding = settings.get(guild.getIdLong());
        if (binding == null) throw new IllegalStateException("Use /configurar-updates primeiro.");
        Role role = guild.getRoleById(binding.role());
        TextChannel channel = guild.getTextChannelById(binding.updatesChannel());
        if (role == null || channel == null) throw new IllegalStateException("O canal ou cargo foi removido. Use /configurar-updates novamente.");
        if (!canPing(guild, channel, role)) throw new IllegalStateException("Permita que o bot mencione todos os cargos no canal de atualizações.");
        return channel.sendMessage(announcement(role, text)).submit()
                .thenApply(message -> "Atualização publicada: " + message.getJumpUrl());
    }

    static MessageCreateData announcement(Role role, String text) {
        var embed = new EmbedBuilder().setTitle("📢・Atualização do GeoAnarchy")
                .setDescription(text).setColor(0x49DB8A).build();
        return new MessageCreateBuilder().setContent(role.getAsMention()).setEmbeds(embed)
                .setAllowedMentions(List.of()).mentionRoles(role.getId()).build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()
                || event.getMessage().getType() != MessageType.DEFAULT) return;
        Guild guild = event.getGuild();
        var binding = settings.get(guild.getIdLong());
        if (binding == null || binding.updatesChannel() != event.getChannel().getIdLong()) return;
        var member = event.getMember();
        var channel = event.getChannel().asTextChannel();
        if (member == null || !(member.hasPermission(Permission.MANAGE_SERVER)
                || member.hasPermission(channel, Permission.MESSAGE_MANAGE))) return;
        Role role = guild.getRoleById(binding.role());
        if (role == null) return;
        if (event.getMessage().getMentions().getRoles().contains(role)
                && (role.isMentionable() || member.hasPermission(channel, Permission.MESSAGE_MENTION_EVERYONE))) return;
        if (!canPing(guild, channel, role)) {
            logger.warning("Sem permissão para mencionar Updates no canal " + channel.getId());
            return;
        }
        event.getMessage().reply(role.getAsMention()).setAllowedMentions(List.of())
                .mentionRoles(role.getId()).mentionRepliedUser(false).queue(null,
                        error -> logger.log(Level.WARNING, "Falha ao avisar os inscritos.", error));
    }

    void respond(InteractionHook hook, Supplier<CompletableFuture<String>> work) {
        CompletableFuture<String> result;
        try {
            result = work.get();
        } catch (RuntimeException error) {
            result = CompletableFuture.failedFuture(error);
        }
        result.whenComplete((message, error) -> {
            if (error == null) {
                hook.editOriginal(message).queue();
            } else {
                Throwable reason = cause(error);
                logger.log(Level.WARNING, "Ação de notificações do Discord não concluída.", reason);
                hook.editOriginal(reason instanceof IllegalStateException ? reason.getMessage()
                        : "Não foi possível concluir. Confira as permissões do bot e tente novamente.").queue();
            }
        });
    }

    private static Throwable cause(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}
