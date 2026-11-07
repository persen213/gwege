package dev.toaru.blackhole.client;

import dev.toaru.blackhole.BlackHoleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Screen effect: the closer you are to a running black hole, the more the edges of the screen go black (drawn as plain
 * rectangles and gradients, nothing else). The field-of-view narrowing that goes with it is in {@link ClientEvents}.
 */
@Mod.EventBusSubscriber(modid = BlackHoleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ScreenFx {
    private ScreenFx() { }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("blackhole_darkening", (gui, graphics, partialTick, width, height) -> render(graphics, width, height));
    }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        double prox = ClientFx.blackHoleProximity(mc.player);
        if (prox <= 0.01) return;

        int tint = (int) (prox * prox * 110.0);
        g.fill(0, 0, w, h, tint << 24);

        int alpha = (int) (Math.min(1.0, prox * 1.15) * 235.0);
        int band = (int) (Math.max(w, h) * 0.30 * (0.4 + 0.6 * prox));
        g.fillGradient(0, 0, w, band, alpha << 24, 0);          // top: solid black fading inward
        g.fillGradient(0, h - band, w, h, 0, alpha << 24);      // bottom
        int strips = 12;
        int strip = Math.max(1, band / strips);
        for (int i = 0; i < strips; i++) {                      // left and right, stepped fade
            int a = (int) (alpha * (1.0 - i / (double) strips));
            g.fill(i * strip, 0, (i + 1) * strip, h, a << 24);
            g.fill(w - (i + 1) * strip, 0, w - i * strip, h, a << 24);
        }
    }
}
