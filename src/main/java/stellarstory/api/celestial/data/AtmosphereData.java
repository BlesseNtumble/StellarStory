package stellarstory.api.celestial.data;

public class AtmosphereData {

    public static final float ABSOLUTE_ZERO = -273.15F;
    private float temperature = 15.0F;
    private float temperatureModificator = 1.0F;
    private float temperatureOffset = 0.0F;

    private AtmosphereData() {}

    public AtmosphereData setTemperature(float temperature, float temperatureMod, float tempOffset)
    {
        this.temperature = Math.max(temperature, ABSOLUTE_ZERO);
        this.temperatureModificator = temperatureMod;
        this.temperatureOffset = tempOffset;
        return this;
    }

    public float getTemperature() {
        return this.temperature;
    }
    public float getTemperatureModificator() { return this.temperatureModificator; }
    public float getTemperatureOffset() { return temperatureOffset; }

    public static class Builder {
        public static AtmosphereData create() {
            return new AtmosphereData();
        }
    }
}
