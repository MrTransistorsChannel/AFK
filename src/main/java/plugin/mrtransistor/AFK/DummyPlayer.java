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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class DummyPlayer extends ServerPlayer {
    public static ArrayList<DummyPlayer> dummies = new ArrayList<>();
    public static ArrayList<String> dummyNames = new ArrayList<>();

    private boolean isSelfDefending = false;
    private boolean isAttackingContinuous = false;
    private boolean isForcePoI = false;
    private boolean aware = false;
    private boolean isTicking = true;
    private Location PoI_loc;
    private final float[] PoI_yawpitch = new float[2];

    private static String[] getSkin(ServerPlayer player) {
        GameProfile gameProfile = player.getGameProfile();
        if (!gameProfile.getProperties().containsKey("textures")) return null;
        Property property = gameProfile.getProperties().get("textures").iterator().next();
        String texture = property.getValue();
        String signature = property.getSignature();
        return new String[]{texture, signature};
    }

    public DummyPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile);
        dummyNames.add(getName().getContents());
        dummies.add(this);
    }

    public static DummyPlayer spawnBot(String name, Location location, org.bukkit.entity.Player spawner) { // some of this crap is redundant but it works
        //TODO: check userCache for existing bots, update skin if spawner is not null (as it will be on server restart)
        MinecraftServer server = ((CraftServer) (Bukkit.getServer())).getServer();
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        DummyPlayer dummy = new DummyPlayer(server, world, gameProfile);

        String[] texSign = null;
        if (spawner != null)
            texSign = getSkin(((CraftPlayer) spawner).getHandle());

        if (texSign != null)
            gameProfile.getProperties().put("textures", new Property("textures", texSign[0], texSign[1]));

        server.getPlayerList().placeNewPlayer(new DummyConnection(), dummy);

        new BukkitRunnable() {

            @Override
            public void run() { // Fix for Paper`s post-chunk-load player join
                dummy.connection.tick();
            }
        }.runTaskLater(server.server.getPluginManager().getPlugin("AFK"), 2);

        dummy.teleportTo(world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        dummy.setHealth(20.0F);
        dummy.unsetRemoved();
        dummy.setGameMode(GameType.SURVIVAL);
        dummy.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0xf7);

        //TODO: move to AFK#onDisable();
        AFK _plg = (AFK) Bukkit.getServer().getPluginManager().getPlugin("AFK");
        FileConfiguration botSaveYml = _plg.getBotSaveFile();
        Map<String, Object> botCoords = new HashMap<>();
        if (spawner != null)
            botCoords.put("spawner", spawner.getUniqueId().toString());
        botCoords.put("level", dummy.getBukkitEntity().getLocation().getWorld().getName());
        botCoords.put("x", dummy.getBukkitEntity().getLocation().getX());
        botCoords.put("y", dummy.getBukkitEntity().getLocation().getY());
        botCoords.put("z", dummy.getBukkitEntity().getLocation().getZ());
        botCoords.put("yaw", dummy.getBukkitEntity().getLocation().getYaw());
        botCoords.put("pitch", dummy.getBukkitEntity().getLocation().getPitch());
        botSaveYml.createSection(dummy.getBukkitEntity().getName(), botCoords);
        _plg.saveBotSaveFile();
        return dummy;
    }

    @Override
    public void tick() { // some of this crap is not necessary but it works
        if (!isTicking) return;
        super.tick(); // handle gamemode and inventory
        doTick(); // update health, air and other levels

        if (getServer().getTickCount() % 10 == 0) { // update position in chunks
            connection.resetPosition(); // maybe not necessary?
            getLevel().getChunkSource().move(this);
        }

        if (isForcePoI) goToPoI();
        selfDefence();
        if (isAttackingContinuous) attackContinuous();

        /*if (getServer().getTickCount() % 100 == 0) {
            CraftPlayer bukkitEntity = getBukkitEntity();
            if (bukkitEntity.getItemInHand().getType().equals(Material.BOW)) {
                startUsingItem(InteractionHand.MAIN_HAND);
                bukkitEntity.launchProjectile(Arrow.class);
            } else if (bukkitEntity.getItemInHand().getType().equals(Material.SNOWBALL)) {
                startUsingItem(InteractionHand.MAIN_HAND);
                bukkitEntity.launchProjectile(Snowball.class);
            }
        }
        System.out.println(getUseItemRemainingTicks());*/

    }

    public void attackOnce() {
        CraftLivingEntity target = getTargetedLivingEntity();
        if (getAttackStrengthScale(0.5f) == 1 && target != null) {
            attack(target);
        }
        swing(InteractionHand.MAIN_HAND);
    }

    public void setTicking(boolean state) {
        isTicking = state;
    }

    public boolean isTicking() {
        return isTicking;
    }

    public void setAttackingContinuous(boolean state) {
        isAttackingContinuous = state;
    }

    public boolean isAttackingContinuous() {
        return isAttackingContinuous;
    }

    public void setSelfDefending(boolean state) {
        isSelfDefending = state;
    }

    public boolean isSelfDefending() {
        return isSelfDefending;
    }

    public void setForcePoI(boolean state) {
        isForcePoI = state;
        if (isForcePoI) {
            PoI_loc = getBukkitEntity().getEyeLocation();
            PoI_yawpitch[0] = PoI_loc.getYaw();
            PoI_yawpitch[1] = PoI_loc.getPitch();
        }

    }

    public boolean isForcePoI() {
        return isForcePoI;
    }

    private void attackContinuous() {
        if (!aware) {
            CraftLivingEntity target = getTargetedLivingEntity();
            if (getAttackStrengthScale(0.5f) == 1 && target != null) {
                attack(target);
                swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private void selfDefence() {
        LivingEntity damager = getLastHurtByMob();

        if (damager == null || !isSelfDefending) {
            if (aware) {
                Vector toTarget = getBukkitEntity().getEyeLocation().toVector().subtract(PoI_loc.toVector()).normalize();
                float yaw = (float) Math.atan2(toTarget.getX(), -toTarget.getZ());
                float pitch = (float) Math.atan2(toTarget.getY(), Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ()));
                yaw *= 180 / Math.PI;
                pitch *= 180 / Math.PI;
                setRot(yaw, pitch);
                setYHeadRot(yaw);
                travel(new Vec3(0, 0, 1));
                if (PoI_loc.distance(getBukkitEntity().getEyeLocation()) < 0.1) {
                    getBukkitEntity().setVelocity(new Vector(0, 0, 0));
                    setRot(PoI_yawpitch[0], PoI_yawpitch[1]);
                    setYHeadRot(PoI_yawpitch[0]);
                    aware = false;
                }
            }
            return;
        }
        if (!aware) {
            if (!isForcePoI) {
                PoI_loc = getBukkitEntity().getEyeLocation();
                PoI_yawpitch[0] = PoI_loc.getYaw();
                PoI_yawpitch[1] = PoI_loc.getPitch();
            }
            aware = true;
        }

        Vector toTarget = getBukkitEntity().getEyeLocation().toVector().subtract(((CraftLivingEntity) damager.getBukkitEntity()).getEyeLocation().toVector()).normalize();
        float yaw = (float) Math.atan2(toTarget.getX(), -toTarget.getZ());
        float pitch = (float) Math.atan2(toTarget.getY(), Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ()));
        yaw *= 180 / Math.PI;
        pitch *= 180 / Math.PI;
        setRot(yaw, pitch);
        setYHeadRot(yaw);
        travel(new Vec3(0, 0, 1));

        CraftLivingEntity target = getTargetedLivingEntity();
        if (getAttackStrengthScale(0.5f) == 1 && target != null && target.getUniqueId().equals(damager.getUUID())) {
            attack(target);
            swing(InteractionHand.MAIN_HAND);
        }
    }

    private void goToPoI() {
        if (PoI_loc.distance(getBukkitEntity().getEyeLocation()) > 0.1) {
            aware = true;
        }
    }

    public CraftLivingEntity getTargetedLivingEntity() {
        Player bukkitPlayer = getBukkitEntity();
        World world = bukkitPlayer.getWorld();
        Location eyePos = bukkitPlayer.getEyeLocation();
        Vector eyeDirection = bukkitPlayer.getEyeLocation().getDirection();
        Predicate<Entity> filter = entity -> !entity.equals(bukkitPlayer) && entity instanceof CraftLivingEntity;
        double reach = getBukkitEntity().getGameMode() == GameMode.CREATIVE ? 4.5 : 3;

        RayTraceResult trace = world.rayTrace(eyePos, eyeDirection, reach, FluidCollisionMode.NEVER, true, 0, filter);
        if (trace == null) return null;
        Entity hitEntity = trace.getHitEntity();
        if (hitEntity instanceof CraftLivingEntity)
            return (CraftLivingEntity) hitEntity;
        else return null;
    }

    public void attack(Entity entity) {
        attack(((CraftEntity) entity).getHandle());
    }

    public void remove(String reason) {
        dropEquipment();
        CraftPlayer bEntity = getBukkitEntity();
        World xpWorld = bEntity.getWorld();
        Location xpLoc = bEntity.getLocation();
        int xp = bEntity.getTotalExperience();
        connection.disconnect(reason);
        xpWorld.spawn(xpLoc, ExperienceOrb.class).setExperience(xp);
        dummyNames.remove(getName().getContents());
        dummies.remove(this);
        AFK _plg = (AFK) Bukkit.getServer().getPluginManager().getPlugin("AFK");
        FileConfiguration botSaveYml = _plg.getBotSaveFile();
        botSaveYml.set(bEntity.getName(), null);
        _plg.saveBotSaveFile();
    }

    public void softRemove() {
        connection.disconnect("soft removed");
        dummyNames.remove(getName().getContents());
        dummies.remove(this);
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        getServer().execute(new TickTask(getServer().getTickCount(), () -> remove("Died")));
    }
}
