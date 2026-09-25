package stellarstory.api.transfer;

import net.minecraft.core.Direction;

/**
 * Блок (BlockEntity), у которого игрок может настроить роли сторон.
 * Сейчас настраивается ключом, позже — селектором в GUI.
 */
public interface ISideConfigurable {

    SideConfig getSideConfig();

    /** @return true, если роль реально изменилась. */
    boolean setSideMode(TransferKind kind, Direction side, SideMode mode);

    /** Переключить роль по кругу (используется ключом). */
    SideMode cycleSideMode(TransferKind kind, Direction side);
}
