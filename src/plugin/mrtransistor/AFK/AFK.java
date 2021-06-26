package plugin.mrtransistor.AFK;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Attack test
        /*new BukkitRunnable() {
            @Override
            public void run() {
                for (DummyPlayer dummy : DummyPlayer.dummies) {
                    LivingEntity target = dummy.getTargetedLivingEntity();
                    if (target != null) {
                        dummy.swingHand(EnumHand.MAIN_HAND, true);
                        dummy.attack(target);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);*/

        getLogger().info("AFK plugin started!");
    }

    @Override
    public void onDisable() {
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            getLogger().info("Bot '" + ChatColor.DARK_GREEN + dummy.getName() + ChatColor.RESET + "' with UUID:["
                    + ChatColor.GOLD + dummy.getUniqueIDString() + ChatColor.RESET + "] was removed");
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
        PlayerConnection plc = ((CraftPlayer) e.getPlayer()).getHandle().playerConnection;
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            plc.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                    dummy));
            plc.sendPacket(new PacketPlayOutNamedEntitySpawn(dummy));
            plc.sendPacket(new PacketPlayOutEntityMetadata(dummy.getId(),
                    dummy.getDataWatcher(), true));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        getLogger().info("player died");
        if(((CraftEntity)e.getEntity()).getHandle() instanceof DummyPlayer){
            e.setDroppedExp(e.getEntity().getTotalExperience());
        }
    }
}
