package crab.features.gameplay.presentation;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;

import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.particle.ParticleSystem;
import javafx.geometry.Point2D;
import com.almasb.fxgl.core.math.FXGLMath;

import java.util.ArrayList;
import java.util.List;

/**
 * A beautiful premium encyclopedia scene for cards in Crab Inc.
 * Accessable via the settings pop-up, it displays descriptions, action types, rarities, and lore.
 */
public final class EncyclopediaScreen implements GameScreen {
    public static final String ID = "encyclopedia";
    public static String previousScreenId = "menu_main";

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    private javafx.scene.image.ImageView backgroundImageView;
    private ParticleSystem particleSystem;

    public EncyclopediaScreen(ScreenManager screens) {
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

        // Underwater GIF Background Integration
        try {
            var resource = getClass().getResource("/assets/textures/bg_underwater.gif");
            if (resource != null) {
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm(), true));
                backgroundImageView.setFitWidth(getAppWidth());
                backgroundImageView.setFitHeight(getAppHeight());
                backgroundImageView.setPreserveRatio(false);

                // Add background behind UI
                getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        root = createView();

        // 3D Tilt Effect on Title
        root.lookupAll(".title-text").forEach(node -> {
            node.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
            node.setRotate(8);
        });

        getGameScene().addUINode(root);
        addBubbles();
    }

    private void addBubbles() {
        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
        emitter.setStartColor(Color.web("rgba(255, 255, 255, 0.25)"));
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
        // Main Rounded Dark Box Container
        VBox menuContainer = new VBox(15);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setStyle(
                "-fx-background-color: rgba(13, 43, 62, 0.94); " +
                "-fx-background-radius: 25; " +
                "-fx-border-color: white; " +
                "-fx-border-width: 4; " +
                "-fx-border-radius: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 150, 255, 0.4), 20, 0, 0, 0); " +
                "-fx-padding: 20 40;");
        menuContainer.setPrefSize(980, 640);
        menuContainer.setMaxSize(980, 640);

        // Header Title
        Label title = new Label("CARD ENCYCLOPEDIA");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 42px;");

        // Split view container
        HBox splitBox = new HBox(30);
        splitBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(splitBox, Priority.ALWAYS);

        // --- LEFT SIDE: CARD SELECTOR GRID ---
        VBox leftBox = new VBox(10);
        leftBox.setAlignment(Pos.CENTER);
        leftBox.setPrefWidth(520);

        Label gridHeader = new Label("Browse Crab Deck");
        gridHeader.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 18px; -fx-text-fill: #60a5fa;");

        FlowPane cardGrid = new FlowPane();
        cardGrid.setHgap(12);
        cardGrid.setVgap(12);
        cardGrid.setPrefWrapLength(500);
        cardGrid.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(cardGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // --- RIGHT SIDE: DETAILED INSPECTOR PANEL ---
        VBox inspectorPanel = new VBox(12);
        inspectorPanel.setAlignment(Pos.CENTER);
        inspectorPanel.setPrefWidth(360);
        inspectorPanel.setPadding(new Insets(15));
        inspectorPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.45); -fx-background-radius: 20; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 2; -fx-border-radius: 20;");

        // Load the cards list
        List<ActionCard> deck = createDeckList();

        // Populate card views in grid
        for (ActionCard card : deck) {
            StackPane cardView = createCardView(card, inspectorPanel);
            cardGrid.getChildren().add(cardView);
        }

        // Initialize Inspector with the first card (Give)
        if (!deck.isEmpty()) {
            updateInspectorPanel(deck.get(0), inspectorPanel);
        }

        leftBox.getChildren().addAll(gridHeader, scrollPane);
        splitBox.getChildren().addAll(leftBox, inspectorPanel);

        // Back Button
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("menu-button", "btn-secondary");
        backBtn.setPrefWidth(220);
        backBtn.setPrefHeight(50);
        backBtn.setOnAction(e -> screens.show(previousScreenId));

        menuContainer.getChildren().addAll(title, splitBox, backBtn);

        StackPane rootWrapper = new StackPane(menuContainer);
        rootWrapper.setPrefSize(getAppWidth(), getAppHeight());
        rootWrapper.setAlignment(Pos.CENTER);

        // CSS Styling Load
        String cssPath = "/assets/ui/cartoon-style.css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            rootWrapper.getStylesheets().add(resource.toExternalForm());
        }

