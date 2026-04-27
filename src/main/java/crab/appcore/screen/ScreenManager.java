package crab.appcore.screen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registers and switches named application screens.
 *
 * Design patterns:
 * - Registry: stores screens by id.
 * - Facade: exposes one API for navigation and active-screen updates.
 *
 * SOLID:
 * - Single Responsibility: owns screen registration, switching, and active-screen dispatch.
 */
public final class ScreenManager {
    private final Map<String, GameScreen> screens = new LinkedHashMap<>();
    private GameScreen current;

    public void register(GameScreen screen) {
        String id = screen.id();
        if (screens.containsKey(id)) {
            throw new IllegalArgumentException("Screen already registered: " + id);
        }

        screens.put(id, screen);
    }

    public void show(String id) {
        GameScreen next = screens.get(id);
        if (next == null) {
            throw new IllegalArgumentException("Unknown screen: " + id);
        }

        if (current == next) {
            return;
        }

        if (current != null) {
            current.hide();
        }

        current = next;
        current.show();
    }

    public Optional<String> currentId() {
        return current == null ? Optional.empty() : Optional.of(current.id());
    }

    public void update(double tpf) {
        if (current != null) {
            current.update(tpf);
        }
    }

    public void clear() {
        if (current != null) {
            current.hide();
            current = null;
        }
    }
}
