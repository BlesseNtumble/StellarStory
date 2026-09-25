package stellarstory.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class StellarGeneralConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ── Трубы / энергосеть ────────────────────────────────────────
    public static final ForgeConfigSpec.IntValue PIPE_TICK_INTERVAL;
    public static final ForgeConfigSpec.IntValue PIPE_MAX_FE_PER_TICK;
    public static final ForgeConfigSpec.IntValue PIPE_REBUILD_BUDGET;
    public static final ForgeConfigSpec.IntValue PIPE_IDLE_AFTER;
    public static final ForgeConfigSpec.IntValue PIPE_ENDPOINT_RECHECK;

    static {
        BUILDER.push("pipes");
        PIPE_TICK_INTERVAL = BUILDER
                .comment("Каждый N-ный тик выполнять перенос энергии по сети")
                .defineInRange("pipeTickInterval", 1, 1, 20);
        PIPE_MAX_FE_PER_TICK = BUILDER
                .comment("Пропускная способность одной сети, FE за тик переноса")
                .defineInRange("pipeMaxFePerTick", 256, 1, 1000000);
        PIPE_REBUILD_BUDGET = BUILDER
                .comment("Сколько сегментов трубы перестраивать за один тик")
                .defineInRange("pipeRebuildBudget", 2048, 16, 65536);
        PIPE_IDLE_AFTER = BUILDER
                .comment("После N тиков без переноса сеть засыпает до изменения графа")
                .defineInRange("pipeIdleAfter", 20, 1, 1000);
        PIPE_ENDPOINT_RECHECK = BUILDER
                .comment("Как часто принудительно переспрашивать capability эндпоинта, тиков")
                .defineInRange("pipeEndpointRecheck", 100, 20, 1000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
