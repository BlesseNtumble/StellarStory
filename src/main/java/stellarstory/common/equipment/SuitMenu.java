package stellarstory.common.equipment;

import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import stellarstory.common.capability.PlayerCapability;
import stellarstory.common.init.StellarCapabilities;
import stellarstory.common.init.StellarMenuTypes;

/**
 * Меню (ScreenHandler) для инвентаря с вкладкой экипировки.
 * Содержит стандартные слоты игрока + кастомные слоты экипировки.
 */
public class SuitMenu extends AbstractContainerMenu {

    private final PlayerCapability playerCapability;
    private final Inventory playerInventory;


    /**
     * Создаёт новое меню экипировки.
     * Берёт контейнер из capability игрока (автосохранение).
     */
    public SuitMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public SuitMenu(int containerId, Inventory playerInventory, @Nullable Player player) {
        super(StellarMenuTypes.SUIT_MENU.get(), containerId);
        this.playerInventory = playerInventory;

        // Получаем capability игрока
        if (player != null) {
            this.playerCapability = player.getCapability(StellarCapabilities.PLAYER_CAPABILITY, null).orElse(null);
        } else {
            this.playerCapability = null;
        }

        setupSlots();
    }

    /**
     * Настраивает все слоты — экипировку + стандартный инвентарь.
     */
    private void setupSlots() {
        if (playerCapability == null) return;

        // ── Слоты экипировки ──────────────────────────────────────────
        setupEquipSlot(EquipSlotType.HELMET.ordinal(), EquipSlotType.HELMET, 62, 17);
        setupEquipSlot(EquipSlotType.CHESTPLATE.ordinal(), EquipSlotType.CHESTPLATE, 62, 41);
        setupEquipSlot(EquipSlotType.LEGGINGS.ordinal(), EquipSlotType.LEGGINGS, 62, 65);
        setupEquipSlot(EquipSlotType.BOOTS.ordinal(), EquipSlotType.BOOTS, 62, 89);

        // Дополнительные слоты справа
        setupEquipSlot(EquipSlotType.OXYGEN_TANK.ordinal(), EquipSlotType.OXYGEN_TANK, 86, 17);
        setupEquipSlot(EquipSlotType.SCANNER.ordinal(), EquipSlotType.SCANNER, 86, 41);
        setupEquipSlot(EquipSlotType.THERMAL_REGULATOR.ordinal(), EquipSlotType.THERMAL_REGULATOR, 86, 65);

        // ── Стандартный инвентарь игрока ──────────────────────────────
        // Горячие слоты (0-8)
        for (int row = 0; row < 9; row++) {
            addSlot(new Slot(playerInventory, row, 8 + row * 18, 142));
        }

        // Основной инвентарь (9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 53 + row * 18 + 90));
            }
        }
    }

    /**
     * Добавляет один слот экипировки.
     */
    private void setupEquipSlot(int index, EquipSlotType type, int x, int y) {
        SlotItemHandler slot = new SlotItemHandler(playerCapability.getSuitInventory(), index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true; // Размещаем любые предметы
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (playerCapability != null) {
                    playerCapability.setChanged();
                }
            }
        };
        addSlot(slot);
    }

    /**
     * При перетаскивании предмета — проверяем слот экипировки.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getItem();
        ItemStack copiedStack = originalStack.copy();

        // Если предмет берём из слота экипировки — только 1 стек
        if (index < EquipSlotType.values().length) {
            // Перемещаем в инвентарь игрока
            if (!moveItemStackTo(originalStack, EquipSlotType.values().length, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Перемещаем в слот экипировки
            EquipSlotType equipType = getEquipTypeForSlot(index);
            if (equipType != null) {
                SlotItemHandler equipSlot = (SlotItemHandler) slots.get(equipType.ordinal());
                if (equipSlot != null && equipSlot.mayPlace(originalStack)) {
                    if (!moveItemStackTo(originalStack, equipType.ordinal(), equipType.ordinal() + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (originalStack.getCount() == copiedStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, originalStack);
        if (playerCapability != null) {
            playerCapability.setChanged();
        }
        return copiedStack;
    }

    /**
     * Определяет тип экипировки для данного индекса слота.
     */
    @Nullable
    private EquipSlotType getEquipTypeForSlot(int slotIndex) {
        if (slotIndex >= EquipSlotType.values().length && slotIndex < slots.size()) {
            int relativeIndex = slotIndex - EquipSlotType.values().length;
            if (relativeIndex >= 0 && relativeIndex < EquipSlotType.values().length) {
                return EquipSlotType.values()[relativeIndex];
            }
        }
        return null;
    }

    /**
     * Проверяет, может ли игрок взаимодействовать с контейнером.
     */
    @Override
    public boolean stillValid(Player player) {
        return true; // Всегда доступен для игрока
    }

    /**
     * Возвращает capability игрока.
     */
    public PlayerCapability getPlayerCapability() {
        return playerCapability;
    }

    /**
     * Возвращает предмет в указанном слоте экипировки.
     */
    public ItemStack getEquippedItem(EquipSlotType type) {
        if (playerCapability != null) {
            return playerCapability.getSuitItem(type);
        }
        return ItemStack.EMPTY;
    }
}
