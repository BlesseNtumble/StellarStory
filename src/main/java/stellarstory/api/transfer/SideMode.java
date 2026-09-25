package stellarstory.api.transfer;

import java.util.Locale;

/**
 * Роль стороны блока в сети передачи.
 * INPUT  = соседи могут ПИСАТЬ в хранилище блока.
 * OUTPUT = соседи могут ЗАБИРАТЬ из хранилища блока.
 */
public enum SideMode {
    DISABLED(false, false),
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true);

    private final boolean receive;
    private final boolean extract;

    SideMode(boolean receive, boolean extract) {
        this.receive = receive;
        this.extract = extract;
    }

    public boolean canReceive() {
        return receive;
    }

    public boolean canExtract() {
        return extract;
    }

    /** Цикл переключения ключом: DISABLED -> INPUT -> OUTPUT -> BOTH -> DISABLED. */
    public SideMode next() {
        SideMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String translationKey() {
        return "gui.stellarstory.side_mode." + name().toLowerCase(Locale.ROOT);
    }
}
