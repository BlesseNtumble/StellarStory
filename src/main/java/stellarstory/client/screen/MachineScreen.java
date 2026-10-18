package stellarstory.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import stellarstory.client.gui.SideConfigPanel;
import stellarstory.common.menu.MachineMenu;

/**
 * Экран механизма. Текстур нет — фон, рамки и полосы рисуются программно,
 * поэтому новый механизм получает GUI без рисования ассетов.
 *
 * Рамки слотов рисует AbstractContainerScreen стандартной текстурой инвентаря.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int COLOR_BACKGROUND = 0xFF1B1B22;
    private static final int COLOR_FRAME = 0xFF4A4A57;
    private static final int COLOR_TRACK = 0xFF2E2E38;
    private static final int COLOR_ENERGY = 0xFFE0B341;
    private static final int COLOR_PROGRESS = 0xFF63C74D;
    /** Полоса цикла, когда машина стоит (нет энергии/ресурса). */
    private static final int COLOR_PROGRESS_DIM = 0xFF8A8A8A;
    private static final int COLOR_TITLE = 0xFFE6E6EC;
    private static final int COLOR_LABEL = 0xFF9A9AA8;

    /** Полосы состояния: ширина, высота и верхняя координата. */
    private static final int BAR_WIDTH = 70;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_Y = 54;
    private static final int BAR_LABEL_Y = BAR_Y - 10;

    @Nullable
    private SideConfigPanel sidePanel;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MachineMenu.WIDTH;
        this.imageHeight = MachineMenu.HEIGHT;
        this.inventoryLabelY = MachineMenu.PLAYER_TOP - 12;
    }

    @Override
    protected void init() {
        super.init();
        // Панель прижата к нижнему краю окна: HEIGHT заранее расширяет окно
        // под неё, поэтому ниже хотбара (166px) остаётся чистая полоса.
        sidePanel = new SideConfigPanel(menu.getBlockEntity().getSideConfig(),
                menu.getBlockEntity().getBlockPos(),
                this.leftPos + (this.imageWidth - SideConfigPanel.WIDTH) / 2,
                this.topPos + this.imageHeight - SideConfigPanel.HEIGHT - 4);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (sidePanel != null) {
            sidePanel.render(graphics, mouseX, mouseY);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
        Component tip = sidePanel == null ? null : sidePanel.getTooltip();
        if (tip != null) {
            graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, COLOR_BACKGROUND);
        drawFrame(graphics, x, y);

        drawEnergyBar(graphics, x, y);
        if (menu.hasProgress()) {
            drawProgress(graphics, x, y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, COLOR_TITLE, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                COLOR_LABEL, false);
    }

    /** Клик по панели граней не должен долетать до слотов. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sidePanel != null && sidePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + this.imageWidth, y + 1, COLOR_FRAME);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, COLOR_FRAME);
        graphics.fill(x, y, x + 1, y + this.imageHeight, COLOR_FRAME);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, COLOR_FRAME);
    }

    /** Горизонтальная шкала заряда слева, под слотами машины. */
    private void drawEnergyBar(GuiGraphics graphics, int x, int y) {
        int barX = x + 8;
        graphics.drawString(this.font, Component.translatable("gui.stellarstory.machine.energy"),
                barX, y + BAR_LABEL_Y, COLOR_LABEL, false);

        graphics.fill(barX, y + BAR_Y, barX + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, COLOR_TRACK);
        int filled = Math.round(BAR_WIDTH * menu.getEnergyRatio());
        if (filled > 0) {
            graphics.fill(barX, y + BAR_Y, barX + filled, y + BAR_Y + BAR_HEIGHT, COLOR_ENERGY);
        }
        graphics.drawString(this.font,
                Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"),
                barX, y + BAR_Y + BAR_HEIGHT + 2, COLOR_LABEL, false);
    }

    /** Горизонтальная полоса прогресса цикла справа от шкалы энергии. */
    private void drawProgress(GuiGraphics graphics, int x, int y) {
        int barX = x + this.imageWidth - BAR_WIDTH - 8;
        graphics.drawString(this.font, Component.translatable("gui.stellarstory.machine.progress"),
                barX, y + BAR_LABEL_Y, COLOR_LABEL, false);

        graphics.fill(barX, y + BAR_Y, barX + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, COLOR_TRACK);
        int filled = Math.round(BAR_WIDTH * menu.getProgress());
        if (filled > 0) {
            graphics.fill(barX, y + BAR_Y, barX + filled, y + BAR_Y + BAR_HEIGHT,
                    menu.isWorking() ? COLOR_PROGRESS : COLOR_PROGRESS_DIM);
        }
        graphics.drawString(this.font, Component.literal(menu.getProgressPercent() + "%"),
                barX, y + BAR_Y + BAR_HEIGHT + 2, COLOR_LABEL, false);
    }
}
