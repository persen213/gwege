package dev.toaru.blackhole.net;

import dev.toaru.blackhole.BlackHoleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The add-on's network channel. Both sides must run the same protocol version. */
public final class Net {
    private Net() { }

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BlackHoleMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, FxStartMsg.class, FxStartMsg::encode, FxStartMsg::decode, FxStartMsg::handle);
        CHANNEL.registerMessage(id++, FxCancelMsg.class, FxCancelMsg::encode, FxCancelMsg::decode, FxCancelMsg::handle);
    }
}
