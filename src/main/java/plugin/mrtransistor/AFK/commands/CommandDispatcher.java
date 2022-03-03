package plugin.mrtransistor.AFK.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import plugin.mrtransistor.AFK.AFK;
import plugin.mrtransistor.AFK.DummyPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CommandDispatcher implements TabExecutor {

    static AFK _plg;
    PluginDescriptionFile pluginDotYml;
    HashMap<String, String> aliasMap = new HashMap<>();

    public CommandDispatcher(AFK plg) {
        pluginDotYml = plg.getDescription();
        _plg = plg;
        for (String cmd : pluginDotYml.getCommands().keySet()) {
            if (!pluginDotYml.getCommands().get(cmd).containsKey("aliases")) continue;
            for (String alias : (List<String>) pluginDotYml.getCommands().get(cmd).get("aliases"))
                aliasMap.put(alias, cmd);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (aliasMap.containsKey(label)) label = aliasMap.get(label);
        if (pluginDotYml.getCommands().get(label) == null) return false;
        if (args.length == 0) {
            if (pluginDotYml.getCommands().get(label).get("handler") == null) return false;
            try {
                getClass().getMethod(
                                pluginDotYml.getCommands().get(label).get("handler").toString(),
                                CommandSender.class, String[].class)
                        .invoke(null, sender, args);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            Map<String, Object> _args = (Map<String, Object>) pluginDotYml.getCommands().get(label).get("args");
            Pattern numeric = Pattern.compile("~?-?\\d+(?:\\.\\d+)?|~");
            for (String arg : args) {
                Map<String, Object> _args_bc = _args;
                if (numeric.matcher(arg).matches())
                    _args = (Map<String, Object>) _args.get("__numeric__");
                else
                    _args = (Map<String, Object>) _args.get(arg);
                if (_args == null) {
                    if (_args_bc.containsKey("__text__")) _args = (Map<String, Object>) _args_bc.get("__text__");
                    else return false;
                }
            }
            try {
                getClass().getMethod(_args.get("handler").toString(), CommandSender.class, String[].class)
                        .invoke(null, sender, args);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (aliasMap.containsKey(label)) label = aliasMap.get(label);
        if (pluginDotYml.getCommands().get(label) == null) return null;
        if (args.length == 1) {
            if (pluginDotYml.getCommands().get(label).get("tabCompletion") == null) return null;
            return (List<String>) pluginDotYml.getCommands().get(label).get("tabCompletion");
        } else {
            Map<String, Object> _args = (Map<String, Object>) pluginDotYml.getCommands().get(label).get("args");
            Pattern numeric = Pattern.compile("~?-?\\d+(?:\\.\\d+)?|~");
            for (int i = 0; i < args.length - 1; i++) {
                Map<String, Object> _args_old = _args;
                if (numeric.matcher(args[i]).matches()) {
                    _args = (Map<String, Object>) _args.get("__numeric__");
                } else {
                    _args = (Map<String, Object>) _args.get(args[i]);
                }
                if (_args == null) {
                    if (_args_old.containsKey("__text__")) {
                        _args = (Map<String, Object>) _args_old.get("__text__");
                    } else return new ArrayList<>();
                }
            }
            return (List<String>) _args.get("tabCompletion");
        }
    }

    public static void attackOnce(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[1]))
                dummy.attackOnce();
        }
    }

    public static void attackContinuous(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[1])) {
                dummy.setAttackingContinuous(!dummy.isAttackingContinuous());
                sender.sendMessage(ChatColor.GREEN + "Continuous attacking is toggled " + ChatColor.DARK_GREEN
                        + (dummy.isAttackingContinuous() ? "ON" : "OFF"));
            }
        }
    }

    public static void selfDefence(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[1])) {
                dummy.setSelfDefending(!dummy.isSelfDefending());
                sender.sendMessage(ChatColor.GREEN + "Self defence is toggled " + ChatColor.DARK_GREEN
                        + (dummy.isSelfDefending() ? "ON" : "OFF"));
            }
        }
    }

    public static void setForcePoI(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[1])) {
                dummy.setForcePoI(!dummy.isForcePoI());
                sender.sendMessage(ChatColor.GREEN + "Force point of interest is toggled " + ChatColor.DARK_GREEN
                        + (dummy.isForcePoI() ? "ON" : "OFF"));
            }
        }
    }

    public static void ticking(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[1])) {
                dummy.setTicking(!dummy.isTicking());
                sender.sendMessage(ChatColor.GREEN + "Entity ticking is " + ChatColor.DARK_GREEN
                        + (dummy.isTicking() ? "enabled" : "disabled"));
            }
        }
    }

    public static void spawnBot(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            if (args[0].length() > 16) {
                sender.sendMessage(ChatColor.RED + "Player names can`t be longer than 16 symbols");
                return;
            }
            if (!DummyPlayer.dummyNames.contains(args[0])) {
                DummyPlayer dummy = DummyPlayer.spawnBot(args[0], ((Player) sender).getLocation(), (Player) sender);

                String json = "[{\"text\":\"Bot '\", \"color\":\"white\"}," +
                        "{\"text\":\"" + dummy.getName().getContents() + "\", \"color\":\"dark_green\"}," +
                        "{\"text\":\"' with UUID:[\", \"color\":\"white\"}," +
                        "{\"text\":\"" + dummy.getStringUUID() + "\", \"color\":\"gold\"," +
                        " \"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + dummy.getStringUUID() + "\"}," +
                        " \"hoverEvent\":{\"action\":\"show_text\", \"value\":\"Click to copy to clipboard\"}}," +
                        "{\"text\":\"] spawned\", \"color\":\"white\"}]";

                Bukkit.dispatchCommand(sender, "tellraw " + sender.getName() + " " + json);
            } else {
                sender.sendMessage(ChatColor.RED + "Bot with name '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RED
                        + "' already exists");
            }
            return;
        }
        sender.sendMessage("Sorry, this command is only for players");
    }

    public static void removeBot(CommandSender sender, String[] args) {
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            if (dummy.getName().getContents().equals(args[0])) {
                sender.sendMessage("Bot '" + ChatColor.DARK_GREEN + args[0] + ChatColor.RESET + "' with UUID:["
                        + ChatColor.GOLD + dummy.getStringUUID() + ChatColor.RESET + "] was removed");
                dummy.remove("Removed using command");
                FileConfiguration botSaveYml = _plg.getBotSaveFile();
                botSaveYml.set(args[0], null);
                _plg.saveBotSaveFile();
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "There is no bot with specified name");
    }

    public static void removeAllBots(CommandSender sender, String[] args) {
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            sender.sendMessage("Bot '" + ChatColor.DARK_GREEN + dummy.getName().getContents() + ChatColor.RESET
                    + "' with UUID:["
                    + ChatColor.GOLD + dummy.getStringUUID() + ChatColor.RESET + "] was removed");
            dummy.remove("Removed using command");
            botsRemoved++;
        }
        if (botsRemoved == 0) sender.sendMessage(ChatColor.RED + "There are no bots");
        else if (botsRemoved == 1)
            sender.sendMessage(ChatColor.GREEN + "1 bot was removed");
        else sender.sendMessage(ChatColor.GREEN + Integer.toString(botsRemoved) + " bots were removed");
    }

}