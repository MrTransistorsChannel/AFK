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
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class DummyPlayer extends ServerPlayer {
    private static double ARROW_DIST_COEFF = 0.16;
    private static double ARROW_HEIGHT_COEFF = 0.03;
    private static double SNOWBALL_DIST_COEFF = 0.45;
    private static double SNOWBALL_HEIGHT_COEFF = 0.1;
    private static double TRIDENT_DIST_COEFF = 0.26;
    private static double TRIDENT_HEIGHT_COEFF = 0.055;
    private LivingEntity target;

    public static ArrayList<DummyPlayer> dummies = new ArrayList<>();
    public static ArrayList<String> names = new ArrayList<>();
    public String locale = "en_us"; // Needed to prevent plugins using aikar`s ACF library from throwing NoSuchFieldException
    public boolean isFullyConnected = false;
    private boolean isSelfDefending = false;
    private boolean isAttackingContinuous = false;
    private boolean isForcePoI = false;
    private boolean aware = false;
    private boolean isTicking = true;
    private Location PoI_loc;
    private final float[] PoI_yawpitch = new float[2];

    private String[] getSkin(Player spawner) {
        if (spawner != null) {
            GameProfile gameProfile = ((CraftPlayer) spawner).getHandle().getGameProfile();
            if (!gameProfile.getProperties().containsKey("textures")) return null;
            Property property = gameProfile.getProperties().get("textures").iterator().next();
            String texture = property.getValue();
            String signature = property.getSignature();
            return new String[]{texture, signature};
        } else {
            ConfigurationSection botsToReload = ((AFK) server.server.getPluginManager().getPlugin("AFK")).getBotSaveYml()
                    .getConfigurationSection("botsToReload");
            if (botsToReload.getKeys(false).contains(getScoreboardName())) {
                String texture = botsToReload.getConfigurationSection(getScoreboardName()).getString("texture");
                String signature = botsToReload.getConfigurationSection(getScoreboardName()).getString("signature");
                if (texture != null && signature != null)
                    return new String[]{texture, signature};
            }
            return null;
        }
    }

    public DummyPlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile);
        dummies.add(this);
        names.add(getScoreboardName());
    }

    public static DummyPlayer spawnBot(String name, Location location, org.bukkit.entity.Player spawner, boolean updateStats) { // some of this crap is redundant but it works
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel world = (location != null && location.getWorld() != null) ? ((CraftWorld) location.getWorld()).getHandle()
                : server.getLevel(Level.OVERWORLD);
        GameProfile gameProfile = server.getProfileCache().get(name).orElse(new GameProfile(UUID.randomUUID(), name)); // maybe needs something else?

        DummyPlayer dummy = new DummyPlayer(server, world, gameProfile);

        String[] texSign;// maybe simplify this?
        texSign = dummy.getSkin(spawner);

        if (texSign != null)
            gameProfile.getProperties().put("textures", new Property("textures", texSign[0], texSign[1]));

        /* spawning entity */
        server.getPlayerList().placeNewPlayer(new DummyConnection(), dummy);

        new BukkitRunnable() {
            @Override
            public void run() { // ensuring that the bot has been connected and started ticking
                if (!dummy.isFullyConnected) {
                    dummy.connection.tick();
                    return;
                } else if (location != null && location.getWorld() != null)
                    dummy.teleportTo(world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                this.cancel();
            }
        }.runTaskTimer(server.server.getPluginManager().getPlugin("AFK"), 0, 10);

        if (updateStats) {
            dummy.setHealth(20.0F);
            dummy.unsetRemoved();
            dummy.setGameMode(GameType.SURVIVAL);
        }
        dummy.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0xf7);
        return dummy;
    }

    @Override
    public void tick() { // still can`t figure out how to process knockback from players
        isFullyConnected = true;
        if (!isTicking) return;
        super.tick(); // handle gamemode and inventory
        doTick(); // update health, air and other levels

        if (getServer().getTickCount() % 10 == 0) { // update position in chunks
            connection.resetPosition(); // maybe not necessary?
            getLevel().getChunkSource().move(this);
        }

        if (getServer().getTickCount() % 20 == 0) // prevents hunger
            getBukkitEntity().setFoodLevel(20);

        if (isForcePoI) goToPoI();
        selfDefence();
        if (isAttackingContinuous) attackContinuous();
        rangeAttackTarget();

        /*Location targetLoc = Bukkit.getServer().getPlayer("MrTransistor_").getLocation();
        Location loc = this.getBukkitEntity().getLocation();
        BlockPos targetPos = ((CraftBlock) targetLoc.getBlock()).getPosition();
        BlockPos pos = ((CraftBlock) loc.getBlock()).getPosition();
        Zombie zombie = new Zombie(this.getLevel());
        Vec3 vecPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        zombie.setPos(vecPos);
        zombie.setOnGround(true);

        PathFinder pathFinder = new PathFinder(new WalkNodeEvaluator(), 300);                                                                                                                                                                                                                                           
        PathNavigationRegion reg = new PathNavigationRegion(((CraftWorld) loc.getWorld()).getHandle(),
                pos.offset(-120, -120, -120), pos.offset(120, 120, 120));
        Path path = pathFinder.findPath(reg,
                zombie, ImmutableSet.of(targetPos), 120, 1, 1.f);
        path.advance();
        if(path.isDone()) return;
        BlockPos nextNode = path.getNextNodePos();
        System.out.println(nextNode);
        PoI_loc = loc;
        PoI_loc.setX(nextNode.getX()+0.5);
        PoI_loc.setY(nextNode.getY()+1.6);
        PoI_loc.setZ(nextNode.getZ()+0.5);*/

    }

    public void attackOnce() {
        CraftLivingEntity target = getTargetedLivingEntity();
        if (getAttackStrengthScale(0.5f) == 1 && target != null) {
            attack(target);
        }
        swing(InteractionHand.MAIN_HAND);
    }

    public void shootNearestLivingEntity() {
        CraftPlayer bukkitEntity = getBukkitEntity();
        List<Entity> nearbyEntities = bukkitEntity.getNearbyEntities(50, 50, 50);
        Entity nearest = null;
        double nearestDistance = 0;

        for (Entity e : nearbyEntities) {
            if (!(e instanceof CraftLivingEntity)
                    || e instanceof Player || e instanceof Enderman || e.isDead()) continue;
            if ((nearest == null || e.getLocation().distanceSquared(getBukkitEntity().getLocation()) < nearestDistance)
                    && bukkitEntity.hasLineOfSight(e)) {
                nearest = e;
                nearestDistance = e.getLocation().distanceSquared(getBukkitEntity().getLocation());
            }
        }
        if (nearest != null) {
            target = (LivingEntity) ((CraftEntity) nearest).getHandle();
        }
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

    private void rangeAttackTarget() {
        if (target == null) return;

        lookAt(EntityAnchorArgument.Anchor.EYES, target, EntityAnchorArgument.Anchor.EYES);
        double x = getX() - target.getX();
        double y = getY() - target.getY();
        double z = getZ() - target.getZ();
        double r = Math.sqrt(x * x + z * z); // distance in X-Z plane

        ItemStack itemStackInHand = getItemInHand(InteractionHand.MAIN_HAND);
        Item itemInHand = itemStackInHand.getItem();

        float pitch = getXRot(); // correcting pitch for distance to target, totally simplified, but it has enough accuracy
        if (itemInHand instanceof BowItem || itemInHand instanceof CrossbowItem) {
            setXRot((float) (pitch - r * ARROW_DIST_COEFF + y * ARROW_HEIGHT_COEFF));
            if (itemInHand instanceof BowItem) {
                if (!isUsingItem()) {
                    itemInHand.use(getLevel(), this, InteractionHand.MAIN_HAND);
                }
                if (BowItem.getPowerForTime(getTicksUsingItem()) == 1.0) {
                    itemStackInHand.releaseUsing(getLevel(), this, 0);
                    stopUsingItem();
                    target = null;
                }
            } else {
                if (!isUsingItem() || CrossbowItem.isCharged(itemStackInHand)) {
                    if (CrossbowItem.isCharged(itemStackInHand))
                        target = null;
                    itemInHand.use(getLevel(), this, InteractionHand.MAIN_HAND);
                }
                if (getTicksUsingItem() >= CrossbowItem.getChargeDuration(itemStackInHand)) {
                    itemInHand.releaseUsing(itemStackInHand, getLevel(), this, 0);
                    stopUsingItem();
                }
            }
        } else if (itemInHand instanceof SnowballItem || itemInHand instanceof EggItem) {
            setXRot((float) (pitch - r * SNOWBALL_DIST_COEFF + y * SNOWBALL_HEIGHT_COEFF));
            itemInHand.use(getLevel(), this, InteractionHand.MAIN_HAND);
            target = null;
        } else if (itemInHand instanceof TridentItem) {
            setXRot((float) (pitch - r * TRIDENT_DIST_COEFF + y * TRIDENT_HEIGHT_COEFF));
            if (!isUsingItem())
                itemInHand.use(getLevel(), this, InteractionHand.MAIN_HAND);
            if ((float) getTicksUsingItem() >= 10 && !isAutoSpinAttack()) {
                itemInHand.releaseUsing(itemStackInHand, getLevel(), this, 0);
                target = null;
            }

        } else {
            setXRot(pitch);
            target = null;
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

    public void disconnect(String reason) {
        connection.disconnect(reason);
        dummies.remove(this);
        names.remove(getScoreboardName());
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        setExperienceLevels(0); // idk why we need this but without this bot does not clear xp on death
        setExperiencePoints(0);
        removeAllEffects();
        getBukkitEntity().setTotalExperience(0); // needed to simplify xp reward calculation
        getServer().execute(new TickTask(getServer().getTickCount(), () -> disconnect("Died")));
    }
}
