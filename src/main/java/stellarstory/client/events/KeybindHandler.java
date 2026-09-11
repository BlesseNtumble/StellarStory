package stellarstory.client.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import stellarstory.StellarStory;
import stellarstory.client.screen.SpaceMapScreen;
import stellarstory.client.screen.SuitScreen;
import stellarstory.common.equipment.SuitMenu;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeybindHandler {

    public static final KeyMapping OPEN_MAP_KEY = new KeyMapping(
            "key.stellarstory.open_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.stellarstory"
    );

    public static final KeyMapping OPEN_SUIT_KEY = new KeyMapping(
            "key.stellarstory.open_suit_inventory",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.stellarstory"
    );

    @Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_MAP_KEY);
            event.register(OPEN_SUIT_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Открытие карты космоса
        if (OPEN_MAP_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new SpaceMapScreen());
        }
        
        // Открытие инвентаря экипировки
        if (OPEN_SUIT_KEY.consumeClick()) {
            openSuitInventoryScreen();
        }
    }

    /**
     * Открывает экран экипировки.
     */
    private static void openSuitInventoryScreen() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null) {
            StellarStory.addDebugMessage("Opening Suit Inventory Screen");

            // Если открыт обычный инвентарь — переключаем на вкладку Suit
            if (mc.screen instanceof InventoryScreen) {
                mc.setScreen(null); // Закрываем инвентарь
                // После закрытия открываем SuitScreen с активной вкладкой Suit
                mc.execute(() -> {
                    mc.setScreen(new SuitScreen(
                            new SuitMenu(0, player.getInventory(), player),
                            player.getInventory(),
                            Component.translatable("container.stellarstory.suit_inventory")
                    ));
                });
            } else {
                // Открываем SuitScreen
                mc.setScreen(new SuitScreen(
                        new SuitMenu(0, player.getInventory(), player),
                        player.getInventory(),
                        Component.translatable("container.stellarstory.suit_inventory")
                ));
            }
        }
    }
}