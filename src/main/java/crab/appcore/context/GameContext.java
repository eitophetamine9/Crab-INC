package crab.appcore.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared application context for foundation services.
 *
 * Design patterns:
 * - Service Registry: stores appcore services by type.
 *
 * SOLID:
 * - Single Responsibility: owns service registration and lookup only.
 */
public final class GameContext {
    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> type, T service) {
        services.put(type, service);
    }

    public <T> Optional<T> find(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            return Optional.empty();
        }

        return Optional.of(type.cast(service));
    }

    public <T> T require(Class<T> type) {
        return find(type).orElseThrow(() ->
                new IllegalStateException("Missing service: " + type.getName()));
    }
}
