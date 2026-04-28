package crab.appcore.context;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates application module lifecycle order.
 *
 * Design patterns:
 * - Facade: exposes one lifecycle API for all registered modules.
 *
 * SOLID:
 * - Single Responsibility: owns module registration and lifecycle dispatch.
 */
public final class ModuleRegistry {
    private final List<GameModule> modules = new ArrayList<>();

    public void register(GameModule module) {
        modules.add(module);
    }

    public void initialize(GameContext context) {
        modules.forEach(module -> module.initialize(context));
    }

    public void start() {
        modules.forEach(GameModule::start);
    }

    public void initializeUi() {
        modules.forEach(GameModule::initializeUi);
    }

    public void update(double tpf) {
        modules.forEach(module -> module.update(tpf));
    }

    public void stop() {
        for (int i = modules.size() - 1; i >= 0; i--) {
            modules.get(i).stop();
        }
    }
}
