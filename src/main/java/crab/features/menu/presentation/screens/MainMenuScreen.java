package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.particle.ParticleSystem;
import javafx.geometry.Point2D;
import com.almasb.fxgl.core.math.FXGLMath;

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
    private static final double APP_WIDTH = 1080;
    private static final double APP_HEIGHT = 720;
    private static final double PANEL_WIDTH = 550;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ParticleSystem particleSystem;

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
        // Video Background Integration
        try {
            var resource = getClass().getResource("/assets/textures/underwater_scene.mp4");
            if (resource != null) {
                Media media = new Media(resource.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);

                mediaView = new MediaView(mediaPlayer);
                mediaView.setPreserveRatio(false);

                // Static sizing via DSL to ensure compilation in 17.3
                mediaView.setFitWidth(getAppWidth());
                mediaView.setFitHeight(getAppHeight());

                // Ensure it's behind UI
                getGameScene().getContentRoot().getChildren().add(0, mediaView);
                mediaPlayer.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        root = createView();
        
        // 3D Tilt for Titles
        root.lookupAll(".title-text").forEach(node -> {
            node.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
            node.setRotate(10);
        });

        getGameScene().addUINode(root);
        addBubbles();
    }

    private void addBubbles() {
        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
        emitter.setStartColor(Color.web("rgba(255, 255, 255, 0.3)"));
        emitter.setEndColor(Color.TRANSPARENT);
        emitter.setNumParticles(2);
        emitter.setEmissionRate(0.2);
        emitter.setSize(1, 4);
        emitter.setVelocityFunction(i -> new Point2D(FXGLMath.random(-1, 1), -FXGLMath.random(5, 15)));
        emitter.setAccelerationFunction(() -> new Point2D(0, -0.05));
        emitter.setSpawnPointFunction(i -> new Point2D(FXGLMath.random(0, getAppWidth()), getAppHeight()));
        
        this.particleSystem = new ParticleSystem();
        this.particleSystem.addParticleEmitter(emitter, 0, getAppHeight());
        // Add at index 1 (behind UI at index 2, in front of video at index 0)
        getGameScene().getContentRoot().getChildren().add(1, particleSystem.getPane());
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

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (mediaView != null) {
            getGameScene().getContentRoot().getChildren().remove(mediaView);
            mediaView = null;
        }
        if (particleSystem != null) {
            getGameScene().getContentRoot().getChildren().remove(particleSystem.getPane());
            particleSystem = null;
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

        Button logout = menuButton("Log Out", () -> screens.show(LoginScreen.ID), "btn-secondary", 0.0);
        logout.setPrefWidth(190);
        logout.setPrefHeight(60);

        Button exit = menuButton("Exit", () -> getGameController().exit(), "btn-secondary", 0.0);
        exit.setPrefWidth(190);
        exit.setPrefHeight(60);

        Button play = menuButton("Play", () -> screens.show(SetupScreen.ID), "btn-play", 0.0);
        play.setPrefWidth(400);
        play.setPrefHeight(60);

        String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
        String saveFileStr = crab.appcore.db.DatabaseManager.getSaveForUser(username);
        java.io.File saveFile = saveFileStr != null ? new java.io.File(saveFileStr) : null;
        
        Button continueBtn = menuButton("Continue", () -> {
            if (saveFile == null || !saveFile.exists()) return;
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(saveFile))) {
                crab.features.gameplay.presentation.GameplayScreen.loadedSession = (crab.features.gameplay.domain.GameSession) ois.readObject();
                screens.show(crab.features.gameplay.presentation.GameplayScreen.ID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "btn-auth", 0.0);
        continueBtn.setPrefWidth(400);
        continueBtn.setPrefHeight(60);

        HBox secondaryActions = new HBox(15, logout, exit);
        secondaryActions.setAlignment(Pos.CENTER);

        VBox menu = new VBox(30, title, subtitle, continueBtn, play, secondaryActions);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(20, 50, 50, 50));

        // CRITICAL: Set the size so the CSS panel background shows up!
        menu.setPrefWidth(PANEL_WIDTH);
        menu.setMaxWidth(PANEL_WIDTH);
        menu.getStyleClass().add("menu-panel");
        menu.setPrefHeight(620);
        menu.setMaxHeight(620);

        // Use StackPane for centering to avoid translation issues
        StackPane rootWrapper = new StackPane(menu);
        rootWrapper.setPrefSize(APP_WIDTH, APP_HEIGHT);
        rootWrapper.setAlignment(Pos.CENTER);

        // Ensure CSS is loaded from the root resources
        String cssPath = "/assets/ui/cartoon-style.css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            rootWrapper.getStylesheets().add(resource.toExternalForm());
        }

        return rootWrapper;
    }

    // --- Ensure your menuButton method looks like this ---
    private static Button menuButton(String text, Runnable action, String styleClass, double rotation) {
        Button button = new Button(text);
        button.setPrefWidth(400);
        button.setPrefHeight(60);
        button.getStyleClass().addAll("menu-button", styleClass);
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

        button.setOnAction(event -> action.run());
        return button;
    }
}