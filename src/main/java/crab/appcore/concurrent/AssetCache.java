package crab.appcore.concurrent;

import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, safe image cache for a JavaFX application.
 *
 * <h3>Why synchronous loading instead of background threads?</h3>
 * <p>
 * JavaFX {@code Image} with {@code backgroundLoading=true} caused
 * {@code OutOfMemoryError} when many GIF/PNG files were queued simultaneously
 * because each background-loaded image holds its full uncompressed pixel data
 * in heap until garbage-collected, and animated GIFs are especially large.
 * Loading 28+ assets concurrently exceeded the default JVM heap.
 * </p>
 * <p>
 * The chosen strategy is <b>load-on-first-use, cache forever</b>:
 * each image is loaded synchronously the first time it is requested (fast, since
 * individual image files are small) and the resulting object is cached in a
 * {@link ConcurrentHashMap}. All subsequent requests for the same path return
 * the cached object instantly — no I/O, no thread overhead.
 * </p>
 *
 * <h3>Where multithreading IS appropriate in this project</h3>
 * <ul>
 *   <li><b>Database I/O</b> — login, save/load, credential checks all block the
 *       FX Application Thread today (visible in stack traces). Move these to a
 *       {@code javafx.concurrent.Task} or a single-thread {@code ExecutorService}
 *       and post results back via {@code Platform.runLater()}.</li>
 *   <li><b>AI action calculation</b> — if AI logic grows (decision trees, scoring),
 *       run {@code submitAiActions()} on a background thread so complex turns
 *       do not stall the UI.</li>
 *   <li><b>Serialization / Save operations</b> — writing game state to disk or DB
 *       should be off-thread to avoid jank during auto-save.</li>
 * </ul>
 */
public final class AssetCache {

    private static final AssetCache INSTANCE = new AssetCache();

    /** Thread-safe map from resolved URL string → cached Image. */
    private final ConcurrentHashMap<String, Image> cache = new ConcurrentHashMap<>();

    private AssetCache() {}

    public static AssetCache getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the cached {@link Image} for a fully-resolved URL string,
     * loading it synchronously on the first call.
     *
     * <p>This is safe to call from the FX Application Thread because:
     * <ul>
     *   <li>Individual image files are small — the synchronous load is
     *       imperceptibly fast (a few milliseconds).</li>
     *   <li>Every subsequent call for the same URL returns the cached object
     *       in O(1) with zero I/O — no heap allocation, no thread creation.</li>
     * </ul>
     *
     * @param resolvedUrl a fully-qualified URL string obtained via
     *                    {@code getClass().getResource(path).toExternalForm()}
     * @return the cached or freshly-loaded {@link Image}
     */
    public Image getOrLoad(String resolvedUrl) {
        // Fast path — already cached
        Image cached = cache.get(resolvedUrl);
        if (cached != null) return cached;

        // First-time load: synchronous, fast for small files
        Image img = new Image(resolvedUrl);           // backgroundLoading defaults to false
        Image winner = cache.putIfAbsent(resolvedUrl, img);
        return winner != null ? winner : img;         // winner != null only if two threads raced
    }

    /**
     * Classpath path → URL resolution helper used by the controller.
     * Returns {@code null} when the resource is not found so callers can skip gracefully.
     */
    public static List<String> allGameplayPaths(String saboteurKey,
                                                  String altruistKey,
                                                  String opportunistKey) {
        return List.of(
            "/assets/humanoid-art/saboteurm.gif",
            "/assets/humanoid-art/saboteurfm.gif",
            "/assets/humanoid-art/altruistm.gif",
            "/assets/humanoid-art/altruistfm.gif",
            "/assets/humanoid-art/opportunistm.gif",
            "/assets/humanoid-art/opportunistfm.gif",
            "/assets/crab-art/" + saboteurKey    + "_idle.gif",
            "/assets/crab-art/" + saboteurKey    + "_damage.gif",
            "/assets/crab-art/" + altruistKey    + "_idle.gif",
            "/assets/crab-art/" + altruistKey    + "_damage.gif",
            "/assets/crab-art/" + opportunistKey + "_idle.gif",
            "/assets/crab-art/" + opportunistKey + "_damage.gif",
            "/assets/event-art/market_crash_event.png",
            "/assets/event-art/charity_wave_event.png",
            "/assets/event-art/crab_hunt_event.png",
            "/assets/event-art/travelling_merchant_event.png",
            "/assets/textures/bg_beach.gif",
            "/assets/card-art/placeholdercard.png"
        );
    }
}
