package crab.appcore.context;

/**
 * Lifecycle contract for application modules.
 *
 * Design patterns:
 * - Template Method style lifecycle: modules receive consistent lifecycle hooks.
 *
 * SOLID:
 * - Interface Segregation: keeps the module API focused and small.
 */
public interface GameModule {
    void initialize(GameContext context);

    void start();

    default void initializeUi() {
    }

    default void update(double tpf) {
    }

    void stop();
}
