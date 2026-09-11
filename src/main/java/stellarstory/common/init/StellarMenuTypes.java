package stellarstory.common.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import stellarstory.StellarStory;
import stellarstory.common.equipment.SuitMenu;

/**
 * Регистрация типов меню (ScreenHandler) для мода.
 */
public class StellarMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, StellarStory.MODID);

    /**
     * Меню для инвентаря с вкладкой экипировки скафандра.
     */
    public static final RegistryObject<MenuType<SuitMenu>> SUIT_MENU =
            REGISTRY.register("suit_menu", () ->
                    IForgeMenuType.create(
                            (windowId, inv, data) -> new SuitMenu(windowId, inv)
                    )
            );
}
