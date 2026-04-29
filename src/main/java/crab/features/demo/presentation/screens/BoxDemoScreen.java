package crab.features.demo.presentation.screens;

import com.almasb.fxgl.entity.Entity;
import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.demo.presentation.components.DemoNavigatorController;
import crab.features.menu.presentation.screens.MainMenuScreen;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyCode;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.net.URL;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.onKey;

/**
 * Demo screen focused only on the centered red square.
 *
 * Design patterns:
 * - State: one navigable screen implementation in the application flow.
 *
 * SOLID:
 * - Single Responsibility: owns only the red-square demo presentation.
 */
public final class BoxDemoScreen implements GameScreen {
    public static final String ID = "demo_box";
    private static final double APP_WIDTH = 1024;
    private static final double APP_HEIGHT = 720;
    private static final double NAVIGATOR_WIDTH = 548;
    private static final double BOX_SIZE = 96;

    private final ScreenManager screens;
    private Entity sampleEntity;
    private Parent navigator;
    private boolean controlsBound;
    private boolean visible;

    public BoxDemoScreen(ScreenManager screens) {
        this.screens = screens;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void show() {
        if (visible) {
            return;
        }

        visible = true;
        getGameScene().setBackgroundColor(Color.rgb(16, 22, 31));
        sampleEntity = entityBuilder()
                .at((APP_WIDTH - BOX_SIZE) / 2.0, (APP_HEIGHT - BOX_SIZE) / 2.0)
                .view(new Rectangle(BOX_SIZE, BOX_SIZE, Color.CRIMSON))
                .buildAndAttach();
        navigator = loadNavigator();
        getGameScene().addUINode(navigator);
        bindControlsOnce();
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        visible = false;
        if (sampleEntity != null) {
            sampleEntity.removeFromWorld();
            sampleEntity = null;
        }

        if (navigator != null) {
            getGameScene().removeUINode(navigator);
            navigator = null;
        }
    }

    private Parent loadNavigator() {
        URL resource = getClass().getResource("/fxml/components/demo-navigator.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/components/demo-navigator.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            DemoNavigatorController controller = loader.getController();
            controller.setNavigationActions(
                    () -> screens.show(BoxDemoScreen.ID),
                    () -> screens.show(BunnyDemoScreen.ID),
                    () -> screens.show(CrabDemoScreen.ID),
                    () -> screens.show(MainMenuScreen.ID)
            );
            controller.setActiveScreen(ID);
            root.setTranslateX((APP_WIDTH - NAVIGATOR_WIDTH) / 2.0);
            root.setTranslateY(APP_HEIGHT - 72);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Navigator load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    private void bindControlsOnce() {
        if (controlsBound) {
            return;
        }

        controlsBound = true;
        // Use FXGL's held-key input binding here instead of GameScreen.update(tpf),
        // since direct movement input is simpler and clearer at this screen level.
        onKey(KeyCode.W, () -> moveSquare(0, -8));
        onKey(KeyCode.S, () -> moveSquare(0, 8));
        onKey(KeyCode.A, () -> moveSquare(-8, 0));
        onKey(KeyCode.D, () -> moveSquare(8, 0));
    }

    private void moveSquare(double x, double y) {
        if (sampleEntity != null) {
            sampleEntity.translateX(x);
            sampleEntity.translateY(y);
        }
    }
}
