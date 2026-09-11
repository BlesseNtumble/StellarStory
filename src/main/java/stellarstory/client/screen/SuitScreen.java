package stellarstory.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import stellarstory.StellarStory;
import stellarstory.common.equipment.EquipSlotType;
import stellarstory.common.equipment.SuitMenu;

/**
 * Экран экипировки скафандра.
 */
public class SuitScreen extends AbstractContainerScreen<SuitMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(StellarStory.MODID, "textures/gui/suit_inventory.png");

    public SuitScreen(SuitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
    }

    // ═══════════════════════════════════════════════════════════════
    // Рендеринг
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        drawBackground(guiGraphics);
        drawSuitSlots(guiGraphics);
    }

    /**
     * Рисует фон экрана.
     */
    private void drawBackground(GuiGraphics guiGraphics) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (BACKGROUND != null) {
            guiGraphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);
        } else {
            guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1A2E);
        }
    }

    /**
     * Рисует слоты экипировки скафандра.
     */
    private void drawSuitSlots(GuiGraphics guiGraphics) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Заголовок
        String title = Component.translatable("container.stellarstory.suit_inventory").getString();
        guiGraphics.drawString(font, title, x + 8, y + 6, 0xFFFFFF, false);

        // Слоты экипировки
        for (int i = 0; i < EquipSlotType.values().length; i++) {
            EquipSlotType type = EquipSlotType.values()[i];
            ItemStack stack = menu.getEquippedItem(type);
            int slotX = menu.slots.get(i).x;//getSlotScreenX(i);
            int slotY = menu.slots.get(i).y;

            // Подпись слота
            //String label = Component.translatable(type.getTranslationKey()).getString();
            //nguiGraphics.drawString(font, label, x + slotX - font.width(label) / 2, y + slotY - 12, 0xCCCCCC, false);

            // Предмет в слоте
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, x + slotX - 9, y + slotY - 9);
                String itemName = stack.getHoverName().getString();
                guiGraphics.drawString(font, itemName, x + slotX - font.width(itemName) / 2, y + slotY + 20, 0xFFFFFF, true);
            }
        }
    }

    /**
     * X координата слота экипировки на экране.
     */
    private int getSlotScreenX(int slotIndex) {
        int[] xPositions = {62, 62, 62, 62, 86, 86, 86};
        return xPositions[slotIndex] - 9;
    }

    /**
     * Y координата слота экипировки на экране.
     */
    private int getSlotScreenY(int slotIndex) {
        int[] yPositions = {17, 41, 65, 89, 17, 41, 65};
        return yPositions[slotIndex] - 9;
    }

    // ═══════════════════════════════════════════════════════════════
    // Обработка мыши
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
