package stellarstory.api.energy;

/**
 * Параметры энергоячейки механизма (все значения в FE).
 * maxReceive/maxExtract = 0 полностью запрещают соответствующую операцию,
 * независимо от настроенных ролей сторон.
 */
public record EnergyConfig(int capacity, int maxReceive, int maxExtract) {

    public static EnergyConfig of(int capacity, int maxReceive, int maxExtract) {
        return new EnergyConfig(Math.max(0, capacity), Math.max(0, maxReceive), Math.max(0, maxExtract));
    }

    /** Только приём: машины-потребители. */
    public static EnergyConfig receiver(int capacity, int maxReceive) {
        return of(capacity, maxReceive, 0);
    }

    /** Только отдача: генераторы. */
    public static EnergyConfig provider(int capacity, int maxExtract) {
        return of(capacity, 0, maxExtract);
    }
}
