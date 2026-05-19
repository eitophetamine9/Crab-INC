package crab.app;

import com.almasb.fxgl.app.scene.StartupScene;
import javafx.scene.paint.Color;

/**
 * Empty startup scene that suppresses FXGL's default blue startup symbol.
 *
 * Design patterns:
 * - Null Object style scene: fulfills startup contract without extra presentation.
 *
 * SOLID:
 * - Single Responsibility: removes framework startup branding only.
 */
public final class BlankStartupScene extends StartupScene {
    public BlankStartupScene(int width, int height) {
        super(width, height);
        setBackgroundColor(Color.BLACK);
    }
}
