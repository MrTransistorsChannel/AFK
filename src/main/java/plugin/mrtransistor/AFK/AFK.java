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

import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.mrtransistor.AFK.commands.AFKUtils;
import plugin.mrtransistor.AFK.commands.RemoveAllBots;
import plugin.mrtransistor.AFK.commands.RemoveBot;
import plugin.mrtransistor.AFK.commands.SpawnBot;

public class AFK extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("spawnBot").setExecutor(new SpawnBot());
        getCommand("spawnBot").setTabCompleter(new SpawnBot());
        getCommand("removeBot").setExecutor(new RemoveBot());
        getCommand("removeBot").setTabCompleter(new RemoveBot());
        getCommand("removeAllBots").setExecutor(new RemoveAllBots());
        getCommand("removeAllBots").setTabCompleter(new RemoveAllBots());
        getCommand("afkutils").setExecutor(new AFKUtils());
        getCommand("afkutils").setTabCompleter(new AFKUtils());

        getLogger().info("AFK plugin started!");
    }

    @Override
    public void onDisable() {
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            getLogger().info("Bot '" + ChatColor.DARK_GREEN + dummy.getName().getContents() + ChatColor.RESET + "' with UUID:["
                    + ChatColor.GOLD + dummy.getStringUUID() + ChatColor.RESET + "] was removed");
            dummy.remove("Plugin stopped");
            botsRemoved++;
        }
        if (botsRemoved == 0) getLogger().info(ChatColor.GREEN + "No bots were removed");
        else if (botsRemoved == 1)
            getLogger().info(ChatColor.GREEN + "1 bot was removed");
        else getLogger().info(ChatColor.GREEN + Integer.toString(botsRemoved) + " bots were removed");
        getLogger().info("AFK plugin stopped!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ServerGamePacketListenerImpl plc = ((CraftPlayer) e.getPlayer()).getHandle().connection;
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            plc.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, dummy));
            plc.send(new ClientboundAddPlayerPacket(dummy));
            plc.send(new ClientboundSetEntityDataPacket(dummy.getId(), dummy.getEntityData(), true));
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
