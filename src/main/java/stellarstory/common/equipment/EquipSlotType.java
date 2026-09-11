package stellarstory.common.equipment;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Типы кастомных слотов экипировки для космического мода.
 * Каждый тип определяет, какие предметы в него можно положить.
 */
public enum EquipSlotType {
    /** Шлем — защита головы в космосе */
    HELMET("helmet",
            stack -> stack.getItem() instanceof ArmorItem),

    /** Верхняя часть скафандра */
    CHESTPLATE("chestplate",
            stack -> stack.getItem() instanceof ArmorItem),

    /** Нижняя часть скафандра (штаны) */
    LEGGINGS("leggings",
            stack -> stack.getItem() instanceof ArmorItem),

    /** Ботинки — скафандр-обувь */
    BOOTS("boots",
            stack -> stack.getItem() instanceof ArmorItem),

    /** Кислородный баллон — обеспечивает дыхание в вакууме */
    OXYGEN_TANK("oxygen_tank",
            stack -> stack.hasTag() && stack.getOrCreateTag().contains("Oxygen")),

    /** Сканер — для обнаружения ресурсов и аномалий */
    SCANNER("scanner",
            stack -> stack.hasTag() && stack.getOrCreateTag().contains("ScannerType")),

    /** Терморегулятор — защита от экстремальных температур */
    THERMAL_REGULATOR("thermal_regulator",
            stack -> stack.hasTag() && stack.getOrCreateTag().contains("ThermalMod"));

    private final String name;
    private final Predicate<ItemStack> validator;

    EquipSlotType(String name, Predicate<ItemStack> validator) {
        this.name = name;
        this.validator = validator;
    }

    /**
     * Проверяет, можно ли положить предмет в этот слот.
     */
    public boolean canItemEquip(ItemStack stack) {
        return validator.test(stack);
    }

    /**
     * Возвращает перевод названия слота для GUI.
     */
    public String getTranslationKey() {
        return "tooltip.stellarstory.equip_slot." + name;
    }

    public String getName() {
        return name;
    }
}
