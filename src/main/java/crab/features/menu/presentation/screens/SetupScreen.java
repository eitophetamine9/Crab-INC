package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.presentation.GameplayScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
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

public final class SetupScreen implements GameScreen {
    public static final String ID = "setup";
    private static final double APP_WIDTH = 1080;
    private static final double APP_HEIGHT = 720;
    private static final double PANEL_WIDTH = 550;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    private javafx.scene.image.ImageView backgroundImageView;
    private ParticleSystem particleSystem;

    public SetupScreen(ScreenManager screens) {
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
        // GIF Background Integration
        try {
            var resource = getClass().getResource("/assets/textures/bg_underwater.gif");
            if (resource != null) {
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm()));
                backgroundImageView.setFitWidth(getAppWidth());
                backgroundImageView.setFitHeight(getAppHeight());
                backgroundImageView.setPreserveRatio(false);

                // Ensure it's behind UI
                getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
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

        if (backgroundImageView != null) {
            getGameScene().getContentRoot().getChildren().remove(backgroundImageView);
            backgroundImageView = null;
        }
        if (particleSystem != null) {
            getGameScene().getContentRoot().getChildren().remove(particleSystem.getPane());
            particleSystem = null;
        }
    }

    private Parent createView() {
        Label title = new Label("Game Setup");
        title.getStyleClass().add("title-text");
        
        Label subtitle = new Label("Select Number of Enemies:");
        subtitle.getStyleClass().add("subtitle-text");

        Slider enemySlider = new Slider(3, 7, 3);
        enemySlider.setShowTickLabels(true);
        enemySlider.setShowTickMarks(true);
        enemySlider.setMajorTickUnit(1);
        enemySlider.setMinorTickCount(0);
        enemySlider.setSnapToTicks(true);
        enemySlider.setPrefWidth(300);
        
        Label enemyCountLabel = new Label("3 Enemies");
        enemyCountLabel.getStyleClass().add("subtitle-text");
        enemySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            enemyCountLabel.setText(newVal.intValue() + " Enemies");
        });

        Button startBtn = new Button("Start Game");
        startBtn.getStyleClass().addAll("menu-button", "btn-play");
        startBtn.setPrefWidth(400);
        startBtn.setPrefHeight(60);
        startBtn.setOnAction(e -> {
            GameplayScreen.requestedEnemyCount = (int) enemySlider.getValue();
            screens.show(GameplayScreen.ID);
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        backBtn.setPrefWidth(400);
        backBtn.setPrefHeight(60);
        backBtn.setOnAction(e -> screens.show(MainMenuScreen.ID));

        VBox menu = new VBox(30, title, subtitle, enemySlider, enemyCountLabel, startBtn, backBtn);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(20, 50, 50, 50));
        menu.setPrefWidth(PANEL_WIDTH);
        menu.setMaxWidth(PANEL_WIDTH);
        menu.setPrefHeight(620);
        menu.setMaxHeight(620);
        menu.getStyleClass().add("menu-panel");

        // Use StackPane for centering
        StackPane rootWrapper = new StackPane(menu);
        rootWrapper.setPrefSize(APP_WIDTH, APP_HEIGHT);
        rootWrapper.setAlignment(Pos.CENTER);

        String cssPath = "/assets/ui/cartoon-style.css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            rootWrapper.getStylesheets().add(resource.toExternalForm());
        }

        return rootWrapper;
    }
}
