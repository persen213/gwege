package dev.toaru.blackhole;

import com.mojang.logging.LogUtils;
import dev.toaru.blackhole.net.Net;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Black Hole: a one-skill category ("Singularity") for Re:AcademyCraft 1.5.0, registered through the public API only.
 * The skill is level 5 because its cost (7000 CP by default) is only affordable at that level (about 12000 CP).
 */
@Mod(BlackHoleMod.MOD_ID)
public final class BlackHoleMod {
    public static final String MOD_ID = "blackhole";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    /** The item that gives the Singularity category (used in the Ability Developer like the built-in factors). */
    public static final RegistryObject<Item> SINGULARITY_FACTOR = ITEMS.register("singularity_factor", () -> new Item(new Item.Properties().stacksTo(16)));

    public static AbilityId id(String path) { return AbilityId.parse(MOD_ID + ":" + path); }
    public static final AbilityId CATEGORY = id("singularity"), BLACK_HOLE = id("black_hole");

    public BlackHoleMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Cfg.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) { event.enqueueWork(Net::register); }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(SINGULARITY_FACTOR);
    }

    /** Re:AcademyCraft fires this once during common setup; registration is only possible here. */
    private void registerAbilities(RegisterAbilitiesEvent event) {
        var abilities = event.registrar(MOD_ID);
        // The only skill sits at level 5, so levels 1-4 have no skills of their own.
        abilities.category(new CategoryDefinition(CATEGORY, id("singularity_factor"), List.of(0, 0, 0, 0, 1),
                new Presentation(MOD_ID + ".ability.singularity", null, null)));
        abilities.skill(new SkillDefinition(BLACK_HOLE, CATEGORY, 5, List.of(), ActivationKind.TOGGLE,
                new Presentation(MOD_ID + ".skill.black_hole", MOD_ID + ".skill.black_hole.desc", null),
                new TreePosition(110, 55, null)));
        event.behaviors(MOD_ID).behavior(BLACK_HOLE, new BlackHole());
    }

    /** Clean-up of the add-on's own state (Re:AcademyCraft already stops running sessions in these situations). */
    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static final class Cleanup {
        private Cleanup() { }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { BlackHole.forget(event.getEntity().getUUID()); }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) { BlackHole.clearAll(); }
    }
}
