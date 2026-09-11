package stellarstory.common.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

public class StationBuildData extends SavedData {

    private static final String DATA_NAME = "station_build";

    public boolean built;
    private String templateId;

    public StationBuildData() {
        this.built = false;
        this.templateId = "";
    }

    public StationBuildData(CompoundTag tag) {
        // getString возвращает "" для отсутствующего ключа, getBoolean — false
        this.built = tag.getBoolean("built");
        this.templateId = tag.getString("template");
    }

    @Nonnull
    public static StationBuildData get(Level level) {
        if (level.isClientSide) {
            throw new RuntimeException("Don't access this client-side!");
        }

        DimensionDataStorage storage = ((ServerLevel) level).getDataStorage();
        return storage.computeIfAbsent(StationBuildData::new, StationBuildData::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("built", this.built);
        tag.putString("template", this.templateId);
        return tag;
    }

    public boolean isBuilt() {
        return built;
    }

    /**
     * Станция считается построенной только если шаблон совпадает.
     * Поменяли .nbt у станции — она переставится при следующем визите,
     * без ручной зачистки мира.
     */
    public boolean isBuiltFor(ResourceLocation template) {
        return built && template != null && this.templateId.equals(template.toString());
    }

    public void markBuilt(ResourceLocation template) {
        this.built = true;
        this.templateId = template == null ? "" : template.toString();
        setDirty();
    }

    /** Для отладки: сбросить отметку, чтобы станция переставилась. */
    public void reset() {
        this.built = false;
        this.templateId = "";
        setDirty();
    }

    public ResourceLocation getTemplateId() {
        return templateId.isEmpty() ? null : ResourceLocation.tryParse(templateId);
    }
}