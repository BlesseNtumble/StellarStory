package stellarstory;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.server.ServerLifecycleEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stellarstory.api.celestial.Star;
import stellarstory.api.celestial.StarSystem;
import stellarstory.common.data.StellarBlockstates;
import stellarstory.common.data.StellarItemmodels;
import stellarstory.common.registers.StellarDimensions;
import stellarstory.network.NetworkHandler;

import java.util.Locale;

@Mod(StellarStory.MODID)
public class StellarStory {
    public static final String MODID = "stellarstory";
    private static final Logger LOGGER = LogManager.getLogger();

    private static final boolean isDebug = true;


    public StellarStory() {

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus modEventBus = MinecraftForge.EVENT_BUS;

        StellarRegistries.ITEMS.register(eventBus);
        StellarRegistries.BLOCKS.register(eventBus);
        StellarRegistries.DIMENSION_TYPES.register(eventBus);
        StellarRegistries.CHUNK_GENERATORS.register(eventBus);

        StellarDimensions.register(eventBus);
        NetworkHandler.registerPackets();

        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::serverStarting);
        eventBus.addListener(this::gatherData);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(this::clientSetup));
        //StellarCelestial.registerCelestials(eventBus);


    }


    public void clientSetup(FMLClientSetupEvent event) {

    }

    public void serverSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            StellarDimensions.commonRegister();
        });
    }

    public void serverStarting(ServerLifecycleEvent event) {
        MinecraftServer server = event.getServer();
        ResourceManager manager = server.getResourceManager();
/*
        List<JsonElement> json = Utils.getAllJsonByRL(manager, "celestial_bodies/systems");
        addDebugMessage(json.size() + "");
        json.forEach(jsonElement -> {
            Codec<StarSystem> codec = StarSystem.CODEC;
            DataResult<Pair<StarSystem, JsonElement>> result = codec.decode(JsonOps.INSTANCE, jsonElement);
            result.result().ifPresent(data -> {
                addDebugMessage(data.getFirst().toString());
            });

        });
*/
    }

    public static Logger getLogger() {
        return LOGGER;
    }
    public static void addDebugMessage(String message) {
        if(!isDebug) return;
        getLogger().debug(String.format("[%s] DEBUG: %s", MODID, message));
    }

    public static ResourceLocation prefix(String name) {
        return new ResourceLocation(MODID, name.toLowerCase(Locale.ROOT));
    }


    public void gatherData(GatherDataEvent e) {
        DataGenerator generator = e.getGenerator();
        ExistingFileHelper helper = e.getExistingFileHelper();

        if(e.includeClient()) {
            generator.addProvider(true, new StellarBlockstates(generator, helper));
            generator.addProvider(true, new StellarItemmodels(generator, helper));
        }

        if(e.includeServer()) {}
    }

}


