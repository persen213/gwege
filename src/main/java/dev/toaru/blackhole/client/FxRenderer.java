package dev.toaru.blackhole.client;

import dev.toaru.blackhole.BlackHoleMod;
import dev.toaru.blackhole.client.ClientFx.Effect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Draws the black hole as real 3D geometry inside the world: a pitch-black sphere, a glowing photon
 * ring that always faces the viewer, a rotating tilted accretion disk, spiralling infall streams and expanding gravitational
 * wave rings; then, on collapse, an implosion flash and an expanding shockwave shell with rings. Two passes: first the dark
 * surfaces, then everything that glows, so the glow is correctly hidden behind the hole.
 * Cost control: at most 8 effects exist, anything beyond 260 blocks is skipped, and segment counts drop with distance.
 */
@Mod.EventBusSubscriber(modid = BlackHoleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxRenderer {
    private FxRenderer() { }

    private static final double MAX_DISTANCE = 260.0;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ClientFx.ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = mc.isPaused() ? 0f : event.getPartialTick();
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f pose = event.getPoseStack().last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        Geo dark = new Geo(buffers.getBuffer(BhRenderTypes.DARK), pose, cam);
        for (Effect fx : ClientFx.ACTIVE) {
            if (fx.pos.distanceTo(cam) <= MAX_DISTANCE) drawDark(fx, fx.age(now, partial), dark, cam);
        }
        buffers.endBatch(BhRenderTypes.DARK);

        Geo glow = new Geo(buffers.getBuffer(RenderType.lightning()), pose, cam);
        for (Effect fx : ClientFx.ACTIVE) {
            if (fx.pos.distanceTo(cam) <= MAX_DISTANCE) drawGlow(fx, fx.age(now, partial), glow, cam);
        }
        buffers.endBatch(RenderType.lightning());
    }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static double mix(double a, double b, double t) { return a + (b - a) * t; }

    private static int segments(Effect fx, Vec3 cam) {
        double dist = fx.pos.distanceTo(cam);
        return dist < 50 ? 56 : (dist < 120 ? 36 : 24);
    }

    /** Current event-horizon radius: grows in during the formation, then swells slightly while active. */
    private static double horizon(Effect fx, double age) {
        double form = clamp01(age / Math.max(1.0, fx.aux.z));
        double ease = form * form * (3.0 - 2.0 * form);
        double grow = 0.88 + 0.12 * clamp01(age / Math.max(1, fx.duration));
        return fx.aux.y * ease * grow;
    }

    private static void drawDark(Effect fx, double age, Geo g, Vec3 cam) {
        int seg = segments(fx, cam);
        switch (fx.type) {
            case BLACK_HOLE -> {
                double rh = horizon(fx, age);
                if (rh > 0.02) {
                    g.color(0f, 0f, 0f, 0.26f).sphere(fx.pos, rh * 1.35, 8, seg / 2);
                    g.color(0f, 0f, 0f, 0.985f).sphere(fx.pos, rh, 12, seg / 2);
                }
            }
            case BLACK_HOLE_BLAST -> {
                double t = age / Math.max(1, fx.duration);
                if (t < 0.22) { // the core collapses to nothing
                    double s = 1.0 - t / 0.22;
                    double rh = fx.aux.y * s * s;
                    if (rh > 0.02) g.color(0f, 0f, 0f, 0.985f).sphere(fx.pos, rh, 12, seg / 2);
                }
            }
        }
    }

    private static void drawGlow(Effect fx, double age, Geo g, Vec3 cam) {
        int seg = segments(fx, cam);
        switch (fx.type) {
            case BLACK_HOLE -> blackHoleGlow(fx, age, g, cam, seg);
            case BLACK_HOLE_BLAST -> blastGlow(fx, age, g, cam, seg);
        }
    }

    private static void blackHoleGlow(Effect fx, double age, Geo g, Vec3 cam, int seg) {
        double form = clamp01(age / Math.max(1.0, fx.aux.z));
        double ease = form * form * (3.0 - 2.0 * form);
        double rh = horizon(fx, age);
        if (rh < 0.02) return;
        Vec3 c = fx.pos;
        double pullR = fx.aux.x;
        Vec3 toCam = cam.subtract(c);
        toCam = toCam.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : toCam.normalize();

        // Photon ring and lens halos: always turned toward the viewer, like gravitational lensing.
        g.color(1f, 0.96f, 0.82f, (float) (0.95 * ease)).ring(c, toCam, rh * 1.07, rh * 0.035, seg, 0, false);
        g.color(1f, 0.70f, 0.30f, (float) (0.30 * ease)).ring(c, toCam, rh * 1.28, rh * 0.13, seg, 0, false);
        g.color(0.6f, 0.8f, 1f, (float) (0.22 * ease)).ring(c, toCam, rh * 2.1, rh * 0.03, seg, 0, false);

        // Accretion disk: tilted concentric rings, dashed, rotating faster the closer they are (Keplerian).
        Vec3 diskNormal = new Vec3(Math.sin(0.35) * Math.cos(0.6), Math.cos(0.35), Math.sin(0.35) * Math.sin(0.6)).normalize();
        int rings = seg >= 56 ? 16 : (seg >= 36 ? 12 : 8);
        for (int k = 0; k < rings; k++) {
            double u = k / (double) (rings - 1);
            double radius = rh * (1.5 + 3.5 * u);
            double omega = 0.2 / Math.pow(1.0 + 3.0 * u, 1.5);
            g.color(1f, (float) mix(0.96, 0.42, u), (float) mix(0.85, 0.08, u), (float) ((0.9 - 0.6 * u) * ease))
                    .ring(c, diskNormal, radius, rh * (0.05 + 0.05 * u), seg, age * omega + k * 0.7, true);
        }

        // Infall streams: thin geometric lines spiralling inward from the outer region.
        int arms = seg >= 56 ? 10 : (seg >= 36 ? 7 : 5);
        int steps = 20;
        for (int arm = 0; arm < arms; arm++) {
            double base = arm * Math.PI * 2.0 / arms;
            Vec3 prev = null;
            for (int j = 0; j <= steps; j++) {
                double s = j / (double) steps;
                double rad = rh * 1.2 + (pullR * 0.6 - rh * 1.2) * Math.pow(1.0 - s, 1.6);
                double ang = base + age * 0.04 + s * 5.0;
                double h = Math.sin(base * 3.0 + s * 3.0) * (1.0 - s) * pullR * 0.10;
                Vec3 pt = c.add(Math.cos(ang) * rad, h, Math.sin(ang) * rad);
                if (prev != null) {
                    g.color(1f, (float) mix(0.55, 0.95, s), (float) mix(0.15, 0.8, s), (float) (0.5 * ease * (0.25 + 0.75 * s)));
                    g.ribbon(prev, pt, 0.03 + 0.04 * s);
                }
                prev = pt;
            }
        }

        // Gravitational waves: faint rings sweeping outward across the whole pull radius.
        for (int w = 0; w < 3; w++) {
            double ph = ((age * 0.9 + w * 40.0) % 120.0) / 120.0;
            g.color(0.55f, 0.65f, 1f, (float) ((1.0 - ph) * 0.30 * ease))
                    .ring(c, new Vec3(0, 1, 0), ph * pullR, 0.06 + 0.12 * ph, seg, 0, false);
        }
    }

    private static void blastGlow(Effect fx, double age, Geo g, Vec3 cam, int seg) {
        double t = clamp01(age / Math.max(1, fx.duration));
        double radius = fx.aux.x;
        Vec3 c = fx.pos;
        double e = 1.0 - Math.pow(1.0 - t, 3.0);
        double shell = Math.max(0.01, radius * e * 0.95);
        float fade = (float) (1.0 - t);

        if (t < 0.3) { // flash at the instant of collapse
            double s = t / 0.3;
            g.color(1f, 0.95f, 0.85f, (float) ((1.0 - s) * 0.9)).sphere(c, fx.aux.y * (0.4 + 1.6 * s), 8, 16);
        }
        g.color(0.7f, 0.82f, 1f, 0.16f * fade).sphere(c, shell, 12, seg / 2);

        double band = 0.15 + 0.45 * fade;
        Vec3 toCam = cam.subtract(c);
        toCam = toCam.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : toCam.normalize();
        g.color(1f, 0.85f, 0.6f, 0.8f * fade).ring(c, new Vec3(0, 1, 0), shell, band, seg, 0, false);
        g.color(0.7f, 0.85f, 1f, 0.6f * fade).ring(c, new Vec3(1, 0, 0), shell, band * 0.7, seg, 0, false);
        g.color(0.7f, 0.85f, 1f, 0.6f * fade).ring(c, new Vec3(0, 0, 1), shell, band * 0.7, seg, 0, false);
        g.color(1f, 0.95f, 0.85f, 0.7f * fade).ring(c, toCam, shell * 0.8, band * 0.5, seg, 0, false);
    }
}
