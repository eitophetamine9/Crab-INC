package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.demo.presentation.screens.BoxDemoScreen;
import crab.features.demo.presentation.screens.BunnyDemoScreen;
import crab.features.demo.presentation.screens.CrabDemoScreen;
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
        // MOVED CSS INJECTION TO createView() to avoid Java compile errors
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

    // --- Replace your createView method with this ---
    private Parent createView() {
        Label title = new Label("Crab Inc.");
        title.getStyleClass().add("title-text");
        title.setMouseTransparent(true);

        Label subtitle = new Label("Choose where to begin.");
        subtitle.getStyleClass().add("subtitle-text");
        subtitle.setMouseTransparent(true);

        // FIX: All calls MUST pass the 4th "rotation" parameter now
        Button redSquare = menuButton("Red Square", () -> screens.show(BoxDemoScreen.ID), "#14b8a6", -2.0);
        Button bunny = menuButton("Bunny Shader Demo", () -> screens.show(BunnyDemoScreen.ID), "#0ea5e9", 1.5);
        Button crab = menuButton("Crab Model Demo", () -> screens.show(CrabDemoScreen.ID), "#e11d48", 3.0);

        Button logout = menuButton("Log Out", () -> screens.show(LoginScreen.ID), "#64748b", 0.0);
        logout.setPrefWidth(140);

        Button exit = menuButton("Exit", () -> getGameController().exit(), "#334155", 0.0);
        exit.setPrefWidth(140);

<<<<<<< Updated upstream
=======
        Button play = menuButton("Play", () -> screens.show(SetupScreen.ID), "#10b981", 0.0);
        play.setPrefWidth(280);

>>>>>>> Stashed changes
        HBox secondaryActions = new HBox(15, logout, exit);
        secondaryActions.setAlignment(Pos.CENTER);

        VBox menu = new VBox(20, title, subtitle, redSquare, bunny, crab, secondaryActions);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(40));

        // CRITICAL: Set the size so the CSS panel background shows up!
        menu.setPrefWidth(PANEL_WIDTH);
        menu.setMaxWidth(PANEL_WIDTH);
        menu.getStyleClass().add("menu-panel");

        // Ensure CSS is loaded from the root resources
        String cssPath = "/assets/ui/cartoon-style.css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            menu.getStylesheets().add(resource.toExternalForm());
        }

        menu.setTranslateX((APP_WIDTH - PANEL_WIDTH) / 2.0);
        menu.setTranslateY((APP_HEIGHT - 500) / 2.0);
        return menu;
    }

    // --- Ensure your menuButton method looks like this ---
    private static Button menuButton(String text, Runnable action, String color, double rotation) {
        Button button = new Button(text);
        button.setPrefWidth(280);
        button.getStyleClass().add("menu-button");
        button.setRotate(rotation); // Base rotation

        // 🦀 THE WOBBLE TRICK: Add listeners for hover
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
            button.setRotate(rotation * -1.5); // Tilt the other way on hover!
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setRotate(rotation); // Return to base tilt
        });

        button.setStyle("-fx-background-color: linear-gradient(to bottom, %s, derive(%s, -30%%));".formatted(color, color));
        button.setOnAction(event -> action.run());
        return button;
    }
}