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
        if(args.length >= 1 && args[0].equalsIgnoreCase("controlBot")){
            if(args.length >= 2) {
                boolean botFound = false;
                for (DummyPlayer dummy : DummyPlayer.dummies) {
                    if (dummy.getName().equals(args[1])) {
                        botFound = true;
                        if(args.length >= 3) {
                            if (args[2].equalsIgnoreCase("attackOnce")) {
                                dummy.attackOnce();
                            }
                            else if(args[2].equalsIgnoreCase("toggle")) {
                                if(args.length >= 4){
                                    if(args[3].equalsIgnoreCase("selfDefence")){
                                        dummy.setSelfDefending(!dummy.getSelfDefending());
                                        sender.sendMessage(ChatColor.GREEN + "Self defence is toggled " + ChatColor.DARK_GREEN + (dummy.getSelfDefending() ? "ON" : "OFF"));
                                    }
                                    else if(args[3].equalsIgnoreCase("attackContinuous")){
                                        dummy.setAttackingContinuous(!dummy.getAttackingContinuous());
                                        sender.sendMessage(ChatColor.GREEN + "Continuous attacking is toggled " + ChatColor.DARK_GREEN + (dummy.getAttackingContinuous() ? "ON" : "OFF"));
                                    }
                                    else if(args[3].equalsIgnoreCase("setForcePoI")){
                                        dummy.setForcePoI(!dummy.getForcePoI());
                                        sender.sendMessage(ChatColor.GREEN + "Force point of interest is toggled " + ChatColor.DARK_GREEN + (dummy.getForcePoI() ? "ON" : "OFF"));
                                    }
                                    return true;
                                }
                                else return false;
                            }
                            return true;
                        }
                        else return false;
                    }
                }
                if(!botFound){
                    sender.sendMessage(ChatColor.RED + "There is no bot with name '" + ChatColor.DARK_GREEN + args[1] + ChatColor.RED + "'");
                }
                return true;
            }
            else return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> tabCompletion = new ArrayList<>();
        switch(args.length){
            case 1:
                tabCompletion.add("controlBot");
                break;
            case 2:
                if(args[0].equalsIgnoreCase("controlBot")){
                    for(DummyPlayer dummy : DummyPlayer.dummies) tabCompletion.add(dummy.getName());
                }
                break;
            case 3:
                if(args[0].equalsIgnoreCase("controlBot")){
                    tabCompletion.add("attackOnce");
                    tabCompletion.add("toggle");
                }
                break;
            case 4:
                if(args[0].equalsIgnoreCase("controlBot")){
                    if(args[2].equalsIgnoreCase("toggle")){
                        tabCompletion.add("selfDefence");
                        tabCompletion.add("attackContinuous");
                        tabCompletion.add("setForcePoI");
                    }
                }
        }
        return tabCompletion;
    }
}
