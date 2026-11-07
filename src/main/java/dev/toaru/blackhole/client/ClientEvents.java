package dev.toaru.blackhole.client;

import dev.toaru.blackhole.BlackHoleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only Forge events: effect upkeep and the field-of-view squeeze. */
@Mod.EventBusSubscriber(modid = BlackHoleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ClientFx.tick();
    }

    /** Near a black hole the view narrows, as if space itself were being squeezed. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        double prox = ClientFx.blackHoleProximity(event.getPlayer());
        if (prox > 0.0) event.setNewFovModifier(event.getNewFovModifier() * (float) (1.0 - 0.3 * prox));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { ClientFx.clear(); }
}
