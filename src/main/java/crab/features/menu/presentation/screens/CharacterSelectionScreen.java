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
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm(), true));
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
        title.setStyle("-fx-font-size: 48px;"); // Slightly smaller to save space and center Back button
        
        Button genderSwapBtn = new Button("Gender: Male ♂");
        genderSwapBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        genderSwapBtn.setPrefWidth(250);
        genderSwapBtn.setOnAction(e -> {
            currentIsMale = !currentIsMale;
            genderSwapBtn.setText("Gender: " + (currentIsMale ? "Male ♂" : "Female ♀"));
            GameplayScreen.isMale = currentIsMale;
            refreshCards();
        });

        HBox classBox = new HBox(15); // Tighter card layout spacing
        classBox.setAlignment(Pos.CENTER);
        classBox.setPadding(new Insets(10));
        this.cardsContainer = classBox;
        refreshCards();

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        backBtn.setPrefWidth(200);
        backBtn.setOnAction(e -> screens.show(SetupScreen.ID));

        VBox menu = new VBox(15, title, genderSwapBtn, classBox, backBtn); // Tighter spacing to guarantee Back button fits
        menu.setAlignment(Pos.TOP_CENTER); // Align elements to top to pull layout up and use top space perfectly
        menu.getStyleClass().add("menu-panel-wide"); // Use wide panel to ensure Back button centers perfectly without clipping
        menu.setPadding(new Insets(30, 30, 20, 30)); // Perfect, balanced padding
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
            createClassCard(PlayerClass.OPPORTUNIST, "Goal: 1000 Wealth", "Gain wealth from every interaction."),
            createClassCard(PlayerClass.ALTRUIST, "Goal: 1000 Reputation", "Help others and grow your fame."),
            createClassCard(PlayerClass.SABOTEUR, "Goal: -1000 Reputation", "Disrupt opponents to win.")
        );
    }

    private VBox createClassCard(PlayerClass pClass, String goal, String desc) {
        VBox card = new VBox(6); // Tighter spacing inside cards
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8)); // Tighter padding inside cards
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 18; -fx-background-insets: 2; -fx-border-color: white; -fx-border-width: 2; -fx-border-insets: 1; -fx-border-radius: 20;");

        javafx.scene.text.Text nameLbl = new javafx.scene.text.Text(pClass.name());
        nameLbl.setFont(javafx.scene.text.Font.font("Luckiest Guy", 16)); // Compact size to fit OPPORTUNIST on single line
        nameLbl.setWrappingWidth(260);
        nameLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLbl.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        nameLbl.setStyle("-fx-fill: white; -fx-stroke: #0d2b3e; -fx-stroke-width: 1.0px;");
        nameLbl.setCache(true);
        nameLbl.setCacheHint(javafx.scene.CacheHint.SPEED);

        String baseName = switch (pClass) {
            case SABOTEUR -> "saboteur";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "opportunist";
        };
        
        ImageView imgView = new ImageView();
        imgView.setFitWidth(135);
        imgView.setFitHeight(170); // Strictly reserve height in layout before async image populates
        imgView.setPreserveRatio(true);
        try {
            String suffix = currentIsMale ? "m" : "fm";
            String path = "/assets/humanoid-art/" + baseName + suffix + ".gif";
            String slotId = "select_card_" + baseName + "_" + suffix;
            // Retrieve via Flyweight Factory asynchronously, downsampled to 135x170
            Image img = crab.appcore.concurrent.AssetFlyweightFactory.getSharedImageAsync(slotId, path, 135, 170);
            if (img != null) {
                imgView.setImage(img);
            }
        } catch (Exception e) {}

        javafx.scene.text.Text goalLbl = new javafx.scene.text.Text(goal);
        goalLbl.setFont(javafx.scene.text.Font.font("Luckiest Guy", 13)); // Sized down to save space
        goalLbl.setWrappingWidth(260);
        goalLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        goalLbl.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        goalLbl.setStyle("-fx-fill: #fbbf24; -fx-stroke: #0d2b3e; -fx-stroke-width: 1.0px;");
        goalLbl.setCache(true);
        goalLbl.setCacheHint(javafx.scene.CacheHint.SPEED);

        javafx.scene.text.Text descLbl = new javafx.scene.text.Text(desc);
        descLbl.setFont(javafx.scene.text.Font.font("Arial Rounded MT Bold", javafx.scene.text.FontWeight.BOLD, 11)); // Sized down to save space
        descLbl.setWrappingWidth(240);
        descLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLbl.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        descLbl.setStyle("-fx-fill: white; -fx-stroke: #0d2b3e; -fx-stroke-width: 0.8px;");
        descLbl.setCache(true);
        descLbl.setCacheHint(javafx.scene.CacheHint.SPEED);

        Button selectBtn = new Button("Select");
        selectBtn.getStyleClass().addAll("menu-button", "btn-play");
        selectBtn.setPrefWidth(150);
        selectBtn.setOnAction(e -> {
            GameplayScreen.selectedClass = pClass;
            screens.show(GameplayScreen.ID);
        });

        card.getChildren().addAll(nameLbl, imgView, goalLbl, descLbl, selectBtn);
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);

        // Hover effects removed to prevent card items scaling/overlapping

        return card;
    }
}
