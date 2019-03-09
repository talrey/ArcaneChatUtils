package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;

// TODO: Make the output of each individual commands prettier (cause it's a mess as of now)
public class LinkCommands {
    private static final BaseComponent DISCORD = ArcaneText.url("https://arcaneminecraft.com/discord");
    private static final BaseComponent DONATE = ArcaneText.url("https://arcaneminecraft.com/donate");
    private static final BaseComponent FORUM = ArcaneText.url("https://arcaneminecraft.com/forum");
    private static final BaseComponent RULES = ArcaneText.url("https://arcaneminecraft.com/rules");
    private static final BaseComponent WEBSITE = ArcaneText.url("https://arcaneminecraft.com/");

    public LinkCommands() {
    }

    private BaseComponent singleLink(Locale locale, String what, BaseComponent link) {
        BaseComponent ret = ArcaneText.translatable(locale, "commands.links.single", what, link);
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }

    public class Links extends Command implements TabExecutor {

        public Links() {
            super(BungeeCommandUsage.LINKS.getName(), BungeeCommandUsage.LINKS.getPermission(), BungeeCommandUsage.LINKS.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {

            if (sender instanceof ProxiedPlayer) {
                ProxiedPlayer p = (ProxiedPlayer)sender;

                BaseComponent dash = new TextComponent("- ");
                dash.setColor(ArcaneColor.CONTENT);

                BaseComponent header = ArcaneText.translatable(p.getLocale(), "commands.links.header");
                header.setColor(ArcaneColor.LIST);

                p.sendMessage(ChatMessageType.SYSTEM, header);
                p.sendMessage(ChatMessageType.SYSTEM, dash, WEBSITE);
                p.sendMessage(ChatMessageType.SYSTEM, dash, DISCORD);
                p.sendMessage(ChatMessageType.SYSTEM, dash, DONATE);
                p.sendMessage(ChatMessageType.SYSTEM, dash, FORUM);
                p.sendMessage(ChatMessageType.SYSTEM, dash, RULES);
            } else {
                sender.sendMessage(WEBSITE);
                sender.sendMessage(DISCORD);
                sender.sendMessage(DONATE);
                sender.sendMessage(FORUM);
                sender.sendMessage(RULES);
            }
        }
        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    public abstract class LinkCommand extends Command {
        private final String what;
        private final BaseComponent url;

        private LinkCommand(BungeeCommandUsage bc, String what, BaseComponent url) {
            super(bc.getName(), bc.getPermission(), bc.getAliases());
            this.what = what;
            this.url = url;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(
                        ChatMessageType.SYSTEM,
                        singleLink(((ProxiedPlayer) sender).getLocale(), what, url)
                );
            else
                sender.sendMessage(singleLink(null, what, url));
        }
    }

    public class Donate extends LinkCommand {
        public Donate() {
            super(BungeeCommandUsage.DONATE, "donate", DONATE);
        }
    }

    public class Forum extends LinkCommand {
        public Forum() {
            super(BungeeCommandUsage.FORUM, "forum", FORUM);
        }
    }

    public class Rules extends LinkCommand {
        public Rules() {
            super(BungeeCommandUsage.RULES, "rules", RULES);
        }
    }

    public class Website extends LinkCommand {
        public Website() {
            super(BungeeCommandUsage.WEBSITE, "website", WEBSITE);
        }
    }
}
