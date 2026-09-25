package stellarstory.api.transfer;

/**
 * Роль слота инвентаря относительно внешнего доступа.
 * Один и тот же слот не может быть и входом, и выходом для соседей,
 * иначе труба зальёт сырьём готовый продукт.
 */
public enum SlotRole {
    /** Соседи кладут внутрь, машина забирает сама. */
    INPUT(true, false),
    /** То же, что INPUT, но отдельная семантика (топливный слот). */
    FUEL(true, false),
    /** Машина кладёт, соседи забирают. */
    OUTPUT(false, true),
    /** Только внутренний цикл, снаружи слот не виден. */
    INTERNAL(false, false);

    public final boolean insertable;
    public final boolean extractable;

    SlotRole(boolean insertable, boolean extractable) {
        this.insertable = insertable;
        this.extractable = extractable;
    }
}
