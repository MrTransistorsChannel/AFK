/*   Plugin that adds server-side bots
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
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;

public class DummyPlayer extends EntityPlayer {
    public static ArrayList<DummyPlayer> dummies = new ArrayList<>();
    public static ArrayList<String> dummyNames = new ArrayList<>();

    private static String[] getSkin(EntityPlayer entityPlayer) {
        GameProfile gameProfile = entityPlayer.getProfile();
        Property property = gameProfile.getProperties().get("textures").iterator().next();
        String texture = property.getValue();
        String signature = property.getSignature();
        return new String[]{texture, signature};
    }

    public DummyPlayer(MinecraftServer server, WorldServer world, GameProfile profile, PlayerInteractManager interactManager) {
        super(server, world, profile, interactManager);
        dummyNames.add(this.getName());
        dummies.add(this);
    }

    public static DummyPlayer spawnBot(String name, Location location, Player spawner) {
        MinecraftServer server = ((CraftServer) (Bukkit.getServer())).getServer();
        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        NetworkManager networkManager = new DummyNetworkManager();

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        DummyPlayer dummy = new DummyPlayer(server, worldServer, gameProfile, new PlayerInteractManager(worldServer));

        String[] texSign = getSkin(((CraftPlayer) spawner).getHandle());
        gameProfile.getProperties().put("textures", new Property("textures", texSign[0], texSign[1]));

        dummy.getDataWatcher().set(new DataWatcherObject<>(16, DataWatcherRegistry.a), (byte) 0x7f); // show all model layers

        dummy.playerConnection = new PlayerConnection(server, networkManager, dummy);
        worldServer.addPlayerJoin(dummy);

        dummy.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        dummy.setHeadRotation(location.getYaw());
        dummy.setYawPitch(location.getYaw(), location.getPitch());
        worldServer.getChunkProvider().movePlayer(dummy);


        server.getPlayerList().sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, dummy));
        server.getPlayerList().sendAll(new PacketPlayOutNamedEntitySpawn(dummy));
        server.getPlayerList().sendAll(new PacketPlayOutEntityMetadata(dummy.getId(), dummy.getDataWatcher(), true));
        return dummy;
    }

    @Override
    public void tick() {
        if (this.server.ai() % 10 == 0) {
            this.playerConnection.syncPosition();
            this.getWorldServer().getChunkProvider().movePlayer(this);
        }

        super.tick();
        this.playerTick();
    }

    public void chaseAttacker(){
        Player bukkitPlayer = this.getBukkitEntity();
        EntityLiving damager = this.getLastDamager();

        if(damager == null) return;

        Vector toTarget = this.getBukkitEntity().getEyeLocation().toVector().subtract(((LivingEntity)damager.getBukkitEntity()).getEyeLocation().toVector()).normalize();
        float yaw = (float) Math.atan2(toTarget.getX(), -toTarget.getZ());
        float pitch = (float) Math.atan2(toTarget.getY(), Math.sqrt(toTarget.getX()*toTarget.getX() + toTarget.getZ()*toTarget.getZ()));
        yaw *= 180/Math.PI;
        pitch *= 180/Math.PI;
        this.getBukkitEntity().getServer().getLogger().info(pitch + "");
        this.setYawPitch(yaw, pitch);
        this.setHeadRotation(yaw);
        this.g(new Vec3D(0, 0, 1));
    }

    public LivingEntity getTargetedLivingEntity() {
        Player bukkitPlayer = this.getBukkitEntity();
        World world = bukkitPlayer.getWorld();
        Location eyePos = bukkitPlayer.getEyeLocation();
        Vector eyeDirection = bukkitPlayer.getEyeLocation().getDirection();
        Predicate<org.bukkit.entity.Entity> filter = entity -> !entity.equals(bukkitPlayer);
        double reach = this.playerConnection.getPlayer().getGameMode() == GameMode.CREATIVE ? 4.5 : 3;

        RayTraceResult trace = world.rayTrace(eyePos, eyeDirection, reach, FluidCollisionMode.NEVER, true, 0, filter);
        if (trace == null) return null;
        Entity hitEntity = trace.getHitEntity();
        if (hitEntity instanceof LivingEntity)
            return (LivingEntity) hitEntity;
        else return null;
    }

    public void attack(Entity entity) {
        this.attack(((CraftEntity) entity).getHandle());
    }

    public void remove(String reason) {
        this.dropInventory();
        this.playerConnection.disconnect(reason);
        dummyNames.remove(this.getName());
        dummies.remove(this);
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        this.server.a(new TickTask(this.server.ai(), () -> remove("Died")));
    }

}
