package stellarstory.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Загружает описание небесных тел из data/&lt;ns&gt;/celestialbodies на ОБЕИХ сторонах.
 *
 * <p>Сервер — через AddReloadListenerEvent (старт сервера и /reload),
 * клиент — через RegisterClientReloadListenersEvent (загрузка ресурсов и F3+T).
 * JSON лежат в самом моде, поэтому обе стороны парсят одинаковые файлы
 * и реестры совпадают без пакета синхронизации.</p>
 */
public class CelestialBodyDataLoader extends SimpleJsonResourceReloadListener {

    public static final CelestialBodyDataLoader INSTANCE = new CelestialBodyDataLoader();

    public CelestialBodyDataLoader() {
        super(new Gson(), "celestialbodies");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        // Разбор делаем сами: лоадер различает типы тел по подпапкам
        // (planets/, moons/, stations/...), а не по общему списку json.
        CelestialBodyLoader.loadCelestialBodies(resourceManager);
    }
}
