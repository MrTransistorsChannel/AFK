package plugin.mrtransistor.AFK;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class DummyPlayer {
    public static ArrayList<DummyPlayer> dummies = new ArrayList<>();
    public static ArrayList<String> dummyNames = new ArrayList<>();

    private final String name;
    private UUID uuid;
    private final Player spawner;
    private WorldServer worldServer;
    private CraftPlayer dummyPlayer;
    private EntityPlayer dummyEntityPlayer;

    private String[] getSkin() {
        EntityPlayer entityPlayer = ((CraftPlayer) this.spawner).getHandle();
        GameProfile gameProfile = entityPlayer.getProfile();
        Property property = gameProfile.getProperties().get("textures").iterator().next();
        String texture = property.getValue();
        String signature = property.getSignature();
        return new String[]{texture, signature};
    }

    public DummyPlayer(Player spawner, String name) {
        this.spawner = spawner;
        this.name = name;
        dummyNames.add(this.name);
        dummies.add(this);
    }

    public String getName() {
        return this.name;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Player getSpawner() {
        return this.spawner;
    }

    public CraftPlayer getPlayer() {
        return this.dummyPlayer;
    }

    public EntityPlayer getEntityPlayer() {
        return this.dummyEntityPlayer;
    }

    public WorldServer getWorldServer() {
        return this.worldServer;
    }

    public void spawn(Location location) throws IOException {
        this.uuid = UUID.randomUUID();
        GameProfile gameProfile = new GameProfile(uuid, name);
        String[] texSign = getSkin();
        gameProfile.getProperties().put("textures", new Property("textures", texSign[0], texSign[1]));
        this.worldServer = ((CraftWorld) location.getWorld()).getHandle();

        MinecraftServer server = ((CraftServer) (Bukkit.getServer())).getServer();
        CraftServer cServer = (CraftServer) Bukkit.getServer();
        NetworkManager networkManager = new DummyNetworkManager();

        dummyEntityPlayer = new EntityPlayer(server, this.worldServer, gameProfile, new PlayerInteractManager(this.worldServer));
        dummyPlayer = new CraftPlayer(cServer, dummyEntityPlayer);

        dummyEntityPlayer.getDataWatcher().set(new DataWatcherObject<>(16, DataWatcherRegistry.a), (byte) 127);

        dummyPlayer.getHandle().playerConnection = new PlayerConnection(server, networkManager, dummyEntityPlayer);
        this.worldServer.addPlayerJoin(dummyEntityPlayer);

        dummyEntityPlayer.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        dummyEntityPlayer.getWorldServer().getChunkProvider().movePlayer(dummyEntityPlayer);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerConnection plc = ((CraftPlayer) player).getHandle().playerConnection;
            plc.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, dummyEntityPlayer));
            plc.sendPacket(new PacketPlayOutNamedEntitySpawn(dummyEntityPlayer));
            plc.sendPacket(new PacketPlayOutEntityMetadata(dummyEntityPlayer.getId(), dummyEntityPlayer.getDataWatcher(), true));
            plc.sendPacket(new PacketPlayOutEntityHeadRotation(dummyEntityPlayer, (byte) (dummyEntityPlayer.yaw * 256 / 360)));
        }
    }

    public void remove() {
        this.dummyPlayer.kickPlayer("");
        this.worldServer.removeEntity(this.dummyEntityPlayer);
        this.dummyPlayer.getHandle().playerConnection = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerConnection plc = ((CraftPlayer) player).getHandle().playerConnection;
            plc.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, dummyEntityPlayer));
        }
        dummyNames.remove(this.name);
        dummies.remove(this);
    }
}
