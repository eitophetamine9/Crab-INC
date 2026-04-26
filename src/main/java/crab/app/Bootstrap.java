package crab.app;

import com.almasb.fxgl.app.GameSettings;
import crab.appcore.context.GameContext;
import crab.appcore.context.ModuleRegistry;
import crab.features.demo.DemoModule;

/**
 * Builds the project-owned application foundation on top of FXGL.
 *
 * Design patterns:
 * - Facade: centralizes startup/shutdown for the application layer.
 *
 * SOLID:
 * - Single Responsibility: wires settings, context, and modules.
 * - Open/Closed: new modules can be registered without changing FXGL entrypoint code.
 */
public final class Bootstrap {
    private final GameContext context = new GameContext();
    private final ModuleRegistry modules = new ModuleRegistry();
    private boolean modulesInitialized;

    public void configure(GameSettings settings) {
        settings.setWidth(1024);
        settings.setHeight(720);
        settings.setTitle("Crab Inc.");
        settings.setVersion("0.1");
        settings.setDeveloperMenuEnabled(true);
    }

    public void initializeGame() {
        if (!modulesInitialized) {
            modules.register(new DemoModule());
            modules.initialize(context);
            modulesInitialized = true;
        }

        modules.start();
    }

    public void initializeUi() {
        modules.initializeUi();
    }

    public void update(double tpf) {
        modules.update(tpf);
    }

    public void shutdown() {
        modules.stop();
    }
}
