package crab.appcore.concurrent;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight Factory for managing and sharing downsampled visual assets.
 * 
 * Design Pattern: Flyweight
 * - Intrinsic State: The shared resource path, requested width/height, and class theme.
 * - Extrinsic State: The specific viewer slot or playerId (to prevent concurrent JavaFX shared-GIF freezes).
 * 
 * Benefits:
 * - Bounds memory usage by lazily loading and caching exactly one independent downsampled Image per slot/asset.
 * - Prevents multiple duplicate images for the same slot, avoiding OutOfMemoryErrors.
 * - Solves the JavaFX shared GIF animation freeze bug by partitioning cache keys by viewer slot.
 */
public final class AssetFlyweightFactory {

    private static final Map<String, Image> flyweightCache = new HashMap<>();

    private AssetFlyweightFactory() {}

    /**
     * Retrieves a downsampled Image for the given slot and asset path.
     * If not loaded, it is lazily loaded and cached.
     * 
     * @param slotId the extrinsic viewer slot (e.g. "human", "ai_0", "card_opportunist")
     * @param resourcePath the intrinsic asset path (e.g. "/assets/crab-art/altruist_idle.gif")
     * @param requestedWidth target downsampled width
     * @param requestedHeight target downsampled height
     * @return the unique downsampled Image for this slot
     */
    public static Image getSharedImage(String slotId, String resourcePath, double requestedWidth, double requestedHeight) {
        String cacheKey = slotId + "_" + resourcePath + "_" + (int)requestedWidth + "x" + (int)requestedHeight;
        
        Image img = flyweightCache.get(cacheKey);
        if (img == null) {
            var resource = AssetFlyweightFactory.class.getResource(resourcePath);
            if (resource != null) {
                // Load synchronously, downsampled to request dimensions, backgroundLoading = false
                img = new Image(resource.toExternalForm(), requestedWidth, requestedHeight, true, true, false);
                flyweightCache.put(cacheKey, img);
            }
        }
        return img;
    }

    /**
     * Retrieves an image with background loading enabled (e.g. for character selection cards).
     */
    public static Image getSharedImageAsync(String slotId, String resourcePath, double requestedWidth, double requestedHeight) {
        String cacheKey = slotId + "_" + resourcePath + "_" + (int)requestedWidth + "x" + (int)requestedHeight + "_async";
        
        Image img = flyweightCache.get(cacheKey);
        if (img == null) {
            var resource = AssetFlyweightFactory.class.getResource(resourcePath);
            if (resource != null) {
                // Load asynchronously, downsampled to request dimensions, backgroundLoading = true
                img = new Image(resource.toExternalForm(), requestedWidth, requestedHeight, true, true, true);
                flyweightCache.put(cacheKey, img);
            }
        }
        return img;
    }

    /**
     * Reclaims memory by clearing flyweight references and running Garbage Collection.
     */
    public static void clear() {
        flyweightCache.clear();
        System.gc();
    }
}
