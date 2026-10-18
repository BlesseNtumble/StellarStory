package stellarstory.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;

public class StellarTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StellarStory.MODID);

    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB;

    static {
        CREATIVE_TAB = REGISTRY.register("creative_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.literal("Stellar Story Blocks"))
                    .icon(() -> new ItemStack(Items.APPLE))
                    .displayItems((params, output) -> {
                        StellarItems.MOON_BASIC_BLOCK_ITEMS.forEach(item -> output.accept(item.get()));
                        output.accept(StellarItems.ENERGY_BATTERY.get());
                        output.accept(StellarItems.ENERGY_CELL.get());
                        output.accept(StellarItems.COAL_GENERATOR.get());
                        output.accept(StellarItems.ELECTRIC_FURNACE.get());
                        output.accept(StellarItems.GRINDER.get());
                        output.accept(StellarItems.ENERGY_PIPE.get());
                        output.accept(StellarItems.ENERGY_WRENCH.get());
                    }).build()
        );
    }



}
