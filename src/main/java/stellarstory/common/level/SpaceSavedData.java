package stellarstory.common.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

public class SpaceSavedData extends SavedData {

    public float lastSyncedTemp = -999f;
    public int syncTicks = 0;

    private float temperature;

    public SpaceSavedData() {
        this.temperature = 0.0F;
    }

    @Nonnull
    public static SpaceSavedData get(Level level) {
        if (level.isClientSide) {
            throw new RuntimeException("Don't access this client-side!");
        }

        DimensionDataStorage storage = ((ServerLevel) level).getDataStorage();
        return storage.computeIfAbsent(SpaceSavedData::new, SpaceSavedData::new, "spacesaveddata");
    }

    public SpaceSavedData(CompoundTag tag) {
        this.temperature = tag.getFloat("temperature");
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putFloat("temperature", this.temperature);
        return tag;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
        setDirty();
    }

    public float getTemperature() {
        return this.temperature;
    }
}
