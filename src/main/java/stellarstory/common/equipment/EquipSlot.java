package stellarstory.common.equipment;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Кастомный слот для экипировки скафандра и космического оборудования.
 * Позволяет положить только предметы, подходящие для данного типа слота.
 */
public class EquipSlot extends Slot {

    private final EquipSlotType slotType;

    public EquipSlot(Container inventory, EquipSlotType slotType, int index, int x, int y) {
        super(inventory, index, x, y);
        this.slotType = slotType;
    }

    /**
     * Определяет, можно ли положить предмет в этот слот.
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return slotType.canItemEquip(stack);
    }

    /**
     * Ограничивает количество предметов в слоте (только 1).
     */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * При снятии предмета — не проверяем ограничения инвентаря.
     */
    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }

    /**
     * Тип слота для отладки.
     */
    public EquipSlotType getSlotType() {
        return slotType;
    }
}
