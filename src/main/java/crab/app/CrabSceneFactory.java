package crab.app;

import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.app.scene.StartupScene;

/**
 * Project-owned scene factory overrides FXGL's default startup presentation.
 *
 * Design patterns:
 * - Factory Method: supplies framework scenes with project-specific replacements.
 *
 * SOLID:
 * - Single Responsibility: customizes framework-owned scene creation only.
 */
public final class CrabSceneFactory extends SceneFactory {
    @Override
    public StartupScene newStartup(int width, int height) {
        // Override FXGL's default startup scene so the app boots without framework branding.
        return new BlankStartupScene(width, height);
    }
}
