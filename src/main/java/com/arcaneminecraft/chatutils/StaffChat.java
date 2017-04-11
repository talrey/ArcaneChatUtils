package com.arcaneminecraft.chatutils;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

class StaffChat implements ChatTogglable, CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String PERMISSION_NODE = "arcane.staffchat";
	private static final String TAG = "Staff";
	private final HashSet<Player> toggled = new HashSet<>();
	
	StaffChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean isToggled(Player p) {
		return toggled.contains(p);
	}

	@Override
	public void runToggled(Player p, String msg) {
		String[] m = { msg };
		broadcast(p, m);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission(PERMISSION_NODE)) {
			sender.sendMessage(ArcaneCommons.noPermissionMsg());
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("a")) {
    		if (args.length == 0) {
        		if (!(sender instanceof Player)) {
        			sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /a <message>"));
        			return true;
        		}
    			sender.sendMessage(ArcaneCommons.tag(TAG, "Your toggle is currently "
    					+ (toggled.contains(sender)? ColorPalette.POSITIVE + "on": ColorPalette.NEGATIVE + "off")
    					+ ColorPalette.CONTENT + ". Usage: /a <message>"));
    			return true;
    		}
			broadcast(sender, args);
			return true;
		}
		
    	if (cmd.getName().equalsIgnoreCase("atoggle") && sender.hasPermission("simonplugin.a")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage(ArcaneCommons.tag(TAG, "You must be a player."));
    			return true;
    		}
    		Player p = (Player) sender;
    		
    		if (toggled.add(p))
    		{
    			sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.POSITIVE + "on" + ColorPalette.CONTENT + "."));
    		} else {
    			toggled.remove(p);
    			sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.NEGATIVE + "off" + ColorPalette.CONTENT + "."));
    		}
    		
    		return true;
    	}
    	
    	return false;
	}
	
	private void broadcast (CommandSender sender, String[] args) {
		String msg = ColorPalette.HEADING + "Staff // "
				+ ColorPalette.RESET + sender.getName()
				+ ": "
				+ ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&',String.join(" ", args));
		
		
		plugin.getServer().broadcast(msg, PERMISSION_NODE);
	}
}
