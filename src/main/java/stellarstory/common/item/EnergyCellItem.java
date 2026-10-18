package stellarstory.common.item;

/**
 * Предмет-ячейка питания. Хранит энергию (Forge Energy) и может заряжаться
 * из машин или разряжаться в них.
 *
 * Используется в EnergyBatteryBlockEntity для приёма/выдачи заряда.
 */
public class EnergyCellItem extends EnergyStorageItem {

    public static final int CAPACITY = 80_000;
    public static final int MAX_TRANSFER = 500;

    public EnergyCellItem() {
        super(CAPACITY, MAX_TRANSFER);
    }
}
