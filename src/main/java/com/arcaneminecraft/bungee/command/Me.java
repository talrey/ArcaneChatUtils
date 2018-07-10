package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Me extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public Me(ArcaneBungee plugin) {
        super(BungeeCommandUsage.ME.getName(), BungeeCommandUsage.ME.getPermission(), BungeeCommandUsage.ME.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.ME.getCommand(), args);

        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.ME.getUsage()));
            else
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.ME.getUsage()));
            return;
        }

        BaseComponent ret = new TranslatableComponent("chat.type.emote", ArcaneText.playerComponentBungee(sender), String.join(" ", args));

        plugin.getProxy().getConsole().sendMessage(ret);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, ret);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return plugin.getTabCompletePreset().onlinePlayers(args);
    }
}
