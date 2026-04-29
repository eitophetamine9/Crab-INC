package crab.features.demo;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.appcore.screen.ScreenManager;
import crab.features.devtools.DevToolsModule;
import crab.features.demo.presentation.screens.BoxDemoScreen;
import crab.features.demo.presentation.screens.BunnyDemoScreen;
import crab.features.demo.presentation.screens.CrabDemoScreen;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.onKey;

/**
 * Registers the foundation demo screen.
 *
 * Design patterns:
 * - Module: contributes one feature-owned screen to the application.
 *
 * SOLID:
 * - Single Responsibility: registers demo feature services/screens only.
 */
public final class DemoModule implements GameModule {
    private ScreenManager screens;
    private DevToolsModule devTools;
    private DemoInputRouter inputRouter;
    private boolean navigationBound;

    @Override
    public void initialize(GameContext context) {
        context.register(DemoModule.class, this);
        screens = context.require(ScreenManager.class);
        devTools = context.find(DevToolsModule.class).orElse(null);
        inputRouter = new DemoInputRouter(screens, this::isGameInputAllowed);

        BoxDemoScreen boxScreen = new BoxDemoScreen(screens);
        BunnyDemoScreen bunnyScreen = new BunnyDemoScreen(screens, devTools);
        CrabDemoScreen crabScreen = new CrabDemoScreen(screens, devTools);

        screens.register(boxScreen);
        screens.register(bunnyScreen);
        screens.register(crabScreen);
        inputRouter.register(boxScreen.id(), boxScreen);
        inputRouter.register(crabScreen.id(), crabScreen);
    }

    @Override
    public void start() {
        bindNavigationOnce();
    }

    @Override
    public void update(double tpf) {
    }

    @Override
    public void stop() {
    }

    private void bindNavigationOnce() {
        if (navigationBound) {
            return;
        }

        navigationBound = true;
        onKey(KeyCode.DIGIT1, () -> showScreenInGameMode(BoxDemoScreen.ID));
        onKey(KeyCode.DIGIT2, () -> showScreenInGameMode(BunnyDemoScreen.ID));
        onKey(KeyCode.DIGIT3, () -> showScreenInGameMode(CrabDemoScreen.ID));
        onKey(KeyCode.W, () -> inputRouter.moveUp());
        onKey(KeyCode.UP, () -> inputRouter.moveUp());
        onKey(KeyCode.S, () -> inputRouter.moveDown());
        onKey(KeyCode.DOWN, () -> inputRouter.moveDown());
        onKey(KeyCode.A, () -> inputRouter.moveLeft());
        onKey(KeyCode.LEFT, () -> inputRouter.moveLeft());
        onKey(KeyCode.D, () -> inputRouter.moveRight());
        onKey(KeyCode.RIGHT, () -> inputRouter.moveRight());
    }

    private void showScreenInGameMode(String screenId) {
        if (isGameInputAllowed()) {
            screens.show(screenId);
        }
    }

    private boolean isGameInputAllowed() {
        return devTools == null || !devTools.isEnabled();
    }
}
