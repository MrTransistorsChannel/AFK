package plugin.mrtransistor.AFK.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import plugin.mrtransistor.AFK.DummyPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpawnBot implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) return false;
        if (sender instanceof Player) {
            if (args[0].length() > 16) {
                sender.sendMessage(ChatColor.RED + "Player names can`t be longer than 16 symbols");
                return true;
            }
            if (!DummyPlayer.dummyNames.contains(args[0])) {
                DummyPlayer plr = new DummyPlayer((Player) sender, args[0]);
                try {
                    plr.spawn(((Player) sender).getLocation());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sender.sendMessage("Bot '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RESET + "' with UUID:["
                        + ChatColor.GOLD + plr.getUUID().toString() + ChatColor.RESET + "] spawned");
                return true;
            }
            else {
                sender.sendMessage(ChatColor.RED + "Bot with name '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RED + "' already exists");
                return true;
            }
        }

        sender.sendMessage("Sorry, this command is only for players");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
