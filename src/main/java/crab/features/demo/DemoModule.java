package crab.features.demo;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.appcore.screen.ScreenManager;
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
    private boolean navigationBound;

    @Override
    public void initialize(GameContext context) {
        context.register(DemoModule.class, this);
        screens = context.require(ScreenManager.class);
        screens.register(new BoxDemoScreen(screens));
        screens.register(new BunnyDemoScreen(screens));
        screens.register(new CrabDemoScreen(screens));
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
        onKey(KeyCode.DIGIT1, () -> screens.show(BoxDemoScreen.ID));
        onKey(KeyCode.DIGIT2, () -> screens.show(BunnyDemoScreen.ID));
        onKey(KeyCode.DIGIT3, () -> screens.show(CrabDemoScreen.ID));
    }
}
