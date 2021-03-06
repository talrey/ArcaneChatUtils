package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChatPrefixModule {
    /** Players with modified badge */
    // TODO: Find out practicability of this
    private final Set<UUID> alteredPrefix;

    private static final int CUSTOM_PREFIX_PRIORITY = 1000000;
    private static final int TEMP_PREFIX_PRIORITY = 1500000;
    private static final String ALTERED_PREFIX_PATH = "badge-altered";
    private static final String PREFIX_PRIORITY_STRING = "PrefixPriority";

    public ChatPrefixModule() {
        this.alteredPrefix = new HashSet<>();

        // Load UUID with modified player list
        List<String> l = ArcaneBungee.getInstance().getCacheData().getStringList(ALTERED_PREFIX_PATH);
        for (String s : l) {
            this.alteredPrefix.add(UUID.fromString(s));
        }
    }

    public void saveConfig() {
        List<String> ap = new ArrayList<>();

        for (UUID u : alteredPrefix)
            ap.add(u.toString());

        ArcaneBungee.getInstance().getCacheData().set(ALTERED_PREFIX_PATH, ap);
    }

    public Collection<UUID> getAlteredPrefixPlayers() {
        return alteredPrefix;
    }

    private LuckPermsApi getLpApi() {
        return LuckPerms.getApi();
    }

    private CompletableFuture<User> getUser(UUID uuid) {
        CompletableFuture<User> future = new CompletableFuture<>();
        User u = getLpApi().getUser(uuid);
        if (u == null) {
            getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync((user) -> {
                future.complete(user);
                getLpApi().cleanupUser(user);
            });
        } else {
            future.complete(u);
        }
        return future;
    }

    private void saveUser(User user) {
        getLpApi().getUserManager().saveUser(user);
        getLpApi().getMessagingService().ifPresent(ms -> ms.pushUserUpdate(user));
    }

    // Return true if changed
    private void setPriority(User user, int priority) {
        // Check
        String oldPriority = user.getCachedData().getMetaData(Contexts.global()).getMeta().get(PREFIX_PRIORITY_STRING);

        // If already the same
        if (String.valueOf(priority).equals(oldPriority))
            return;

        clearPriority(user);

        // Set if not already this value
        Node node = getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, String.valueOf(priority)).build();
        user.setPermission(node);
        alteredPrefix.add(user.getUuid());
    }

    // Return true if changed
    private boolean clearPriority(User user) {
        // Check
        String oldPriority = user.getCachedData().getMetaData(Contexts.global()).getMeta().get(PREFIX_PRIORITY_STRING);

        // If already cleared
        if (oldPriority == null)
            return false;

        // Remove priority node
        Node node = getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, oldPriority).build();
        user.unsetPermission(node);
        alteredPrefix.remove(user.getUuid());

        return true;
    }

    private String getCurrentPrefix(User user) {
        if (user == null)
            return null;

        MetaData md = user.getCachedData().getMetaData(Contexts.global());
        try {
            int index = Integer.valueOf(md.getMeta().get(PREFIX_PRIORITY_STRING));
            if (index != -1)
                return md.getPrefixes().get(index);
        } catch (NumberFormatException ignored) {
            return md.getPrefix();
        }

        return null;
    }

    private Integer prefixToPriority(User user, String prefix) {
        for (Map.Entry<Integer, String> e : user.getCachedData().getMetaData(Contexts.global()).getPrefixes().entrySet()) {
            if (prefix.equalsIgnoreCase(e.getValue()))
                return e.getKey();
        }
        return null;
    }

    private boolean clearCustomPrefix(User user) {
        MetaData md = user.getCachedData().getMetaData(Contexts.global());
        // Clear pre-existing custom prefix
        String prefix = md.getPrefixes().get(CUSTOM_PREFIX_PRIORITY);
        if (prefix == null) {
            return false;
        }

        Node node = getLpApi().getNodeFactory().makePrefixNode(CUSTOM_PREFIX_PRIORITY, prefix).build();
        user.unsetPermission(node);

        // Clear priority if it was set to custom tag
        if (String.valueOf(CUSTOM_PREFIX_PRIORITY).equals(md.getMeta().get(PREFIX_PRIORITY_STRING)))
            clearPriority(user);

        return true;
    }

    private boolean clearTempPrefix(User user) {
        MetaData md = user.getCachedData().getMetaData(Contexts.global());
        // Clear pre-existing custom prefix
        String prefix = md.getPrefixes().get(TEMP_PREFIX_PRIORITY);

        if (prefix == null) {
            return false;
        }

        // Temporary nodes will fail. Find the correct node with temporary duration and then end it.
        for (Node n : user.getTemporaryPermissionNodes()) { // Not sure if this is big enough to hold the thread...
            if (!n.isPrefix())
                continue;
            try {
                if (!n.getPrefix().getKey().equals(TEMP_PREFIX_PRIORITY))
                    continue;
                user.unsetPermission(n);
                return true;
            } catch (IllegalStateException ignore) {} // basically continue;
        }

        // Probably will be false always. Added just in case
        if (String.valueOf(TEMP_PREFIX_PRIORITY).equals(md.getMeta().get(PREFIX_PRIORITY_STRING))) {
            clearPriority(user);
            return true;
        }

        return false;
    }

    public CompletableFuture<SortedMap<Integer, String>> listBadges(UUID uuid) {
        CompletableFuture<SortedMap<Integer, String>> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            SortedMap<Integer, String> l = md.getPrefixes();
            future.complete(l);
        });

        return future;
    }

    // TODO: Break this up into BadgeCommand and BadgeAdminCommand
    @Deprecated
    public CompletableFuture<BaseComponent> badgeList(UUID uuid, boolean admin) {
        CompletableFuture<BaseComponent> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            SortedMap<Integer, String> l = md.getPrefixes();

            if (l.isEmpty()) {
                BaseComponent ret = new TextComponent((admin ? user.getName() + " does": "You do" ) + " not have any badges");
                ret.setColor(ArcaneColor.CONTENT);
                future.complete(ret);
                return;
            }

            // Check for current PrefixPriority meta
            Integer prefixPriority;
            String prefixPriorityString = md.getMeta().get(PREFIX_PRIORITY_STRING);
            if (prefixPriorityString == null) {
                prefixPriority = null;
            } else {
                try {
                    prefixPriority = Integer.parseInt(prefixPriorityString);
                } catch (NumberFormatException e) {
                    prefixPriority = -1;
                }
            }

            BaseComponent ret = new TextComponent(admin ? user.getName() + "'s badges: " : "Your badges: ");
            ret.setColor(ArcaneColor.CONTENT);

            TextComponent tc = new TextComponent("(hide)");
            tc.setItalic(true);

            String pre = admin ? "/badgeadmin setpriority " + user.getName() + " " : "/badge ";

            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pre + "-hide"));
            ret.addExtra(tc);

            ret.addExtra(" ");

            Iterator<Map.Entry<Integer, String>> i = l.entrySet().iterator();
            String first = i.next().getValue();

            tc = new TextComponent(TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', first)
            ));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    admin ? "/badgeadmin reset " + user.getName() : "/badge -reset"));
            ret.addExtra(tc);

            while (i.hasNext()) {
                Map.Entry<Integer, String> e = i.next();

                ret.addExtra(" ");

                tc = new TextComponent(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', e.getValue())
                ));
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pre + e.getKey()));
                ret.addExtra(tc);
            }

            ret.addExtra("; Current: ");

            String current;
            if (prefixPriority == null) {
                current = md.getPrefix();
            } else {
                current = l.get(prefixPriority);
            }

            if (current == null) {
                tc = new TextComponent("(none)");
                tc.setColor(ArcaneColor.LINK_CONTENT);
            } else {
                tc = new TextComponent(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', current)
                ));
            }
            ret.addExtra(tc);

            future.complete(ret);
        });

        return future;
    }

    public CompletableFuture<String> setPriority(UUID uuid, int priority) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            // Check if prefix by priority exists
            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            String ret = priority == -1 ? "" : md.getPrefixes().get(priority);
            if (ret != null) {
                // Must remove old before setting new.
                setPriority(user, priority);
                saveUser(user);
            }
            future.complete(ret);
        });

        return future;
    }

    public CompletableFuture<Boolean> setPrefix(UUID uuid, String prefix) {
        return setPrefix(uuid, prefix, false);
    }

    public CompletableFuture<Boolean> setPrefix(UUID uuid, String prefix, boolean customPrefix) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            Integer priority = prefixToPriority(user, prefix);

            if (priority != null) {
                // given prefix is in user's collection
                MetaData md = user.getCachedData().getMetaData(Contexts.global());

                if (Objects.equals(md.getPrefix(), md.getPrefixes().get(priority))) {
                    // Given prefix is the highest (reset)
                    clearPriority(user);
                    future.complete(true);
                } else {
                    // Given prefix is lower (or hide)
                    setPriority(user, priority);
                    future.complete(true);
                }
                saveUser(user);

            } else if (customPrefix){
                // custom is allowed
                Node node = getLpApi().getNodeFactory().makePrefixNode(CUSTOM_PREFIX_PRIORITY, prefix).build();
                user.setPermission(node);
                alteredPrefix.add(uuid);
                // Prefix doesn't exist for user and custom was set
                future.complete(false);
                saveUser(user);

            } else {
                // Prefix doesn't exist for user
                future.complete(false);

            }
        });

        return future;
    }

    public CompletableFuture<Void> setTempPrefix(UUID uuid, String prefix, int duration, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            clearTempPrefix(user);
            clearPriority(user);

            Node node = getLpApi().getNodeFactory().makePrefixNode(TEMP_PREFIX_PRIORITY, prefix).setExpiry(duration, unit).build();
            user.setPermission(node);
            alteredPrefix.add(uuid);

            future.complete(null);

            saveUser(user);
        });

        return future;
    }

    public CompletableFuture<String> clearPriority(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            // Ideally there would be only one meta set
            boolean changed = clearPriority(user);
            future.complete(getCurrentPrefix(user));
            if (changed) {
                saveUser(user);
            }
        });

        return future;
    }

    public CompletableFuture<String> clearCustomPrefix(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            boolean changed = clearCustomPrefix(user);
            future.complete(getCurrentPrefix(user));
            if (changed)
                saveUser(user);
        });

        return future;
    }

    public CompletableFuture<String> clearTempPrefix(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getUser(uuid).thenAccept(user -> {
            boolean changed = (clearTempPrefix(user));
            future.complete(getCurrentPrefix(user));
            if (changed)
                saveUser(user);
        });

        return future;
    }
}
