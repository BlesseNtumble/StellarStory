package stellarstory.common.registers;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import stellarstory.StellarStory;

public class StellarItems {

    public static CreativeModeTab STELLARSTORY_ITEMS = new CreativeModeTab("stellarstory_items") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.APPLE);
        }
    };

}
