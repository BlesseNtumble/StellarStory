package stellarstory.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stellarstory.StellarStory;
import stellarstory.client.screen.SuitScreen;
import stellarstory.common.equipment.SuitMenu;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends EffectRenderingInventoryScreen<InventoryMenu> {

    private static final ResourceLocation CREATIVE_TABS_LOCATION = ResourceLocation.parse("textures/gui/container/creative_inventory/tabs.png");

    private static final int TAB_OFFSET_X = 2;
    private static final int TAB_OFFSET_Y = -32;

    public MixinInventoryScreen(InventoryMenu p_98701_, Inventory p_98702_, Component p_98703_) {
        super(p_98701_, p_98702_, p_98703_);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSuitTabButton(CallbackInfo ci) {
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(GuiGraphics graphics, int mouseX, int mouseY, float p_98878_, CallbackInfo ci) {
        renderTab(graphics);
    }

    private void renderTab(GuiGraphics graphics) {
        RenderSystem.enableBlend();
        graphics.blit(CREATIVE_TABS_LOCATION, this.leftPos + TAB_OFFSET_X, this.topPos + TAB_OFFSET_Y, 26, 0, 26, 32);
        //render item
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        ItemStack itemstack = Items.APPLE.getDefaultInstance();
        graphics.renderItem(itemstack, this.leftPos + TAB_OFFSET_X + 13 - 8, this.topPos + TAB_OFFSET_Y + 16 - 7);
        graphics.renderItemDecorations(this.font, itemstack, this.leftPos + TAB_OFFSET_X + 13 - 8, this.topPos + TAB_OFFSET_Y + 16 - 7);
        graphics.pose().popPose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double mouseX, double mouseY, int p_98843_, CallbackInfoReturnable<Boolean> cir) {
        Player player = this.minecraft.player;

        if(mouseX >= this.leftPos + TAB_OFFSET_X && mouseX < this.leftPos + 26 + TAB_OFFSET_X && mouseY >= this.topPos + TAB_OFFSET_Y && mouseY < this.topPos) {
            this.minecraft.setScreen(new SuitScreen(new SuitMenu(0, player.getInventory(), player),
                    player.getInventory(),
                    Component.translatable("container.stellarstory.suit_inventory")));
            cir.setReturnValue(true);
        }
    }
}
