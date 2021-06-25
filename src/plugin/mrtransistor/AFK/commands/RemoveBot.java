package plugin.mrtransistor.AFK.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import plugin.mrtransistor.AFK.DummyPlayer;

import java.util.ArrayList;
import java.util.List;

public class RemoveBot implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) return false;
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        int iter = 0;
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(iter);
            if (dummy.getName().equals(args[0])) {
                sender.sendMessage("Bot '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RESET + "' with UUID:["
                        + ChatColor.GOLD + dummy.getUUID().toString() + ChatColor.RESET + "] was removed");
                dummy.remove();
                botsRemoved++;
            } else iter++;
        }

        if (botsRemoved == 0) sender.sendMessage(ChatColor.RED + "There are no bots with specified name");
        else if (botsRemoved == 1)
            sender.sendMessage(ChatColor.GREEN + "1 bot was removed");
        else sender.sendMessage(ChatColor.GREEN + Integer.toString(botsRemoved) + " bots were removed");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
