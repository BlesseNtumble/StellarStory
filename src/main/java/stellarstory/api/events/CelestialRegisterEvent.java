package stellarstory.api.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import stellarstory.api.CelestialRegisters;
import stellarstory.api.celestial.StarSystem;

public class CelestialRegisterEvent extends Event implements IModBusEvent {

    public CelestialRegisterEvent() {}

    public void registerStarSystem(ResourceLocation rl, StarSystem system) {
        CelestialRegisters.registerStarSystem(rl, system);
    }
}
