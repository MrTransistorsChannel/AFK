/*   Plugin that adds server-side bots
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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import plugin.mrtransistor.AFK.DummyPlayer;

import java.util.ArrayList;
import java.util.List;

public class RemoveAllBots implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            sender.sendMessage("Bot '" + ChatColor.DARK_GREEN + dummy.getName() + ChatColor.RESET + "' with UUID:["
                    + ChatColor.GOLD + dummy.getUniqueIDString() + ChatColor.RESET + "] was removed");
            dummy.remove("Removed using command");
            botsRemoved++;
        }
        if (botsRemoved == 0) sender.sendMessage(ChatColor.RED + "There are no bots");
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
