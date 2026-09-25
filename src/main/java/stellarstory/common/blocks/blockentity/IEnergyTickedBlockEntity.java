package stellarstory.common.blocks.blockentity;

/**
 * Точка серверного тика для энерго-механизмов.
 * Тикер блока работает только с интерфейсом, поэтому новый тип машины
 * не требует правки общего тикера.
 */
public interface IEnergyTickedBlockEntity {

    void serverTick();
}
