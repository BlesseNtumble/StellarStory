package stellarstory.api.celestial.data;

public class PhysicalData {

    private long dayLength = 24000L;
    private float gravity = 1.0F;

    private PhysicalData() {}

    public PhysicalData setDayLength(long dayLength) {
        this.dayLength = dayLength;
        return this;
    }

    public PhysicalData setGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public long getDayLength() { return this.dayLength; }
    public float getGravity() { return this.gravity; }

    public static class Builder {

        public static PhysicalData create() {
            return new PhysicalData();
        }
    }
}
