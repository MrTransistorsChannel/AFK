/*
 *  Plugin that adds server-side bots
 *
 *   Copyright (C) 2021  MrTransistor
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package plugin.mrtransistor.AFK.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import plugin.mrtransistor.AFK.DummyPlayer;

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
                DummyPlayer dummy = DummyPlayer.spawnBot(args[0], ((Player) sender).getLocation(), (Player) sender);

                String json = "[{\"text\":\"Bot '\", \"color\":\"white\"}," +
                        "{\"text\":\"" + dummy.getName() + "\", \"color\":\"dark_green\"}," +
                        "{\"text\":\"' with UUID:[\", \"color\":\"white\"}," +
                        "{\"text\":\"" + dummy.getUniqueIDString() + "\", \"color\":\"gold\"," +
                        " \"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + dummy.getUniqueIDString() + "\"}," +
                        " \"hoverEvent\":{\"action\":\"show_text\", \"value\":\"Click to copy to clipboard\"}}," +
                        "{\"text\":\"] spawned\", \"color\":\"white\"}]";

                Bukkit.dispatchCommand(sender, "tellraw " + sender.getName() + " " + json);
            } else {
                sender.sendMessage(ChatColor.RED + "Bot with name '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RED + "' already exists");
            }
            return true;
        }

        sender.sendMessage("Sorry, this command is only for players");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
