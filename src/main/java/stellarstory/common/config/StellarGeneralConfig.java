package stellarstory.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class StellarGeneralConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    static {
        SPEC = BUILDER.build();
    }
}
