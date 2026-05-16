package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.PlayerClass;
import crab.features.gameplay.presentation.GameplayScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.particle.ParticleSystem;
import javafx.geometry.Point2D;
import com.almasb.fxgl.core.math.FXGLMath;

public final class CharacterSelectionScreen implements GameScreen {
    public static final String ID = "character_selection";
    private static final double APP_WIDTH = 1080;
    private static final double APP_HEIGHT = 720;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;
    private boolean currentIsMale = true;
    private HBox cardsContainer;

    private javafx.scene.image.ImageView backgroundImageView;
    private ParticleSystem particleSystem;

    public CharacterSelectionScreen(ScreenManager screens) {
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
        try {
            var resource = getClass().getResource("/assets/textures/bg_underwater.gif");
            if (resource != null) {
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm()));
                backgroundImageView.setFitWidth(getAppWidth());
                backgroundImageView.setFitHeight(getAppHeight());
                backgroundImageView.setPreserveRatio(false);
                getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        root = createView();
        
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
        Label title = new Label("Choose Your Class");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 56px;"); // Slightly smaller to save space
        
        Button genderSwapBtn = new Button("Gender: Male ♂");
        genderSwapBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        genderSwapBtn.setPrefWidth(250);
        genderSwapBtn.setOnAction(e -> {
            currentIsMale = !currentIsMale;
            genderSwapBtn.setText("Gender: " + (currentIsMale ? "Male ♂" : "Female ♀"));
            GameplayScreen.isMale = currentIsMale;
            refreshCards();
        });

        HBox classBox = new HBox(20);
        classBox.setAlignment(Pos.CENTER);
        classBox.setPadding(new Insets(20));
        this.cardsContainer = classBox;
        refreshCards();

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        backBtn.setPrefWidth(200);
        backBtn.setOnAction(e -> screens.show(SetupScreen.ID));

        VBox menu = new VBox(20, title, genderSwapBtn, classBox, backBtn);
        menu.setAlignment(Pos.CENTER);
        menu.getStyleClass().add("menu-panel");
        menu.setPadding(new Insets(20, 30, 20, 30));
        menu.setMaxWidth(950);
        menu.setPrefHeight(680); // Adjusted height to ensure everything fits

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

    private void refreshCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        cardsContainer.getChildren().addAll(
            createClassCard(PlayerClass.OPPORTUNIST, "Wealth", "Gain wealth from every interaction."),
            createClassCard(PlayerClass.ALTRUIST, "Reputation", "Help others and grow your fame."),
            createClassCard(PlayerClass.SABOTEUR, "Infamy", "Disrupt opponents to win.")
        );
    }

    private VBox createClassCard(PlayerClass pClass, String goal, String desc) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 20;");

        Label nameLbl = new Label(pClass.name());
        nameLbl.setWrapText(true);
        nameLbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        String baseName = switch (pClass) {
            case SABOTEUR -> "saboteur";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "opportunist";
        };
        
        ImageView imgView = new ImageView();
        try {
            String suffix = currentIsMale ? "m" : "fm";
            var res = getClass().getResource("/assets/humanoid-art/" + baseName + suffix + ".gif");
            if (res != null) {
                imgView.setImage(new Image(res.toExternalForm()));
            }
        } catch (Exception e) {}
        imgView.setFitWidth(135);
        imgView.setPreserveRatio(true);

        Label goalLbl = new Label(goal);
        goalLbl.setWrapText(true);
        goalLbl.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        descLbl.setStyle("-fx-text-fill: white; -fx-text-alignment: center; -fx-font-size: 14px; -fx-font-weight: normal;");
        descLbl.setMaxWidth(260);

        Button selectBtn = new Button("Select");
        selectBtn.getStyleClass().addAll("menu-button", "btn-play");
        selectBtn.setPrefWidth(150);
        selectBtn.setOnAction(e -> {
            GameplayScreen.selectedClass = pClass;
            screens.show(GameplayScreen.ID);
        });

        card.getChildren().addAll(nameLbl, imgView, goalLbl, descLbl, selectBtn);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20; -fx-border-color: #fbbf24; -fx-border-width: 3; -fx-border-radius: 20;");
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 20;");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }
}
