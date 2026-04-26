package crab.app;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;

/**
 * FXGL runtime adapter for Crab Inc.
 *
 * Design patterns:
 * - Adapter: maps FXGL lifecycle hooks to project-owned bootstrap lifecycle.
 *
 * SOLID:
 * - Single Responsibility: owns FXGL lifecycle integration only.
 * - Dependency Inversion: delegates project setup to {@link Bootstrap}.
 */
public final class Application extends GameApplication {
    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    protected void initSettings(GameSettings settings) {
        bootstrap.configure(settings);
    }

    @Override
    protected void initGame() {
        bootstrap.initializeGame();
    }

    @Override
    protected void initUI() {
        bootstrap.initializeUi();
    }

    @Override
    protected void onUpdate(double tpf) {
        bootstrap.update(tpf);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
