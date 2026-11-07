package dev.toaru.blackhole.client;

import dev.toaru.blackhole.net.FxStartMsg;
import dev.toaru.blackhole.net.FxType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The client's list of running visual effects. An effect is plain data (what, where, since when, for how long); it is drawn
 * by {@link FxRenderer} and removed automatically when its time is up. The list is capped so effects can never pile up.
 */
public final class ClientFx {
    private ClientFx() { }

    private static final int MAX_EFFECTS = 8;

    public static final class Effect {
        public final long key;
        public final FxType type;
        public final long startTick;
        public final int duration;
        public final Vec3 pos, aux;
        public final int a, b, c;
        public final float power;

        Effect(FxStartMsg m, long startTick) {
            this.key = m.key; this.type = m.type; this.startTick = startTick; this.duration = m.duration;
            this.pos = new Vec3(m.x, m.y, m.z); this.aux = new Vec3(m.ax, m.ay, m.az);
            this.a = m.a; this.b = m.b; this.c = m.c; this.power = m.power;
        }

        /** Ticks since the effect started, including the fraction of the current tick. */
        public double age(long now, float partial) { return (now - startTick) + partial; }
    }

    static final List<Effect> ACTIVE = new ArrayList<>();

    static void start(FxStartMsg msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ACTIVE.add(new Effect(msg, mc.level.getGameTime()));
        while (ACTIVE.size() > MAX_EFFECTS) ACTIVE.remove(0);
    }

    static void cancel(long key) { ACTIVE.removeIf(e -> e.key == key); }

    static void clear() { ACTIVE.clear(); }

    /** Called every client tick: drops finished effects. */
    static void tick() {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(e -> now - e.startTick > e.duration + 2);
    }

    /**
     * How close the given entity is to a running black hole's pull, 0 (outside) to 1 (at the centre). Used for the screen
     * darkening and the narrowing field of view.
     */
    static double blackHoleProximity(Entity player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return 0.0;
        long now = mc.level.getGameTime();
        double best = 0.0;
        for (Effect e : ACTIVE) {
            if (e.type != FxType.BLACK_HOLE) continue;
            double age = e.age(now, 0f);
            double ramp = Math.min(1.0, age / Math.max(1.0, e.aux.z));
            double d = player.position().distanceTo(e.pos);
            double prox = Math.pow(Math.max(0.0, 1.0 - d / Math.max(1.0, e.aux.x)), 1.3) * ramp;
            if (prox > best) best = prox;
        }
        return best;
    }
}
