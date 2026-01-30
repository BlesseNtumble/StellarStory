package stellarstory.common.init;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import stellarstory.common.config.StellarGeneralConfig;

public class StellarConfigs {

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, StellarGeneralConfig.SPEC, "stellarstory/general.toml");
    }
}
