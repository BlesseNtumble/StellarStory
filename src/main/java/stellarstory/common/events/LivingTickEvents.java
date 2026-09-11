package stellarstory.common.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.api.utils.event.EntityTickEvent;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LivingTickEvents {

    private static final double BASE_ITEM_GRAVITY = 0.04D;
    private static final double BASE_LIVING_GRAVITY = 0.08D; // Стандартная гравитация Overworld
    private static final double AIR_RESISTANCE = 0.98D; // Коэффициент сопротивления воздуха

    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent event) {
        Entity entity = event.getEntity();

        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(entity.level());
        if (body == null || body.isEmpty() || !body.isReachable()) return;

        applyCustomGravity(entity, body.getBodyDataHandler().physicalData().gravity());

    }


    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        CelestialBody body = CelestialRegisters.getCelestialBodyFromLevel(entity.level());
        if (body == null || body.isEmpty() || !body.isReachable()) return;

        modifyFallDamage(event, entity, body.getBodyDataHandler().physicalData().gravity());

    }

    private static boolean isSurvivalOrAdventure(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }

    private static void applyCustomGravity(Entity entity, float gravity) {

        if (entity instanceof LivingEntity living) {
            AttributeInstance gravityAttribute = living.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
            if (gravityAttribute != null)
                gravityAttribute.setBaseValue(BASE_LIVING_GRAVITY * gravity);
        }

        if (entity instanceof ItemEntity itemEntity) {
            if(!getCondition(itemEntity))
                return;

            Vec3 motion = itemEntity.getDeltaMovement();

            // Вычисляем новые параметры
            double newGravity = -BASE_ITEM_GRAVITY * (1 - Mth.square(1 - gravity)); // Новая гравитация
            double newAirResistance = AIR_RESISTANCE + (1 - gravity) * 0.01; // Новое воздушное сопротивление


            motion = motion.multiply(newAirResistance, newAirResistance, newAirResistance);
            double dy = motion.y * newAirResistance + newGravity;
            dy = Math.max(dy, -1.0D);

            itemEntity.setDeltaMovement(motion.x, dy, motion.z);

        }
    }

    private static boolean getCondition(ItemEntity entity) {
        return !entity.isInFluidType() && !entity.isInLava() && !entity.isNoGravity();
    }

    // Метод для модификации урона от падения
    private static void modifyFallDamage(LivingFallEvent event, LivingEntity entity, double gravity) {
        double fallDistance = event.getDistance(); // Расстояние падения
        double customGravity = gravity; // Получаем кастомную гравитацию для сущности

        // Пересчитываем урон с учетом кастомной гравитации
        double adjustedFallDistance = fallDistance * (BASE_LIVING_GRAVITY / customGravity);

        // Стандартный расчет урона от падения
        double damage = Math.max(0, (adjustedFallDistance - 3) / 5);

        // Устанавливаем новый урон
        event.setDamageMultiplier((float) damage);
    }
}
