package crab.features.menu;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.appcore.screen.ScreenManager;
import crab.features.menu.presentation.screens.LoginScreen;
import crab.features.menu.presentation.screens.MainMenuScreen;

/**
 * Registers menu screens that belong to the application shell.
 *
 * Design patterns:
 * - Module: contributes login and menu screens to the shared screen registry.
 *
 * SOLID:
 * - Single Responsibility: owns menu feature registration only.
 */
public final class MenuModule implements GameModule {
    @Override
    public void initialize(GameContext context) {
        ScreenManager screens = context.require(ScreenManager.class);
        screens.register(new LoginScreen(screens));
        screens.register(new MainMenuScreen(screens));
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
