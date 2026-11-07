package dev.toaru.blackhole.client;

import dev.toaru.blackhole.net.FxStartMsg;

/** Client-side entry points for the network messages. Only ever loaded on a client. */
public final class ClientNet {
    private ClientNet() { }

    public static void start(FxStartMsg msg) { ClientFx.start(msg); }

    public static void cancel(long key) { ClientFx.cancel(key); }
}
