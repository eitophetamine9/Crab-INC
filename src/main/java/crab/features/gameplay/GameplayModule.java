package crab.features.gameplay;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.presentation.GameplayScreen;

/**
 * Registers screens that belong to the gameplay loop.
 */
public final class GameplayModule implements GameModule {
    @Override
    public void initialize(GameContext context) {
        ScreenManager screens = context.require(ScreenManager.class);
        screens.register(new GameplayScreen(screens));
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
