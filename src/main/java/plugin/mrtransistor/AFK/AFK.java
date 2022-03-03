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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.mrtransistor.AFK.commands.CommandDispatcher;

import java.io.File;
import java.io.IOException;

public class AFK extends JavaPlugin implements Listener {

    private File botSaveFile;
    private FileConfiguration botSaveYml;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createBotSaveFile();

        Bukkit.getPluginManager().registerEvents(this, this);
        CommandDispatcher commandDispatcher = new CommandDispatcher(this);

        getCommand("spawnBot").setExecutor(commandDispatcher);
        getCommand("spawnBot").setTabCompleter(commandDispatcher);
        getCommand("removeBot").setExecutor(commandDispatcher);
        getCommand("removeBot").setTabCompleter(commandDispatcher);
        getCommand("removeAllBots").setExecutor(commandDispatcher);
        getCommand("removeAllBots").setTabCompleter(commandDispatcher);
        getCommand("afkutils").setExecutor(commandDispatcher);
        getCommand("afkutils").setTabCompleter(commandDispatcher);

        spawnBotsOnLoad();

        getLogger().info("AFK plugin started!");
    }

    @Override
    public void onDisable() {
        removeBotsOnUnload();
        getLogger().info("AFK plugin stopped!");
    }

    private void spawnBotsOnLoad() {
        int bots_spawned = 0;
        for (String name : botSaveYml.getKeys(false)) {
            /*Player spawner = (Player) getServer().getOfflinePlayer(UUID.fromString(botSaveYml.getConfigurationSection(name)
                    .getString("spawner")));*/
            World world = getServer().getWorld(botSaveYml.getConfigurationSection(name).getString("level"));
            double x = botSaveYml.getConfigurationSection(name).getDouble("x");
            double y = botSaveYml.getConfigurationSection(name).getDouble("y");
            double z = botSaveYml.getConfigurationSection(name).getDouble("z");
            float yaw = (float) botSaveYml.getConfigurationSection(name).getDouble("yaw");
            float pitch = (float) botSaveYml.getConfigurationSection(name).getDouble("pitch");
            Location location = new Location(world, x, y, z, yaw, pitch);
            DummyPlayer.spawnBot(name, location, null);
        }
    }

    private void removeBotsOnUnload() {
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            getLogger().info("Bot '" + ChatColor.DARK_GREEN + dummy.getName().getContents() + ChatColor.RESET + "' with UUID:["
                    + ChatColor.GOLD + dummy.getStringUUID() + ChatColor.RESET + "] was removed");
            dummy.softRemove();
            botsRemoved++;
        }
        if (botsRemoved == 0) getLogger().info(ChatColor.GREEN + "No bots were removed");
        else if (botsRemoved == 1)
            getLogger().info(ChatColor.GREEN + "1 bot was removed");
        else getLogger().info(ChatColor.GREEN + Integer.toString(botsRemoved) + " bots were removed");
    }

    private void createBotSaveFile() {
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

    public FileConfiguration getBotSaveFile() {
        return botSaveYml;
    }

    public void saveBotSaveFile() {
        try {
            botSaveYml.save(botSaveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (((CraftEntity) e.getEntity()).getHandle() instanceof DummyPlayer) {
            e.setDroppedExp(e.getEntity().getTotalExperience());
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        if (((CraftEntity) e.getEntity()).getHandle() instanceof DummyPlayer) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }
}
