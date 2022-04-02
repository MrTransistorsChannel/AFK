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
package plugin.mrtransistor.AFK;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.mrtransistor.AFK.commands.CommandDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AFK extends JavaPlugin {

    private File botSaveFile;
    private FileConfiguration botSaveYml;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBotSaveFile();

        CommandDispatcher commandDispatcher = new CommandDispatcher(this);
        EventListener eventListener = new EventListener();
        Bukkit.getPluginManager().registerEvents(eventListener, this);


        getCommand("spawnBot").setExecutor(commandDispatcher);
        getCommand("spawnBot").setTabCompleter(commandDispatcher);
        getCommand("removeAllBots").setExecutor(commandDispatcher);
        getCommand("removeAllBots").setTabCompleter(commandDispatcher);
        getCommand("afkutils").setExecutor(commandDispatcher);
        getCommand("afkutils").setTabCompleter(commandDispatcher);

        Bukkit.getScheduler().runTaskLater(this, this::loadBots, 20);

        getLogger().info("AFK plugin started!");
    }

    @Override
    public void onDisable() {
        saveBots();
        getLogger().info("AFK plugin stopped!");
    }

    private void loadBots() {
        ConfigurationSection section = botSaveYml.getConfigurationSection("botsToReload");
        if(section == null) return;
        List<String> botsToLoad = new ArrayList<>(botSaveYml.getConfigurationSection("botsToReload").getKeys(false));
        System.out.println("satsrtbreab");
        if (botsToLoad.isEmpty()) return;
        getLogger().info(ChatColor.GREEN + "Loading " + botsToLoad.size()
                + (botsToLoad.size() == 1 ? " bot" : " bots"));
        for (String bot : botsToLoad) {
            DummyPlayer.spawnBot(bot, null, null, false);
        }
    }

    private void saveBots() {
        if (!DummyPlayer.dummies.isEmpty())
            getLogger().info(ChatColor.GREEN + "Saving " + DummyPlayer.dummies.size()
                    + (DummyPlayer.dummies.size() == 1 ? " bot" : " bots"));
        botSaveYml.set("botsToReload", null);
        botSaveYml.createSection("botsToReload");
        while (!DummyPlayer.dummies.isEmpty()) {
            DummyPlayer botToSave = DummyPlayer.dummies.get(0);
            botSaveYml.createSection("botsToReload." + botToSave.getScoreboardName());
            GameProfile gameProfile = botToSave.getGameProfile();
            if(gameProfile.getProperties().containsKey("textures")) {
                Property textures = gameProfile.getProperties().get("textures").iterator().next();
                botSaveYml.set("botsToReload." + botToSave.getScoreboardName() + ".texture", textures.getValue().toString());
                botSaveYml.set("botsToReload." + botToSave.getScoreboardName() + ".signature", textures.getSignature().toString());
            }
            botToSave.disconnect("Plugin unload");
        }
        saveBotSaveFile();
    }

    private void loadBotSaveFile() {
        botSaveFile = new File(getDataFolder(), "botSave.yml");
        if (!botSaveFile.exists()) {
            botSaveFile.getParentFile().mkdirs();
            saveResource("botSave.yml", false);
        }

        botSaveYml = new YamlConfiguration();
        try {
            botSaveYml.load(botSaveFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveBotSaveFile() {
        try {
            botSaveYml.save(botSaveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getBotSaveYml(){
        return botSaveYml;
    }
}
