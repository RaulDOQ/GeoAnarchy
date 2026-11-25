package me.rauldoq.geoAnarchy.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public final class PainelCommand implements ICommand {
    private static final String SITE = "https://geoanarchy.net";
    private static final String MAPA = "https://mapa.geoanarchy.net/";
    private static final String LOGO = "https://raw.githubusercontent.com/RaulDOQ/GeoAnarchy/"
            + "6c6b3065ad28dc8cde7bbb79ef961bdf1713c45e/website/src/assets/logo-small.webp";

    @Override
    public String getName() {
        return "painel";
    }

    @Override
    public String getDescription() {
        return "Publica a central de informações do GeoAnarchy neste canal.";
    }

    @Override
    public DefaultMemberPermissions getDefaultPermissions() {
        return DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                channelOption("regras", "Canal com as regras do servidor"),
                channelOption("avisos", "Canal de avisos oficiais"),
                channelOption("guias", "Canal que explica como jogar"),
                new OptionData(OptionType.STRING, "loja", "Link HTTPS da loja oficial (opcional)")
                        .setMaxLength(512));
    }

    private static OptionData channelOption(String name, String description) {
        return new OptionData(OptionType.CHANNEL, name, description, true)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var member = event.getMember();
        if (guild == null || member == null || !member.hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("Você precisa da permissão Gerenciar Servidor.").setEphemeral(true).queue();
            return;
        }
        if (event.getChannelType() != ChannelType.TEXT && event.getChannelType() != ChannelType.NEWS) {
            event.reply("Use este comando em um canal de texto ou de anúncios.").setEphemeral(true).queue();
            return;
        }
        var channel = event.getGuildChannel();
        if (!guild.getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("Preciso ver este canal, enviar mensagens e inserir links.")
                    .setEphemeral(true).queue();
            return;
        }
        String shop = event.getOption("loja", OptionMapping::getAsString);
        if (shop != null && !isShopUrl(shop)) {
            event.reply("Informe um link da loja que comece com https:// e tenha um domínio válido.")
                    .setEphemeral(true).queue();
            return;
        }
        var self = event.getJDA().getSelfUser();
        String logo = self.getAvatarUrl() == null ? LOGO : self.getAvatarUrl();
        MessageCreateData panel = buildPanel(logo,
                event.getOption("regras").getAsChannel().getJumpUrl(),
                event.getOption("avisos").getAsChannel().getJumpUrl(),
                event.getOption("guias").getAsChannel().getJumpUrl(), shop);

        event.deferReply(true).queue(hook ->
                channel.sendMessage(panel).queue(
                        message -> hook.editOriginal("Painel publicado: " + message.getJumpUrl()).queue(),
                        error -> hook.editOriginal("Falha ao publicar o painel. Confira as permissões do bot neste canal.").queue()));
    }

    static boolean isShopUrl(String value) {
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (URISyntaxException badUrl) {
            return false;
        }
    }

    static MessageCreateData buildPanel(String logo, String regras, String avisos, String guias, String shop) {
        var embed = new EmbedBuilder()
                .setColor(0x49DB8A)
                .setTitle("📍・Central de informações")
                .setThumbnail(logo)
                .setDescription("Bem-vindo ao **GeoAnarchy**! Explore a Terra em escala 1:1000, "
                        + "encontre uma nação ou escolha onde fundar a sua.")
                .addField("🧭・Primeiros passos",
                        "📜 **Regras** — confira o que vale no jogo e no Discord.\n"
                        + "🌍 **Mapa-múndi** — veja o território das nações em tempo real.\n"
                        + "📢 **Avisos** — acompanhe eventos e manutenções.\n"
                        + "📖 **Guias** — descubra como começar a jogar.", false)
                .addField("🎮・Minecraft Java", "`geoanarchy.net`\nServidor 1.21.11", true)
                .addField("📱・Minecraft Bedrock", "`geoanarchy.net`\nPorta `10109`", true)
                .setFooter("GeoAnarchy • Precisa de ajuda? Fale com a moderação.")
                .build();

        Button official = shop == null ? link(SITE, "Site oficial", "🌐") : link(shop, "Loja oficial", "🛒");
        return new MessageCreateBuilder().setEmbeds(embed).setComponents(
                ActionRow.of(official, link(regras, "Regras", "📜"), link(MAPA, "Mapa-múndi", "🌍")),
                ActionRow.of(link(avisos, "Avisos", "📢"), link(guias, "Guias", "📖"),
                        // Opens the small window where a player types the code from /vincular in game.
                        Button.success(LinkCommands.OPEN_BUTTON, "Vincular conta").withEmoji(Emoji.fromUnicode("🔗")))).build();
    }

    private static Button link(String url, String label, String emoji) {
        return Button.link(url, label).withEmoji(Emoji.fromUnicode(emoji));
    }
}
