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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
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
        //### ДОБАВИТЬ ПРОВЕРКУ ОТСУТСТВИЯ СКИНА
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

    public static DummyPlayer spawnBot(String name, Location location, org.bukkit.entity.Player spawner) {
        MinecraftServer server = ((CraftServer) (Bukkit.getServer())).getServer();
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        Connection conn = new DummyConnection();

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        DummyPlayer dummy = new DummyPlayer(server, world, gameProfile);

        String[] texSign = getSkin(((CraftPlayer) spawner).getHandle());

        if (texSign != null)
            gameProfile.getProperties().put("textures", new Property("textures", texSign[0], texSign[1]));

        dummy.getEntityData().set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 0x7f); // show all model layers

        dummy.connection = new ServerGamePacketListenerImpl(server, conn, dummy);
        world.addNewPlayer(dummy);

        dummy.forceSetPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        dummy.setYHeadRot(location.getYaw());
        world.getChunkSource().move(dummy);

        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, dummy));
        server.getPlayerList().broadcastAll(new ClientboundAddPlayerPacket(dummy));
        server.getPlayerList().broadcastAll(new ClientboundSetEntityDataPacket(dummy.getId(), dummy.getEntityData(), true));
        return dummy;
    }

    @Override
    public void tick() {
        if (!isTicking) return;
        super.tick();
        if (getServer().getTickCount() % 10 == 0) {
            connection.resetPosition();
            getLevel().getChunkSource().move(this);
        }
        doTick();

        if (isForcePoI) goToPoI();
        selfDefence();
        if (isAttackingContinuous) attackContinuous();
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
        dummyNames.remove(getName().getContents());
        dummies.remove(this);
        xpWorld.spawn(xpLoc, ExperienceOrb.class).setExperience(xp);
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        getServer().execute(new TickTask(getServer().getTickCount(), () -> remove("Died")));
    }
}
