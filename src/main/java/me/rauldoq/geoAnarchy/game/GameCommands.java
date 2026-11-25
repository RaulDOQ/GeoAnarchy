package me.rauldoq.geoAnarchy.game;

import me.rauldoq.geoAnarchy.GeoAnarchy;
import me.rauldoq.geoAnarchy.config.Messages;
import me.rauldoq.geoAnarchy.config.VipPlan;
import me.rauldoq.geoAnarchy.link.LinkService;
import me.rauldoq.geoAnarchy.storage.Database.Link;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Every Minecraft command of the plugin. Each one is short, so they share this file. */
public final class GameCommands implements TabExecutor {

    private final GeoAnarchy plugin;

    public GameCommands(GeoAnarchy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "vincular" -> vincular(sender);
            case "desvincular" -> desvincular(sender);
            case "discord" -> discord(sender, args);
            case "mapa" -> link(sender, "map", plugin.settings().map());
            case "ranks" -> ranks(sender);
            case "darvip" -> darVip(sender, args);
            case "vanishcomaura" -> vanishComAura(sender);
            case "geoanarchy" -> admin(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.messages().send(sender, "players-only");
        return null;
    }

    /** Sends a line with a link that opens when clicked. */
    private void link(CommandSender sender, String key, String url) {
        sender.sendMessage(plugin.messages().game(key, "url", url).clickEvent(ClickEvent.openUrl(url)));
    }

    private void vincular(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Messages texts = plugin.messages();
        Optional<String> code = plugin.links().issueCode(player.getUniqueId(), player.getName());
        if (code.isEmpty()) {
            texts.send(player, "link-already");
            return;
        }
        String pretty = LinkService.pretty(code.get());
        player.sendMessage(texts.game("link-code", "code", pretty, "minutes", String.valueOf(plugin.settings().codeMinutes()))
                .clickEvent(ClickEvent.copyToClipboard(code.get())));
        if (!plugin.bot().online()) {
            texts.send(player, "link-bot-off");
        }
    }

    private void desvincular(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Optional<Link> removed = plugin.links().unlinkMinecraft(player.getUniqueId());
        if (removed.isEmpty()) {
            plugin.messages().send(player, "unlink-none");
            return;
        }
        plugin.bot().markUnlinked(removed.get().discord());
        plugin.messages().send(player, "unlink-done");
    }

    private void discord(CommandSender sender, String[] args) {
        String action = args.length == 0 ? "servidor" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "servidor", "convite" -> link(sender, "discord-invite", plugin.settings().invite());
            case "vincular" -> vincular(sender);
            case "desvincular" -> desvincular(sender);
            case "acoes", "ações" -> {
                Player player = player(sender);
                if (player == null) {
                    return;
                }
                if (args.length < 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
                    plugin.messages().send(player, plugin.prefs().actions(player.getUniqueId()) ? "actions-are-on" : "actions-are-off");
                    return;
                }
                boolean on = args[1].equalsIgnoreCase("on");
                plugin.prefs().setActions(player.getUniqueId(), on);
                plugin.messages().send(player, on ? "actions-on" : "actions-off");
            }
            default -> plugin.messages().send(sender, "discord-usage");
        }
    }

    private void ranks(CommandSender sender) {
        Messages texts = plugin.messages();
        Ranks.Board board = plugin.ranks().get(plugin.settings().ranksRefreshMinutes());
        texts.send(sender, "ranks-header-time");
        list(sender, board.hoursPlayed(), "h");
        texts.send(sender, "ranks-header-kills");
        list(sender, board.kills(), "");
    }

    private void list(CommandSender sender, List<Ranks.Entry> entries, String unit) {
        if (entries.isEmpty()) {
            plugin.messages().send(sender, "ranks-empty");
            return;
        }
        int rank = 1;
        for (Ranks.Entry entry : entries) {
            plugin.messages().send(sender, "ranks-line", "rank", String.valueOf(rank++), "player", entry.name(),
                    "value", entry.value() + unit);
        }
    }

    /**
     * /darvip <jogador> <plano> <meses>: adds months of a VIP plan (Geo+, Geo Ultra, Geo Ultra+) to a player who
     * has joined before. The plan is a LuckPerms group given for that many days on top of what is left.
     */
    private void darVip(CommandSender sender, String[] args) {
        Messages texts = plugin.messages();
        if (!sender.hasPermission("geoanarchy.darvip")) {
            texts.send(sender, "no-permission");
            return;
        }
        String plans = String.join(", ", plugin.settings().vipPlans().stream().map(VipPlan::id).toList());
        if (args.length != 3) {
            texts.send(sender, "vip-usage", "plans", plans);
            return;
        }
        Optional<VipPlan> plan = plugin.settings().vipPlan(args[1]);
        if (plan.isEmpty()) {
            texts.send(sender, "vip-unknown-plan", "plan", args[1], "plans", plans);
            return;
        }
        int months;
        try {
            months = Integer.parseInt(args[2]);
        } catch (NumberFormatException notANumber) {
            months = 0;
        }
        if (months < 1 || months > 120) {
            texts.send(sender, "vip-bad-months");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getName() == null) {
            texts.send(sender, "vip-unknown-player", "player", args[0]);
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            texts.send(sender, "vip-no-luckperms");
            return;
        }
        int days = plugin.vips().give(target.getUniqueId(), plan.get(), months, plugin.settings().vipDaysPerMonth());
        texts.send(sender, "vip-given", "player", target.getName(), "plan", plan.get().name(),
                "months", String.valueOf(months), "days", String.valueOf(days));
        Player online = target.getPlayer();
        if (online != null) {
            texts.send(online, "vip-received", "plan", plan.get().name(), "days", String.valueOf(days));
        }
    }

    private void vanishComAura(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("geoanarchy.vanishcomaura")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        // A lightning strike that hurts nobody, then the server's own vanish command.
        player.getWorld().strikeLightningEffect(player.getLocation());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "vanish " + player.getName());
    }

    private void admin(CommandSender sender, String[] args) {
        Messages texts = plugin.messages();
        if (!sender.hasPermission("geoanarchy.admin")) {
            texts.send(sender, "no-permission");
            return;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "reload" -> {
                plugin.reload();
                plugin.messages().send(sender, "reloaded");
            }
            case "status" -> texts.send(sender, "status",
                    "bot", plugin.bot().status(),
                    "links", String.valueOf(plugin.links().size()),
                    "lands", plugin.countries().available() ? "conectado" : "não instalado",
                    "economy", plugin.economy().available() ? "conectado" : "não instalado");
            default -> texts.send(sender, "admin-usage");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("discord") && args.length == 1) {
            options.addAll(List.of("servidor", "vincular", "desvincular", "acoes"));
        } else if (name.equals("discord") && args.length == 2 && args[0].equalsIgnoreCase("acoes")) {
            options.addAll(List.of("on", "off"));
        } else if (name.equals("geoanarchy") && args.length == 1 && sender.hasPermission("geoanarchy.admin")) {
            options.addAll(List.of("reload", "status"));
        } else if (name.equals("darvip") && sender.hasPermission("geoanarchy.darvip")) {
            if (args.length == 1) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    options.add(online.getName());
                }
            } else if (args.length == 2) {
                plugin.settings().vipPlans().forEach(plan -> options.add(plan.id()));
            } else if (args.length == 3) {
                options.addAll(List.of("1", "3", "6", "12"));
            }
        }
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(typed));
        return options;
    }
}
