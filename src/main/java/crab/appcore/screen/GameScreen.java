package crab.appcore.screen;

/**
 * Lifecycle contract for named application screens.
 *
 * Design patterns:
 * - State: each screen represents one navigable application state.
 *
 * SOLID:
 * - Interface Segregation: keeps screen behavior small and focused.
 */
public interface GameScreen {
    String id();

    default void show() {
    }

    default void hide() {
    }

    default void update(double tpf) {
    }
}
