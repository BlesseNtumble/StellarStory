package stellarstory.client.utils;

public class Utils {

    public static float[] convertRGBAtoFloat(int r, int g, int b, int a) {
        return new float[] {r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F};
    }

    public static int getColor(int r, int g, int b, int a) {

        int color = a << 24 | r << 16 | g << 8 | b;
        return color - 16777216;
    }
}
