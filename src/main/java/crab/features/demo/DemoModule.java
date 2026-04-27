package crab.features.demo;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.appcore.screen.ScreenManager;
import crab.features.demo.presentation.DemoScreen;

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
    @Override
    public void initialize(GameContext context) {
        context.register(DemoModule.class, this);
        context.require(ScreenManager.class).register(new DemoScreen());
    }

    @Override
    public void start() {
    }

    @Override
    public void update(double tpf) {
    }

    @Override
    public void stop() {
    }
}
