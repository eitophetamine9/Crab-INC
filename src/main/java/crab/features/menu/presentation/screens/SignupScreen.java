package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.menu.presentation.components.SignupScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

public final class SignupScreen implements GameScreen {
    public static final String ID = "menu_signup";

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    public SignupScreen(ScreenManager screens) {
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
        URL resource = getClass().getResource("/fxml/menu/signup-screen.fxml");
        if (resource == null) {
            return fallbackLabel("Missing /fxml/menu/signup-screen.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent loadedRoot = loader.load();
            SignupScreenController controller = loader.getController();
            controller.setBackToLoginAction(() -> screens.show(LoginScreen.ID));
            controller.setSignupSuccessHandler(message -> {
                LoginScreen.setPendingStatusMessage(message);
                screens.show(LoginScreen.ID);
            });
            return loadedRoot;
        } catch (IOException exception) {
            return fallbackLabel("Signup screen load failed: " + exception.getMessage());
        }
    }

    private static Label fallbackLabel(String message) {
        Label fallback = new Label(message);
        fallback.setTextFill(Color.WHITE);
        return fallback;
    }
}
