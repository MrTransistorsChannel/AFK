package plugin.mrtransistor.AFK;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.mrtransistor.AFK.commands.RemoveAllBots;
import plugin.mrtransistor.AFK.commands.RemoveBot;
import plugin.mrtransistor.AFK.commands.SpawnBot;

public class AFK extends JavaPlugin implements Listener {

    private BukkitTask updater;

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

        this.updater = new BukkitRunnable() {
            /** BUGFIX FOR HEAD ROTATION PACKET NOT BEING REGISTERED PROPERLY **/
            @Override
            public void run() {
                for (DummyPlayer dummy : DummyPlayer.dummies) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        EntityPlayer entityPlayer = dummy.getEntityPlayer();
                        PlayerConnection plc = ((CraftPlayer) player).getHandle().playerConnection;
                        plc.sendPacket(new PacketPlayOutEntityHeadRotation(entityPlayer, (byte) (entityPlayer.yaw * 256 / 360)));
                    }
                }
            }
        }.runTaskTimer(this, 0, 5);

        getLogger().info("AFK plugin started!");
    }

    @Override
    public void onDisable() {
        updater.cancel();
        int botsRemoved = 0;
        int numBots = DummyPlayer.dummies.size();
        for (int i = 0; i < numBots; i++) {
            DummyPlayer dummy = DummyPlayer.dummies.get(0);
            getLogger().info("Bot '" + ChatColor.DARK_GREEN + dummy.getName() + ChatColor.RESET + "' with UUID:["
                    + ChatColor.GOLD + dummy.getUUID().toString() + ChatColor.RESET + "] was removed");
            dummy.remove();
            botsRemoved++;
        }
        if (botsRemoved == 0) getLogger().info(ChatColor.GREEN + "No bots were removed");
        else if (botsRemoved == 1)
            getLogger().info(ChatColor.GREEN + "1 bot was removed");
        else getLogger().info(ChatColor.GREEN + Integer.toString(botsRemoved) + " bots were removed");
        getLogger().info("AFK plugin stopped!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        new BukkitRunnable() {
            @Override
            public void run() {
                Entity entity = e.getEntity();
                String name = entity.getName();
                int numBots = DummyPlayer.dummies.size();
                int iter = 0;
                for (int i = 0; i < numBots; i++) {
                    DummyPlayer dummy = DummyPlayer.dummies.get(iter);
                    if (dummy.getName().equals(name)) {
                        dummy.remove();
                    } else iter++;
                }
            }
        }.runTaskLater(this, 20);
    }

    /*@EventHandler
    public void onUnload(ChunkUnloadEvent e){
        getLogger().info("Unloading chunk at " + e.getChunk().getX() + " " + e.getChunk().getZ());
    }

    @EventHandler
    public void onLoad(ChunkLoadEvent e){

        getLogger().info("Loading chunk at " + e.getChunk().getX() + " " + e.getChunk().getZ());
    }*/

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        PlayerConnection plc = ((CraftPlayer) e.getPlayer()).getHandle().playerConnection;
        for (DummyPlayer dummy : DummyPlayer.dummies) {
            EntityPlayer entityPlayer = dummy.getEntityPlayer();
            plc.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                    entityPlayer));
            plc.sendPacket(new PacketPlayOutNamedEntitySpawn(entityPlayer));
            plc.sendPacket(new PacketPlayOutEntityHeadRotation(entityPlayer,
                    (byte) (entityPlayer.yaw * 256 / 360)));
            plc.sendPacket(new PacketPlayOutEntityMetadata(entityPlayer.getId(),
                    entityPlayer.getDataWatcher(), true));
        }
    }
}
