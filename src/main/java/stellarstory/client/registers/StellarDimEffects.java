package stellarstory.client.registers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import stellarstory.StellarStory;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.CelestialBody;
import stellarstory.client.renderer.sky.CelestialSky;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = StellarStory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StellarDimEffects {

    @SubscribeEvent
    public static void registerDimEffects(RegisterDimensionSpecialEffectsEvent e) {
        Set<CelestialBody> reachableBodies = new HashSet<CelestialBody>();


        CelestialRegisters.getAllPlanets().forEach((planet) -> {
            if(planet.isReachable())
                reachableBodies.add(planet);
        });

        CelestialRegisters.getAllChildBodies().forEach((child) -> {
            if(((CelestialBody)child).isReachable())
                reachableBodies.add((CelestialBody) child);
        });

        reachableBodies.forEach((bodies) -> {
            e.register(bodies.CELESTIAL_DIM_TYPE.get().effectsLocation(), new CelestialSky());
        });

    }
}
