package stellarstory.client.utils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSpaceData {
    private static float currentTemperature = 15.0f;

    public static void setTemperature(float temp) {
        currentTemperature = temp;
    }

    public static float getTemperature() {
        return currentTemperature;
    }
}