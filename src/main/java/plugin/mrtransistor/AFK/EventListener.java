package plugin.mrtransistor.AFK;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class EventListener implements Listener {
    @EventHandler // handles removing of player exp cap on death for bots
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (((CraftEntity) e.getEntity()).getHandle() instanceof DummyPlayer)
            e.setDroppedExp(e.getEntity().getTotalExperience());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!(((CraftEntity) e.getPlayer()).getHandle() instanceof DummyPlayer)) {
            FileConfiguration botSaveYml = Bukkit.getPluginManager().getPlugin("AFK").getConfig();
            List<String> blacklistedNames = new ArrayList<>();
            if (botSaveYml.contains("blacklistedNames"))
                blacklistedNames = botSaveYml.getStringList("blacklistedNames");
            if (!blacklistedNames.contains(e.getPlayer().getName()))
                blacklistedNames.add(e.getPlayer().getName());
            botSaveYml.set("blacklistedNames", blacklistedNames);
            Bukkit.getPluginManager().getPlugin("AFK").saveConfig();
        }
    }

    @EventHandler // handles removing bot from list if lost connection not by DummyPlayer#die() or #disconnect()
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        if (((CraftEntity) e.getPlayer()).getHandle() instanceof DummyPlayer) {
            DummyPlayer.dummies.remove((DummyPlayer) ((CraftEntity) e.getPlayer()).getHandle());
            DummyPlayer.names.remove(e.getPlayer().getName());
        }
    }
}
