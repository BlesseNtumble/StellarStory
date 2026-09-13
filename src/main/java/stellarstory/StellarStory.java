package stellarstory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerLifecycleEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.core.RegistrySetBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stellarstory.common.init.*;
import stellarstory.common.worldgen.StellarFeatures;
import stellarstory.common.worldgen.StellarWorldGen;
import stellarstory.data.StellarBlockstatesData;
import stellarstory.data.StellarItemModelData;

import java.util.Set;
import java.util.concurrent.CompletableFuture;


import java.util.Locale;

@Mod(StellarStory.MODID)
public class StellarStory {
    public static final String MODID = "stellarstory";
    private static final Logger LOGGER = LogManager.getLogger();

    private static final boolean isDebug = true;


    public StellarStory(FMLJavaModLoadingContext context) {

        IEventBus eventBus = context.getModEventBus();
        IEventBus modEventBus = MinecraftForge.EVENT_BUS;

        StellarConfigs.register(context);

        StellarItems.REGISTRY.register(eventBus);
        StellarBlocks.REGISTRY.register(eventBus);
        StellarTabs.REGISTRY.register(eventBus);
        StellarMenuTypes.REGISTRY.register(eventBus);
        StellarFeatures.FEATURES.register(eventBus);
        StellarDimensions.DIMENSION_TYPES.register(eventBus);
        StellarDimensions.CHUNK_GENERATORS.register(eventBus);

        StellarDimensions.initialization();
//
//        StellarRegistries.ITEMS.register(eventBus);
//        StellarRegistries.BLOCKS.register(eventBus);
//        StellarRegistries.DIMENSION_TYPES.register(eventBus);
//        StellarRegistries.CHUNK_GENERATORS.register(eventBus);
//
//        StellarDimensions.register(eventBus);
//        NetworkHandler.registerPackets();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::serverStarting);

        // Реестр небесных тел грузится через AddReloadListenerEvent
        // (см. common.events.CelestialDataEvents) — и на сервере, и на клиенте.
        modEventBus.addListener(this::registerCommands);
        eventBus.addListener(this::gatherData);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(this::clientSetup));

    }




    public void clientSetup(FMLClientSetupEvent event) {

    }

    public void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

        });
    }

    public void serverStarting(ServerLifecycleEvent event) {
        // Пусто: загрузка данных происходит в CelestialDataEvents (AddReloadListenerEvent)
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void addDebugMessage(String message) {
        if(!isDebug) return;
        getLogger().debug("[{}] DEBUG: {}", MODID, message);
    }

    public static ResourceLocation prefix(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name.toLowerCase(Locale.ROOT));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        StellarCommands.register(event.getDispatcher());
    }

    public void gatherData(GatherDataEvent e) {
        DataGenerator generator = e.getGenerator();
        ExistingFileHelper helper = e.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = e.getLookupProvider();


        if(e.includeClient()) {
            generator.addProvider(true, new StellarBlockstatesData(generator.getPackOutput(), helper));
            generator.addProvider(true, new StellarItemModelData(generator.getPackOutput(), helper));
        }

        if(e.includeServer()) {
            RegistrySetBuilder builder = new RegistrySetBuilder()
                    .add(Registries.CONFIGURED_FEATURE, StellarWorldGen::bootstrapConfigured)
                    .add(Registries.PLACED_FEATURE, StellarWorldGen::bootstrapPlaced);
            generator.addProvider(true, new DatapackBuiltinEntriesProvider(
                    generator.getPackOutput(), lookupProvider, builder, Set.of(StellarStory.MODID)));
        }
    }

}


