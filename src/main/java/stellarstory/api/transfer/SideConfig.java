package stellarstory.api.transfer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Настраиваемые роли сторон блока для каждого вида переноса.
 * Индекс массива ролей = Direction.ordinal().
 * Роли могут только СУЖАТЬ возможности хранилища (см. EnergyConfig), но не расширять их.
 */
public final class SideConfig {

    public static final String TAG = "TransferSides";
    private static final TransferKind[] KINDS = TransferKind.values();

    private final Map<TransferKind, SideMode[]> modes = new EnumMap<>(TransferKind.class);

    public SideConfig(SideMode energy, SideMode item, SideMode fluid) {
        for (TransferKind kind : KINDS) {
            SideMode[] arr = new SideMode[6];
            SideMode def = switch (kind) {
                case ENERGY -> energy;
                case ITEM -> item;
                case FLUID -> fluid;
            };
            for (int i = 0; i < 6; i++) {
                arr[i] = def;
            }
            modes.put(kind, arr);
        }
    }

    /**
     * Машина-потребитель: энергия входит со всех сторон, кроме дна.
     * Предметы входят с любой стороны, дно — выход готовой продукции,
     * иначе автоматизация не работает до первой настройки ключом.
     * Жидкость отключена везде — бак ещё не реализован.
     */
    public static SideConfig machineDefaults() {
        return new SideConfig(SideMode.INPUT, SideMode.INPUT, SideMode.DISABLED)
                .with(TransferKind.ENERGY, Direction.DOWN, SideMode.DISABLED)
                .with(TransferKind.ITEM, Direction.DOWN, SideMode.OUTPUT);
    }

    /** Аккумулятор: энергия входит со всех сторон, дно — отдача в линию. */
    public static SideConfig batteryDefaults() {
        return new SideConfig(SideMode.INPUT, SideMode.INPUT, SideMode.DISABLED)
                .with(TransferKind.ENERGY, Direction.DOWN, SideMode.OUTPUT)
                .with(TransferKind.ITEM, Direction.DOWN, SideMode.DISABLED);
    }

    /** Генератор: энергия уходит со всех сторон, кроме верха (верх — загрузка топлива). */
    public static SideConfig generatorDefaults() {
        return new SideConfig(SideMode.OUTPUT, SideMode.INPUT, SideMode.DISABLED)
                .with(TransferKind.ENERGY, Direction.UP, SideMode.DISABLED)
                .with(TransferKind.ITEM, Direction.DOWN, SideMode.DISABLED);
    }

    public SideMode get(TransferKind kind, Direction side) {
        return modes.get(kind)[side.ordinal()];
    }

    public void set(TransferKind kind, Direction side, SideMode mode) {
        modes.get(kind)[side.ordinal()] = mode;
    }

    /** Fluent-хелпер для описания дефолтов блока. */
    public SideConfig with(TransferKind kind, Direction side, SideMode mode) {
        set(kind, side, mode);
        return this;
    }

    public SideMode cycle(TransferKind kind, Direction side) {
        SideMode next = get(kind, side).next();
        set(kind, side, next);
        return next;
    }

    /**
     * null-сторона = внутренний или безликий запрос (хоппер, софт-модель, null-capability):
     * разрешаем, если с какой-то стороны операция вообще возможна.
     */
    public boolean canReceive(TransferKind kind, @Nullable Direction side) {
        return side == null ? any(kind, true) : get(kind, side).canReceive();
    }

    public boolean canExtract(TransferKind kind, @Nullable Direction side) {
        return side == null ? any(kind, false) : get(kind, side).canExtract();
    }

    private boolean any(TransferKind kind, boolean receive) {
        for (SideMode mode : modes.get(kind)) {
            if (receive ? mode.canReceive() : mode.canExtract()) {
                return true;
            }
        }
        return false;
    }

    public void save(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        for (TransferKind kind : KINDS) {
            SideMode[] src = modes.get(kind);
            byte[] arr = new byte[6];
            for (int i = 0; i < 6; i++) {
                arr[i] = (byte) src[i].ordinal();
            }
            data.putByteArray(kind.name(), arr);
        }
        tag.put(TAG, data);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains(TAG)) {
            return;
        }
        CompoundTag data = tag.getCompound(TAG);
        SideMode[] all = SideMode.values();
        for (TransferKind kind : KINDS) {
            if (!data.contains(kind.name())) {
                continue;
            }
            byte[] arr = data.getByteArray(kind.name());
            SideMode[] dst = modes.get(kind);
            for (int i = 0; i < 6 && i < arr.length; i++) {
                if (arr[i] >= 0 && arr[i] < all.length) {
                    dst[i] = all[arr[i]];
                }
            }
        }
    }
}
