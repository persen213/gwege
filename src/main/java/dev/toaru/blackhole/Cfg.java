package dev.toaru.blackhole;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/**
 * Every balance number of the ability. The file is created at config/blackhole-common.toml the first time the game (or
 * server) starts; edit it there and restart. Seconds are real seconds, ranges are blocks.
 */
public final class Cfg {
    private Cfg() { }
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    private static DoubleValue d(String name, double def, double min, double max) { return B.defineInRange(name, def, min, max); }

    public static final DoubleValue CAST_RANGE = d("cast_range", 40, 5, 128);
    public static final DoubleValue COOLDOWN = d("cooldown_seconds", 180, 0, 3600);
    /** Level 5 players have about 12000 CP and 500 overload by default. */
    public static final DoubleValue CP = d("cp_cost", 7000, 0, 100000);
    public static final DoubleValue OVERLOAD = d("overload_cost", 300, 0, 100000);
    public static final DoubleValue FORMATION_SECONDS = d("formation_seconds", 1.5, 0.5, 10);
    public static final DoubleValue ACTIVE_SECONDS = d("active_seconds", 8, 1, 60);

    public static final DoubleValue PULL_RADIUS = d("pull_radius", 24, 5, 64);
    /** Acceleration (blocks per tick, per tick) at full strength. */
    public static final DoubleValue PULL_STRENGTH = d("pull_strength", 0.16, 0.01, 1);
    public static final DoubleValue MAX_SPEED = d("max_pull_speed", 1.2, 0.2, 4);
    /** Sideways push that makes pulled entities spiral around the hole. */
    public static final DoubleValue SWIRL = d("swirl_strength", 0.04, 0, 0.5);
    public static final DoubleValue BOSS_PULL = d("boss_pull_fraction", 0.25, 0, 1);

    public static final DoubleValue HORIZON_RADIUS = d("event_horizon_radius", 3.2, 1, 10);
    /** Damage dealt to everything inside the event horizon, 4 times a second. */
    public static final DoubleValue HORIZON_DAMAGE = d("event_horizon_damage_per_pulse", 10, 0, 500);
    public static final DoubleValue BOSS_DAMAGE = d("boss_damage_fraction", 0.35, 0, 1);

    public static final DoubleValue BLAST_RADIUS = d("collapse_blast_radius", 20, 3, 64);
    public static final DoubleValue BLAST_DAMAGE = d("collapse_blast_damage", 18, 0, 500);
    public static final DoubleValue BLAST_KNOCKBACK = d("collapse_blast_knockback", 2.4, 0, 8);
    public static final DoubleValue BOSS_KNOCKBACK = d("boss_knockback_fraction", 0.3, 0, 1);

    public static final ForgeConfigSpec SPEC = B.build();

    public static int ticks(DoubleValue seconds) { return (int) Math.round(seconds.get() * 20.0); }
}