        return rootWrapper;
    }

    private List<ActionCard> createDeckList() {
        List<ActionCard> list = new ArrayList<>();
        // All 12 default gameplay cards
        list.add(new ActionCard("give_common", "Give", CardType.HELP, CardRarity.COMMON));
        list.add(new ActionCard("take_common", "Take", CardType.STEAL, CardRarity.COMMON));
        list.add(new ActionCard("sabotage_common", "Sabotage", CardType.SABOTAGE, CardRarity.COMMON));

        list.add(new ActionCard("give_uncommon", "Generous Give", CardType.HELP, CardRarity.UNCOMMON));
        list.add(new ActionCard("take_uncommon", "Snatch", CardType.STEAL, CardRarity.UNCOMMON));
        list.add(new ActionCard("sabotage_uncommon", "Scheme", CardType.SABOTAGE, CardRarity.UNCOMMON));

        list.add(new ActionCard("give_rare", "Gracious Give", CardType.HELP, CardRarity.RARE));
        list.add(new ActionCard("take_rare", "Heist", CardType.STEAL, CardRarity.RARE));
        list.add(new ActionCard("sabotage_rare", "Conspiracy", CardType.SABOTAGE, CardRarity.RARE));

        list.add(new ActionCard("give_signature", "Grand Gesture", CardType.SIGNATURE_ALTRUIST, CardRarity.SIGNATURE));
        list.add(new ActionCard("take_signature", "Grand Heist", CardType.SIGNATURE_OPPORTUNIST, CardRarity.SIGNATURE));
        list.add(new ActionCard("sabotage_signature", "Master Sabotage", CardType.SIGNATURE_SABOTEUR, CardRarity.SIGNATURE));
        return list;
    }

    private StackPane createCardView(ActionCard card, VBox inspectorPanel) {
        StackPane stack = new StackPane();
        stack.setUserData(card);
        stack.setPrefSize(90, 130);
        stack.setMaxSize(90, 130);
        stack.setCursor(javafx.scene.Cursor.HAND);

        // Art image
        ImageView artView = new ImageView();
        artView.setFitWidth(90);
        artView.setFitHeight(130);
        artView.setPreserveRatio(false);
        artView.setSmooth(true);
        String artPath = getCardArtPath(card);
        var artUrl = getClass().getResource(artPath);
        if (artUrl != null) {
            artView.setImage(new Image(artUrl.toExternalForm(), 90, 130, false, true));
        } else {
            var phUrl = getClass().getResource("/assets/card-art/placeholdercard.png");
            if (phUrl != null) artView.setImage(new Image(phUrl.toExternalForm(), 90, 130, false, true));
        }

        // Clip rounded corners
        Rectangle clip = new Rectangle(90, 130);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        artView.setClip(clip);

        // Bottom rarity banner overlay
        String rarityColor = getRarityColor(card.rarity());
        Label rarityBadge = new Label(card.rarity().name());
        rarityBadge.setStyle(
                "-fx-background-color: " + rarityColor + "cc; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 8px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 2 4; " +
                "-fx-background-radius: 0 0 6 6;");
        rarityBadge.setMaxWidth(Double.MAX_VALUE);
        rarityBadge.setAlignment(Pos.CENTER);
        StackPane.setAlignment(rarityBadge, Pos.BOTTOM_CENTER);

        // Top card name overlay
        Label nameBadge = new Label(card.name());
        nameBadge.setWrapText(true);
        nameBadge.setAlignment(Pos.CENTER);
        nameBadge.setStyle(
                "-fx-background-color: rgba(0,0,0,0.65); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 9px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 2 4; " +
                "-fx-background-radius: 6 6 0 0;");
        nameBadge.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(nameBadge, Pos.TOP_CENTER);

        stack.getChildren().addAll(artView, nameBadge, rarityBadge);

        // Initial shadow glow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(rarityColor));
        shadow.setRadius(7);
        shadow.setSpread(0.12);
        stack.setEffect(shadow);

        // Interactive Animations
        stack.setOnMouseEntered(e -> {
            stack.setScaleX(1.1);
            stack.setScaleY(1.1);
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#fbbf24")); // Glowing gold border on hover
            glow.setRadius(12);
            glow.setSpread(0.25);
            stack.setEffect(glow);
        });
        stack.setOnMouseExited(e -> {
            stack.setScaleX(1.0);
            stack.setScaleY(1.0);
            stack.setEffect(shadow);
        });

        // Click Handler: Select Card to Inspect
        stack.setOnMouseClicked(e -> updateInspectorPanel(card, inspectorPanel));

        return stack;
    }

    private void updateInspectorPanel(ActionCard card, VBox inspectorPanel) {
        inspectorPanel.getChildren().clear();

        // High resolution enlarged display card
        StackPane largeCard = new StackPane();
        largeCard.setPrefSize(140, 200);
        largeCard.setMaxSize(140, 200);

        ImageView artView = new ImageView();
        artView.setFitWidth(140);
        artView.setFitHeight(200);
        artView.setPreserveRatio(false);
        artView.setSmooth(true);
        String artPath = getCardArtPath(card);
        var artUrl = getClass().getResource(artPath);
        if (artUrl != null) {
            artView.setImage(new Image(artUrl.toExternalForm(), 140, 200, false, true));
        } else {
            var phUrl = getClass().getResource("/assets/card-art/placeholdercard.png");
            if (phUrl != null) artView.setImage(new Image(phUrl.toExternalForm(), 140, 200, false, true));
        }

        Rectangle clip = new Rectangle(140, 200);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        artView.setClip(clip);

        String rarityColor = getRarityColor(card.rarity());
        Label rarityBadge = new Label(card.rarity().name());
        rarityBadge.setStyle(
                "-fx-background-color: " + rarityColor + "ee; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 3 6; " +
                "-fx-background-radius: 0 0 8 8;");
        rarityBadge.setMaxWidth(Double.MAX_VALUE);
        rarityBadge.setAlignment(Pos.CENTER);
        StackPane.setAlignment(rarityBadge, Pos.BOTTOM_CENTER);

        Label nameBadge = new Label(card.name());
        nameBadge.setWrapText(true);
        nameBadge.setAlignment(Pos.CENTER);
        nameBadge.setStyle(
                "-fx-background-color: rgba(0,0,0,0.7); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 6; " +
                "-fx-background-radius: 8 8 0 0;");
        nameBadge.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(nameBadge, Pos.TOP_CENTER);

        largeCard.getChildren().addAll(artView, nameBadge, rarityBadge);

        DropShadow shadowGlow = new DropShadow();
        shadowGlow.setColor(Color.web(rarityColor));
        shadowGlow.setRadius(16);
        shadowGlow.setSpread(0.3);
        largeCard.setEffect(shadowGlow);

        // Card Metadata
        Label cardTitle = new Label(card.name());
        cardTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #fbbf24; -fx-font-family: 'Luckiest Guy';");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER);

        Label rarityLabel = new Label(card.rarity().name());
        rarityLabel.setStyle("-fx-background-color: " + rarityColor + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        String typeName = switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST -> "HELP";
            case STEAL, SIGNATURE_OPPORTUNIST -> "STEAL";
            case SABOTAGE, SIGNATURE_SABOTEUR -> "SABOTAGE";
        };
        String typeColor = switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST -> "#34d399";
            case STEAL, SIGNATURE_OPPORTUNIST -> "#3b82f6";
            case SABOTAGE, SIGNATURE_SABOTEUR -> "#f87171";
        };
        Label typeLabel = new Label(typeName);
        typeLabel.setStyle("-fx-background-color: " + typeColor + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        badges.getChildren().addAll(rarityLabel, typeLabel);

        // Dynamic Effect Description (derived dynamically!)
        Label descLabel = new Label(getCardEffectDescription(card));
        descLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-text-alignment: center; -fx-line-spacing: 2;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);

        // Thin Separator line
        Rectangle sep = new Rectangle(200, 1.5, Color.web("rgba(255,255,255,0.2)"));

        // Funny lore text
        Label loreLabel = new Label(getCardLore(card));
        loreLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-font-size: 11px; -fx-text-alignment: center;");
        loreLabel.setWrapText(true);
        loreLabel.setMaxWidth(280);

        inspectorPanel.getChildren().addAll(largeCard, cardTitle, badges, descLabel, sep, loreLabel);
    }

    private String getCardArtPath(ActionCard card) {
        String rarity = switch (card.rarity()) {
            case COMMON    -> "common";
            case UNCOMMON  -> "uncommon";
            case RARE      -> "rare";
            case SIGNATURE -> "signature";
        };
        return switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST     -> "/assets/card-art/altruist_"    + rarity + "_help.png";
            case STEAL, SIGNATURE_OPPORTUNIST -> "/assets/card-art/oppurtunist_" + rarity + "_steal.png";
            case SABOTAGE, SIGNATURE_SABOTEUR -> "/assets/card-art/sabotuer_"    + rarity + "_sabotage.png";
        };
    }

    private String getCardEffectDescription(ActionCard card) {
        double mult = card.rarity().multiplier();
        return switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST ->
                String.format("Help a target:\n+%d Rep (you)  +%d Clams (you)\n+%d Wealth (target)",
                        Math.round(40 * mult), Math.round(15 * mult), Math.round(30 * mult));
            case STEAL, SIGNATURE_OPPORTUNIST ->
                String.format("Steal from a target:\n+%d Wealth (you)  -%d Rep (you)\n-%d Wealth (target)",
                        Math.round(45 * mult), Math.round(10 * mult), Math.round(35 * mult));
            case SABOTAGE, SIGNATURE_SABOTEUR ->
                String.format("Sabotage a target:\n-%d Reputation (you)\nReduces target's gains by 50%%",
                        Math.round(50 * mult));
        };
    }

    private String getRarityColor(CardRarity rarity) {
        return switch (rarity) {
            case COMMON    -> "#9ca3af";
            case UNCOMMON  -> "#34d399";
            case RARE      -> "#60a5fa";
            case SIGNATURE -> "#fbbf24";
        };
    }

    private String getCardLore(ActionCard card) {
        return switch (card.id()) {
            case "give_common" -> "\"Share the wealth... mostly because you have to. A true altruist makes it look voluntary.\"";
            case "take_common" -> "\"What's yours is mine, and what's mine is mine. It's just simple crustacean accounting.\"";
            case "sabotage_common" -> "\"Throw some sand in their gears. A little muddy water goes a long way in disrupting the competition.\"";
            case "give_uncommon" -> "\"A larger gift that screams: 'Look how nice I am!' Boosts your reputation to the high heavens.\"";
            case "take_uncommon" -> "\"A quick claw-grab in the dark. They won't even notice their clams are gone until next round.\"";
            case "sabotage_uncommon" -> "\"Coordinate a minor inconvenience. Perfect for setting back high-flying competitors.\"";
            case "give_rare" -> "\"The ultimate philanthropic gesture. You're basically a saint of the sea floor.\"";
            case "take_rare" -> "\"A full-scale underwater robbery. High risk, maximum payout. The opportunist's dream.\"";
            case "sabotage_rare" -> "\"A masterclass in sabotage. Rally the local barnacles to completely halt their growth.\"";
            case "give_signature" -> "\"A legendary act of charity that will be sung about in coral reefs for generations.\"";
            case "take_signature" -> "\"The heist of the century. Clean out their vaults and leave only a signature bubble behind.\"";
            case "sabotage_signature" -> "\"A devastating blow to all rival operations. Cruel, calculated, and extremely effective.\"";
            default -> "A specialized crustacean strategy card of mysterious origin.";
        };
    }
}
