package stellarstory.common.blocks.pipe;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** Энерготруба: сегмент энергосети модели A (шина без собственной ёмкости). */
public class EnergyPipeBlock extends AbstractPipeBlock {

    public EnergyPipeBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 4.0F)
                .sound(SoundType.METAL));
    }
}
