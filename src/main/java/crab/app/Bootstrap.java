package crab.app;

import com.almasb.fxgl.app.GameSettings;
import crab.appcore.context.GameContext;
import crab.appcore.context.ModuleRegistry;
import crab.appcore.screen.ScreenManager;
import crab.features.devtools.DevToolsModule;
import crab.features.demo.DemoModule;
import crab.features.menu.MenuModule;
import crab.features.menu.presentation.screens.LoginScreen;
import javafx.scene.Cursor;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

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
    private final ScreenManager screens = new ScreenManager();
    private boolean modulesInitialized;

    public void configure(GameSettings settings) {
        settings.setWidth(1024);
        settings.setHeight(720);
        settings.setTitle("Crab Inc.");
        settings.setVersion("0.1");
        settings.setDeveloperMenuEnabled(true);
        settings.setIntroEnabled(false);
        settings.setSceneFactory(new CrabSceneFactory());
    }

    public void initializeGame() {
        if (!modulesInitialized) {
            context.register(ScreenManager.class, screens);
            modules.register(new MenuModule());
            modules.register(new DevToolsModule());
            modules.register(new DemoModule());
            modules.initialize(context);
            modulesInitialized = true;
        }

        modules.start();
    }

    public void initializeUi() {
        modules.initializeUi();
        getGameScene().setCursor(Cursor.DEFAULT);
        if (screens.currentId().isEmpty()) {
            screens.show(LoginScreen.ID);
        }
    }

    public void update(double tpf) {
        modules.update(tpf);
        screens.update(tpf);
    }

    public void shutdown() {
        screens.clear();
        modules.stop();
    }
}
