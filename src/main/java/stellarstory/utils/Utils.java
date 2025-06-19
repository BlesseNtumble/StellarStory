package stellarstory.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import stellarstory.StellarStory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utils {

    public static List<JsonElement> getAllJsonByRL(ResourceManager manager, String dir_path)
    {
        List<JsonElement> jsonElements = new ArrayList<>();

        manager.listResources(dir_path, path -> path.toString().endsWith(".json") && path.toString().startsWith(StellarStory.MODID)).forEach((resourceLocation, resource) -> {
            try (InputStream inputStream = resource.open()) {
                JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                jsonElements.add(jsonElement);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return jsonElements;
    }

    public static void debugResources(ResourceManager manager, String dirPath) {
        System.out.println("Debugging resources in: " + dirPath);

        manager.listResources(dirPath, path -> path.toString().startsWith("stellar"))
                .forEach((resourceLocation, resource) -> {
                    System.out.println("Found resource: " + resourceLocation);
                });
    }
}
