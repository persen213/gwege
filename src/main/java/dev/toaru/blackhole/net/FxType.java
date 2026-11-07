package dev.toaru.blackhole.net;

/**
 * Every client-side visual effect. The server only says "start this effect here, for this long"; the client draws it as
 * real 3D geometry (see the client package). Nothing here spawns anything in the world.
 */
public enum FxType {
    /** pos = centre, aux = (pull radius, event horizon radius, formation ticks), duration = formation + active ticks. */
    BLACK_HOLE,
    /** pos = centre, aux = (blast radius, event horizon radius, 0). The implosion and the shockwave. */
    BLACK_HOLE_BLAST
}
