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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import plugin.mrtransistor.AFK.DummyPlayer;

import java.util.ArrayList;
import java.util.List;

public class AFKUtils implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("controlBot")) {
            if (args.length >= 2) {
                for (DummyPlayer dummy : DummyPlayer.dummies) {
                    if (dummy.getName().getContents().equals(args[1])) {
                        if (args.length >= 3) {
                            if (args[2].equalsIgnoreCase("attackOnce")) {
                                dummy.attackOnce();
                            } else if (args[2].equalsIgnoreCase("toggle")) {
                                if (args.length >= 4) {
                                    if (args[3].equalsIgnoreCase("selfDefence")) {
                                        dummy.setSelfDefending(!dummy.isSelfDefending());
                                        sender.sendMessage(ChatColor.GREEN + "Self defence is toggled " + ChatColor.DARK_GREEN + (dummy.isSelfDefending() ? "ON" : "OFF"));
                                    } else if (args[3].equalsIgnoreCase("attackContinuous")) {
                                        dummy.setAttackingContinuous(!dummy.isAttackingContinuous());
                                        sender.sendMessage(ChatColor.GREEN + "Continuous attacking is toggled " + ChatColor.DARK_GREEN + (dummy.isAttackingContinuous() ? "ON" : "OFF"));
                                    } else if (args[3].equalsIgnoreCase("setForcePoI")) {
                                        dummy.setForcePoI(!dummy.isForcePoI());
                                        sender.sendMessage(ChatColor.GREEN + "Force point of interest is toggled " + ChatColor.DARK_GREEN + (dummy.isForcePoI() ? "ON" : "OFF"));
                                    } else if (args[3].equalsIgnoreCase("ticking")) {
                                        dummy.setTicking(!dummy.isTicking());
                                        sender.sendMessage(ChatColor.GREEN + "Entity ticking is " + ChatColor.DARK_GREEN + (dummy.isTicking() ? "enabled" : "disabled"));
                                    }
                                    return true;
                                } else return false;
                            }
                            return true;
                        } else return false;
                    }
                }
                sender.sendMessage(ChatColor.RED + "There is no bot with name '" + ChatColor.DARK_GREEN + args[1] + ChatColor.RED + "'");
                return true;
            } else return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> tabCompletion = new ArrayList<>();
        switch (args.length) {
            case 1:
                tabCompletion.add("controlBot");
                break;
            case 2:
                if (args[0].equalsIgnoreCase("controlBot")) {
                    for (DummyPlayer dummy : DummyPlayer.dummies) tabCompletion.add(dummy.getName().getContents());
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("controlBot")) {
                    tabCompletion.add("attackOnce");
                    tabCompletion.add("toggle");
                }
                break;
            case 4:
                if (args[0].equalsIgnoreCase("controlBot")) {
                    if (args[2].equalsIgnoreCase("toggle")) {
                        tabCompletion.add("selfDefence");
                        tabCompletion.add("attackContinuous");
                        tabCompletion.add("setForcePoI");
                        tabCompletion.add("ticking");
                    }
                }
        }
        return tabCompletion;
    }
}
