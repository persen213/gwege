package dev.toaru.blackhole;

import dev.toaru.blackhole.net.Fx;
import dev.toaru.blackhole.net.FxType;
import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Black Hole (ultimate, one activation, toggle session):
 * 1. Formation (1.5 s): the hole opens where you aim (up to 40 blocks); the pull ramps up from nothing.
 * 2. Active (8 s): every valid enemy within 24 blocks is dragged toward the centre, spiralling inward. Anything inside the
 *    event horizon is damaged 4 times a second. Arrows and other projectiles are dragged in and destroyed at the horizon.
 * 3. Collapse: when the time is up, or when you press the key again, the hole implodes and releases a shockwave (damage with
 *    falloff and an outward throw, both capped).
 * Only things the server's PvP / team / protection rules allow you to attack are ever pulled or hurt (re-checked a few times a
 * second); you and your allies are never affected. Bosses are dragged and hurt much less. No blocks are changed. Death,
 * logout or a dimension change end it without the shockwave.
 */
public final class BlackHole implements SkillBehavior {
    private static final class State {
        Vec3 centre;
        long fxKey;
        final Set<UUID> affected = new HashSet<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static int formationTicks() { return Cfg.ticks(Cfg.FORMATION_SECONDS); }
    private static int totalTicks() { return formationTicks() + Cfg.ticks(Cfg.ACTIVE_SECONDS); }

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        // Hover a few blocks above the aimed spot so the sphere is not buried in the ground.
        Vec3 centre = Targeting.point(p, Cfg.CAST_RANGE.get()).add(0, 4.0, 0);
        if (!c.pay(Cfg.CP.get().floatValue(), Cfg.OVERLOAD.get().floatValue())) return ActivationOutcome.REFUSED_RESOURCE;

        ServerLevel level = p.serverLevel();
        State s = new State();
        s.centre = centre;
        s.fxKey = Fx.make(level, centre, FxType.BLACK_HOLE, totalTicks()).range(220.0)
                .aux(new Vec3(Cfg.PULL_RADIUS.get(), Cfg.HORIZON_RADIUS.get(), formationTicks())).send();
        STATES.put(p.getUUID(), s);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, net.minecraft.sounds.SoundSource.PLAYERS, 4.0f, 0.4f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        State s = p == null ? null : STATES.get(p.getUUID());
        int age = c.age();
        if (p == null || s == null || age >= totalTicks()) return false;
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        Vec3 centre = s.centre;
        double pullR = Cfg.PULL_RADIUS.get();
        double horizon = Cfg.HORIZON_RADIUS.get() * (0.88 + 0.12 * Math.min(1.0, age / (double) totalTicks()));
        double ramp = Math.min(1.0, age / (double) Math.max(1, formationTicks()));
        boolean pulse = age % 5 == 0 && ramp >= 0.6;

        if (age % 5 == 0) { // permission checks a few times a second, not every tick
            s.affected.clear();
            for (LivingEntity e : Targeting.livingWithin(level, centre, pullR * 1.05, p))
                if (Targeting.hostile(actor, e, BlackHoleMod.BLACK_HOLE)) s.affected.add(e.getUUID());
        }

        int hits = 0;
        for (UUID id : s.affected) {
            Entity found = level.getEntity(id);
            if (!(found instanceof LivingEntity le) || !le.isAlive()) continue;
            Vec3 to = centre.subtract(Targeting.centre(le));
            double d = to.length();
            if (d < 0.01 || d > pullR * 1.3) continue;
            Vec3 dir = to.scale(1.0 / d);
            double strength = Targeting.bossLike(le) ? Cfg.BOSS_PULL.get() : Targeting.controlFactor(le);
            double closeness = Math.max(0.0, 1.0 - d / pullR);
            double accel = Cfg.PULL_STRENGTH.get() * ramp * strength * (0.25 + 0.75 * Math.pow(closeness, 1.5));
            Vec3 swirl = new Vec3(-dir.z, 0, dir.x).scale(Cfg.SWIRL.get() * ramp * strength * closeness);
            Vec3 v = le.getDeltaMovement().scale(0.96).add(dir.scale(accel)).add(swirl);
            Targeting.setMotion(le, Targeting.capLength(v, Cfg.MAX_SPEED.get()));

            if (pulse && d <= horizon + le.getBbWidth() * 0.5) { // inside the event horizon
                double dmg = Cfg.HORIZON_DAMAGE.get() * (Targeting.bossLike(le) ? Cfg.BOSS_DAMAGE.get() : 1.0);
                if (AcademySafety.attack(actor, le, BlackHoleMod.BLACK_HOLE, (float) dmg)) hits++;
            }
        }

        // Projectiles are dragged in harder and consumed at the event horizon.
        for (Projectile pr : Targeting.projectilesWithin(level, centre, pullR, p)) {
            Vec3 to = centre.subtract(pr.position());
            double d = to.length();
            if (d <= horizon) { pr.discard(); continue; }
            Vec3 v = pr.getDeltaMovement().scale(0.9).add(to.scale(1.0 / d).scale(0.5 * ramp));
            Targeting.setMotion(pr, Targeting.capLength(v, 2.5));
        }

        if (hits > 0) c.addProficiency(0.003f * Math.min(hits, 4));
        if (age % 30 == 0) {
            level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BEACON_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 3.0f, 0.35f);
        }
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        State s = p == null ? null : STATES.remove(p.getUUID());
        if (p != null && s != null) {
            ServerLevel level = p.serverLevel();
            Fx.cancel(level, s.fxKey);
            if (reason == StopReason.FINISHED || reason == StopReason.SWITCHED_OFF) collapse(c, p, s, level);
        }
        c.setCooldown(Cfg.ticks(Cfg.COOLDOWN));
    }

    /** The implosion and shockwave: capped damage with falloff, and an outward throw (bosses much less). */
    private static void collapse(ActivationContext c, ServerPlayer p, State s, ServerLevel level) {
        var actor = c.actor();
        double radius = Cfg.BLAST_RADIUS.get();
        int hits = 0;
        for (LivingEntity e : Targeting.livingWithin(level, s.centre, radius, p)) {
            if (!Targeting.hostile(actor, e, BlackHoleMod.BLACK_HOLE)) continue;
            double d = Targeting.centre(e).distanceTo(s.centre);
            double falloff = 1.0 - 0.7 * Math.min(1.0, d / radius);
            double bossMul = Targeting.bossLike(e) ? Cfg.BOSS_DAMAGE.get() : 1.0;
            if (!AcademySafety.attack(actor, e, BlackHoleMod.BLACK_HOLE, (float) (Cfg.BLAST_DAMAGE.get() * falloff * bossMul))) continue;
            Vec3 away = Targeting.centre(e).subtract(s.centre);
            away = away.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : away.normalize();
            double k = Cfg.BLAST_KNOCKBACK.get() * falloff * (Targeting.bossLike(e) ? Cfg.BOSS_KNOCKBACK.get() : Targeting.controlFactor(e));
            Targeting.setMotion(e, new Vec3(away.x * k, 0.3 + away.y * k * 0.6, away.z * k));
            hits++;
        }
        Fx.make(level, s.centre, FxType.BLACK_HOLE_BLAST, 26).range(220.0)
                .aux(new Vec3(radius, Cfg.HORIZON_RADIUS.get(), 0)).send();
        level.playSound(null, s.centre.x, s.centre.y, s.centre.z, SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 6.0f, 0.4f);
        if (hits > 0) c.addProficiency(0.01f * Math.min(hits, 5));
    }

    public static void forget(UUID player) { STATES.remove(player); }
    public static void clearAll() { STATES.clear(); }
}
