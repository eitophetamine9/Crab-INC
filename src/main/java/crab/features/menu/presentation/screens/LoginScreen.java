package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.menu.presentation.components.LoginScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Login entry screen for the existing FXGL application flow.
 *
 * Design patterns:
 * - State: represents the unauthenticated application state.
 *
 * SOLID:
 * - Single Responsibility: owns login presentation and validation only.
 */
public final class LoginScreen implements GameScreen {
    public static final String ID = "menu_login";

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    public LoginScreen(ScreenManager screens) {
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
        getGameScene().setBackgroundColor(Color.rgb(20, 33, 47));
        root = loadView();
        getGameScene().addUINode(root);
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        visible = false;
        if (root != null) {
            getGameScene().removeUINode(root);
            root = null;
        }
    }

    private Parent loadView() {
        URL resource = getClass().getResource("/fxml/menu/login-screen.fxml");
        if (resource == null) {
            return fallbackLabel("Missing /fxml/menu/login-screen.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent loadedRoot = loader.load();
            LoginScreenController controller = loader.getController();
            controller.setLoginSuccessAction(() -> screens.show(MainMenuScreen.ID));
            return loadedRoot;
        } catch (IOException exception) {
            return fallbackLabel("Login screen load failed: " + exception.getMessage());
        }
    }

    private static Label fallbackLabel(String message) {
        Label fallback = new Label(message);
        fallback.setTextFill(Color.WHITE);
        return fallback;
    }
}
