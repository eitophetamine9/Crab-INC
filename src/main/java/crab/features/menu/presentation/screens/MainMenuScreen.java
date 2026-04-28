package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.demo.presentation.screens.BoxDemoScreen;
import crab.features.demo.presentation.screens.BunnyDemoScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.getGameController;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Main menu screen for navigating into the existing demo/game surfaces.
 *
 * Design patterns:
 * - State: represents the authenticated application menu state.
 *
 * SOLID:
 * - Single Responsibility: owns top-level menu presentation and navigation.
 */
public final class MainMenuScreen implements GameScreen {
    public static final String ID = "menu_main";
    private static final double APP_WIDTH = 1024;
    private static final double APP_HEIGHT = 720;
    private static final double PANEL_WIDTH = 560;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    public MainMenuScreen(ScreenManager screens) {
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
        getGameScene().setBackgroundColor(Color.rgb(13, 22, 34));
        root = createView();
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

    private Parent createView() {
        Label title = new Label("Crab Inc.");
        title.setStyle("-fx-font-size: 44px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subtitle = new Label("Choose where to begin.");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #cbd5e1;");

        Button redSquare = menuButton("Red Square", () -> screens.show(BoxDemoScreen.ID), "#14b8a6");
        Button bunny = menuButton("Bunny Shader Demo", () -> screens.show(BunnyDemoScreen.ID), "#0ea5e9");
        Button logout = menuButton("Log Out", () -> screens.show(LoginScreen.ID), "#64748b");
        Button exit = menuButton("Exit", () -> getGameController().exit(), "#475569");

        HBox secondaryActions = new HBox(12, logout, exit);
        secondaryActions.setAlignment(Pos.CENTER);

        VBox menu = new VBox(16, title, subtitle, redSquare, bunny, secondaryActions);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(44));
        menu.setPrefWidth(PANEL_WIDTH);
        menu.setMaxWidth(PANEL_WIDTH);
        menu.setStyle("""
                -fx-background-color: rgba(15, 23, 42, 0.88);
                -fx-background-radius: 8;
                -fx-border-color: rgba(148, 163, 184, 0.35);
                -fx-border-radius: 8;
                -fx-border-width: 1;
                """);
        menu.setTranslateX((APP_WIDTH - PANEL_WIDTH) / 2.0);
        menu.setTranslateY((APP_HEIGHT - 360) / 2.0);
        return menu;
    }

    private static Button menuButton(String text, Runnable action, String color) {
        Button button = new Button(text);
        button.setPrefWidth(250);
        button.setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: 6;
                -fx-text-fill: white;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-padding: 10 18 10 18;
                -fx-cursor: hand;
                """.formatted(color));
        button.setOnAction(event -> action.run());
        return button;
    }
}
