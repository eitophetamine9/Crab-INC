package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.presentation.GameplayScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.particle.ParticleSystem;
import javafx.geometry.Point2D;
import com.almasb.fxgl.core.math.FXGLMath;

import java.util.ArrayList;
import java.util.List;

public final class SetupScreen implements GameScreen {
    public static final String ID = "setup";
    private static final double APP_WIDTH = 1080;
    private static final double APP_HEIGHT = 720;
    private static final double PANEL_WIDTH = 550;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    private int selectedEnemyCount = 3;
    private String selectedDifficulty = "Medium";

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
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm(), true));
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
        
        // --- Enemy Count ---
        Label subtitle = new Label("No. of Enemies");
        subtitle.getStyleClass().add("subtitle-text");

        HBox enemyButtons = new HBox(10);
        enemyButtons.setAlignment(Pos.CENTER);
        List<Button> enemyBtnList = new ArrayList<>();
        for (int i = 3; i <= 7; i++) {
            int val = i;
            Button b = new Button(String.valueOf(i));
            b.getStyleClass().add("menu-button");
            b.setPrefSize(60, 60);
            if (val == selectedEnemyCount) b.getStyleClass().add("btn-active");
            
            b.setOnAction(e -> {
                selectedEnemyCount = val;
                enemyBtnList.forEach(btn -> btn.getStyleClass().remove("btn-active"));
                b.getStyleClass().add("btn-active");
            });
            enemyBtnList.add(b);
            enemyButtons.getChildren().add(b);
        }

        // --- Difficulty ---
        Label diffSubtitle = new Label("Difficulty");
        diffSubtitle.getStyleClass().add("subtitle-text");

        HBox diffButtons = new HBox(10);
        diffButtons.setAlignment(Pos.CENTER);
        List<Button> diffBtnList = new ArrayList<>();
        String[] difficulties = {"Easy", "Medium", "Hard"};
        for (String d : difficulties) {
            Button b = new Button(d);
            b.getStyleClass().add("menu-button");
            b.setPrefSize(140, 60);
            if (d.equals(selectedDifficulty)) b.getStyleClass().add("btn-active");
            
            b.setOnAction(e -> {
                selectedDifficulty = d;
                diffBtnList.forEach(btn -> btn.getStyleClass().remove("btn-active"));
                b.getStyleClass().add("btn-active");
            });
            diffBtnList.add(b);
            diffButtons.getChildren().add(b);
        }

        Button startBtn = new Button("Continue");
        startBtn.getStyleClass().addAll("menu-button", "btn-play");
        startBtn.setPrefWidth(450);
        startBtn.setPrefHeight(60);
        startBtn.setOnAction(e -> {
            GameplayScreen.requestedEnemyCount = selectedEnemyCount;
            GameplayScreen.difficulty = selectedDifficulty;
            screens.show(CharacterSelectionScreen.ID);
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        backBtn.setPrefWidth(450);
        backBtn.setPrefHeight(60);
        backBtn.setOnAction(e -> screens.show(MainMenuScreen.ID));

        VBox menu = new VBox(25, title, subtitle, enemyButtons, diffSubtitle, diffButtons, startBtn, backBtn);
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
