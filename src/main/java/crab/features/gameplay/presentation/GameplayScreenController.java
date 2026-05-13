package crab.features.gameplay.presentation;

import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.effect.BlendMode;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

public class GameplayScreenController {

    @FXML private AnchorPane mainLayout;
    @FXML private ImageView backgroundImageView;
    @FXML private Label topGoldLabel;
    @FXML private VBox battlefieldArea;
    @FXML private Label phasePromptLabel;
    @FXML private HBox heroArea;
    @FXML private Button genderBtn;
    @FXML private ImageView heroAvatarImage;
    @FXML private Label heroClassLabel;
    @FXML private Label heroNameLabel;
    @FXML private Label heroWealthLabel;
    @FXML private Label heroReputationLabel;
    @FXML private Label heroInfamyLabel;
    @FXML private HBox handArea;
    @FXML private Button endTurnBtn;
    @FXML private Label roundGemLabel;
    @FXML private StackPane enemyScrollContainer;
    @FXML private ScrollPane enemyScrollPane;

    private ScreenManager screens;
    private GameSession gameSession;
    private PlayerState humanPlayer;
    private List<PlayerState> aiPlayers;
    
    private ActionCard selectedCard;
    private ActionCard keptCardToSave;
    private ActionCard lastAiCardPlayed;
    
    private boolean isFemale = false;
    private VBox pauseMenu;

    private final Random random = new Random();

    private String getGoalString(PlayerClass playerClass) {
        return switch (playerClass) {
            case OPPORTUNIST -> "Goal: Wealth";
            case ALTRUIST -> "Goal: Reputation";
            case SABOTEUR -> "Goal: Infamy";
        };
    }

    private Image getHumanAvatarImage(PlayerClass playerClass, boolean female) {
        String path = switch (playerClass) {
            case SABOTEUR -> female ? "/assets/textures/saboteurfm.png" : "/assets/textures/saboteurm.png";
            case ALTRUIST -> female ? "/assets/textures/altruistfm.png" : "/assets/textures/altruistm.png";
            case OPPORTUNIST -> female ? "/assets/textures/opportunistfm.png" : "/assets/textures/opportunistm.png";
        };
        var res = getClass().getResource(path);
        if (res != null) {
            return new Image(res.toExternalForm());
        }
        return null;
    }

    private Image getCrabImage(PlayerClass playerClass) {
        String path = switch (playerClass) {
            case SABOTEUR -> "/assets/textures/saboteurcrab.png";
            case ALTRUIST -> "/assets/textures/altruistcrab.png";
            case OPPORTUNIST -> "/assets/textures/opportunistcrab.png";
        };
        var res = getClass().getResource(path);
        if (res != null) {
            return new Image(res.toExternalForm());
        }
        return null;
    }

    public void initData(GameSession gameSession, ScreenManager screens, PlayerState humanPlayer, List<PlayerState> aiPlayers) {
        this.gameSession = gameSession;
        this.screens = screens;
        this.humanPlayer = humanPlayer;
        this.aiPlayers = aiPlayers;

        var bgRes = getClass().getResource("/assets/textures/bg_beach.gif");
        if (bgRes != null) {
            backgroundImageView.setImage(new Image(bgRes.toExternalForm()));
        }
        backgroundImageView.fitWidthProperty().bind(mainLayout.widthProperty());
        backgroundImageView.fitHeightProperty().bind(mainLayout.heightProperty());

        genderBtn.setStyle("-fx-background-radius: 50em; -fx-min-width: 20px; -fx-min-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-background-color: linear-gradient(to bottom right, #3b82f6 50%, #ec4899 50%); -fx-cursor: hand;");
        genderBtn.setOnAction(e -> {
            isFemale = !isFemale;
            updateHeroAvatarImage();
        });

        if (enemyScrollContainer != null && enemyScrollPane != null) {
            Rectangle fadeMask = new Rectangle();
            fadeMask.widthProperty().bind(enemyScrollContainer.widthProperty());
            fadeMask.heightProperty().bind(enemyScrollContainer.heightProperty());
            fadeMask.setFill(new LinearGradient(
                    0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.TRANSPARENT),
                    new Stop(0.15, Color.WHITE),
                    new Stop(0.85, Color.WHITE),
                    new Stop(1, Color.TRANSPARENT)
            ));
            fadeMask.setBlendMode(BlendMode.MULTIPLY);
            
            Group blendGroup = new Group(enemyScrollPane, fadeMask);
            blendGroup.setBlendMode(BlendMode.SRC_OVER);
            
            enemyScrollContainer.getChildren().clear();
            enemyScrollContainer.getChildren().add(blendGroup);
            enemyScrollPane.prefWidthProperty().bind(enemyScrollContainer.widthProperty());
            enemyScrollPane.prefHeightProperty().bind(enemyScrollContainer.heightProperty());
        }

        updateHeroAvatarImage();
        updateUI();
    }

    private void updateHeroAvatarImage() {
        if (humanPlayer != null) {
            Image heroImg = getHumanAvatarImage(humanPlayer.playerClass(), isFemale);
            if (heroImg != null) {
                heroAvatarImage.setImage(heroImg);
            }
        }
    }

    @FXML
    void handleSettings(ActionEvent event) {
        if (pauseMenu != null) {
            return; // Already paused
        }

        Label pauseLabel = new Label("PAUSED");
        pauseLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button continueBtn = new Button("Continue");
        continueBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        continueBtn.setOnAction(e -> {
            mainLayout.getChildren().remove(pauseMenu);
            pauseMenu = null;
        });

        Button saveBtn = new Button("Save and Return to Menu");
        saveBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-text-fill: #10b981;");
        saveBtn.setOnAction(e -> {
            String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
            String saveFileName = "savegame_" + username + ".dat";
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(saveFileName))) {
                oos.writeObject(gameSession);
                crab.appcore.db.DatabaseManager.registerSave(username, saveFileName);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mainLayout.getChildren().remove(pauseMenu);
            pauseMenu = null;
            screens.show("menu_main");
        });

        Button withdrawBtn = new Button("Withdraw");
        withdrawBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        withdrawBtn.setOnAction(e -> {
            pauseMenu.getChildren().clear();

            Label confirmLabel = new Label("Are you sure you want to withdraw?");
            confirmLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Button yesBtn = new Button("Yes, Withdraw");
            yesBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-text-fill: red;");
            yesBtn.setOnAction(yesEvent -> {
                mainLayout.getChildren().remove(pauseMenu);
                pauseMenu = null;
                screens.show("menu_main");
            });

            Button noBtn = new Button("No, Cancel");
            noBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
            noBtn.setOnAction(noEvent -> {
                pauseMenu.getChildren().clear();
                pauseMenu.getChildren().addAll(pauseLabel, continueBtn, saveBtn, withdrawBtn);
            });

            HBox buttonBox = new HBox(20, yesBtn, noBtn);
            buttonBox.setAlignment(Pos.CENTER);

            pauseMenu.getChildren().addAll(confirmLabel, buttonBox);
        });

        pauseMenu = new VBox(20, pauseLabel, continueBtn, saveBtn, withdrawBtn);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setStyle("-fx-border-color: black; -fx-border-width: 4; -fx-padding: 40; -fx-background-color: white;");
        
        mainLayout.getChildren().add(pauseMenu);
        AnchorPane.setTopAnchor(pauseMenu, 200.0);
        AnchorPane.setLeftAnchor(pauseMenu, 300.0);
        AnchorPane.setRightAnchor(pauseMenu, 300.0);
    }

    private void updateUI() {
        if (gameSession == null) return;

        topGoldLabel.setText("Gold: " + humanPlayer.gold());
        roundGemLabel.setText("Round: " + gameSession.currentRound());

        battlefieldArea.getChildren().clear();
        
        int totalEnemies = aiPlayers.size();
        int topCount = (int) Math.ceil(totalEnemies / 2.0);
        
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER);
        HBox bottomRow = new HBox(15);
        bottomRow.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < totalEnemies; i++) {
            if (i < topCount) {
                topRow.getChildren().add(createBattlefieldEntityView(aiPlayers.get(i), false));
            } else {
                bottomRow.getChildren().add(createBattlefieldEntityView(aiPlayers.get(i), false));
            }
        }
        
        battlefieldArea.getChildren().add(topRow);
        if (!bottomRow.getChildren().isEmpty()) {
            battlefieldArea.getChildren().add(bottomRow);
        }
        
        heroClassLabel.setText(humanPlayer.playerClass().name() + "\n(" + getGoalString(humanPlayer.playerClass()) + ")");
        heroNameLabel.setText(humanPlayer.displayName() + " (You)");
        heroWealthLabel.setText("Wealth: " + humanPlayer.wealth());
        heroReputationLabel.setText("Reputation: " + humanPlayer.reputation());
        heroInfamyLabel.setText("Infamy: " + humanPlayer.infamy());

        handArea.getChildren().clear();
        selectedCard = null;
        keptCardToSave = null;

        endTurnBtn.setDisable(true);
        endTurnBtn.setOnAction(null);

        switch (gameSession.phase()) {
            case DEVELOPMENT -> {
                Map<String, Integer> selections = gameSession.players().stream()
                        .collect(Collectors.toMap(PlayerState::id, p -> 0));
                gameSession.resolveDevelopment(selections, java.util.Set.of());
                updateUI();
            }
            case ACTION -> showActionUI();
            case RESOLUTION -> showResolutionUI();
            case EVENT -> {
                phasePromptLabel.setText("Resolving Events...");
                gameSession.applyEvent(GameEvent.none());
                updateUI();
            }
            case ROUND_COMPLETE -> {
                gameSession.completeRound();
                updateUI();
            }
            case GAME_OVER -> showGameOverUI();
        }
    }

    private VBox createBattlefieldEntityView(PlayerState player, boolean isHuman) {
        // Container for Crab + Stats
        VBox container = new VBox(5);
        container.setAlignment(Pos.CENTER);
        
        // 1. Crab Image (No background)
        ImageView crabAvatar = new ImageView();
        crabAvatar.setFitWidth(100);
        crabAvatar.setFitHeight(80);
        crabAvatar.setPreserveRatio(true);
        Image img = getCrabImage(player.playerClass());
        if (img != null) {
            crabAvatar.setImage(img);
        }
        
        // 2. Stats Box (Has background)
        VBox statsBox = new VBox(2);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPrefWidth(120);
        statsBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: " + (isHuman ? "#10b981" : "white") + "; -fx-border-width: 2; -fx-padding: 5;");
        
        Label pClass = new Label(player.playerClass().name());
        pClass.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        Label pName = new Label(player.displayName() + (isHuman ? " (You)" : ""));
        pName.setStyle("-fx-text-fill: " + (isHuman ? "#10b981" : "gold") + "; -fx-font-weight: bold;");
        Label pWealth = new Label("Wealth: " + player.wealth());
        pWealth.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        Label pRep = new Label("Reputation: " + player.reputation());
        pRep.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        Label pInfamy = new Label("Infamy: " + player.infamy());
        pInfamy.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        
        statsBox.getChildren().addAll(pClass, pName, pWealth, pRep, pInfamy);
        
        container.getChildren().addAll(crabAvatar, statsBox);

        if (gameSession.phase() == GamePhase.RESOLUTION) {
            PlayerAction action = gameSession.pendingActions().get(player.id());
            if (action != null && action.targetPlayerId() != null) {
                String targetName = gameSession.players().stream()
                        .filter(p -> p.id().equals(action.targetPlayerId()))
                        .map(PlayerState::displayName)
                        .findFirst()
                        .orElse(action.targetPlayerId());
                Label actionLabel = new Label("Played: " + action.card().name() + "\nTargeted: " + targetName);
                actionLabel.setStyle("-fx-text-fill: #facc15; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center; -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 3; -fx-background-radius: 5;");
                actionLabel.setWrapText(true);
                actionLabel.setAlignment(Pos.CENTER);
                container.getChildren().add(actionLabel);
            }
        }

        // Targeting logic
        container.setOnMouseClicked(e -> {
            if (gameSession.phase() == GamePhase.ACTION && selectedCard != null) {
                gameSession.submitAction(new PlayerAction(humanPlayer.id(), selectedCard, player.id(), keptCardToSave));
                submitAiActions();
                updateUI();
            }
        });

        // Hover effect to show it's clickable
        container.setOnMouseEntered(e -> {
            if (gameSession.phase() == GamePhase.ACTION && selectedCard != null) {
                statsBox.setStyle("-fx-background-color: rgba(200,50,50,0.8); -fx-border-color: red; -fx-border-width: 3; -fx-padding: 4;");
            }
        });
        container.setOnMouseExited(e -> {
            statsBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: " + (isHuman ? "#10b981" : "white") + "; -fx-border-width: 2; -fx-padding: 5;");
        });

        return container;
    }

    private void showActionUI() {
        phasePromptLabel.setText("Phase: Select Card, Right-Click to Keep 1, then Target");

        endTurnBtn.setDisable(false);
        endTurnBtn.setText("PASS");
        endTurnBtn.setOnAction(e -> {
            gameSession.submitAction(new PlayerAction(humanPlayer.id(), new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON), aiPlayers.get(0).id(), keptCardToSave));
            submitAiActions();
            updateUI();
        });

        List<ActionCard> hand = new java.util.ArrayList<>(humanPlayer.hand());
        hand.sort(java.util.Comparator.comparingInt(c -> java.util.List.of("Take", "Give", "Share").indexOf(c.name())));
        
        for (ActionCard card : hand) {
            StackPane cardView = createHandCardView(card);
            handArea.getChildren().add(cardView);
        }
    }

    private void submitAiActions() {
        List<PlayerState> allPlayers = gameSession.players();
        for (PlayerState ai : aiPlayers) {
            ActionCard keptCard = ai.hand().isEmpty() ? null : ai.hand().get(random.nextInt(ai.hand().size()));
            
            if (ai.hand().isEmpty()) {
                lastAiCardPlayed = new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON);
                gameSession.submitAction(new PlayerAction(ai.id(), lastAiCardPlayed, humanPlayer.id(), null));
            } else {
                if (random.nextInt(10) == 0) {
                    gameSession.submitAction(new PlayerAction(ai.id(), new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON), humanPlayer.id(), keptCard));
                    continue;
                }
                ActionCard aiCard = ai.hand().get(random.nextInt(ai.hand().size()));
                lastAiCardPlayed = aiCard;
                PlayerState target = allPlayers.get(random.nextInt(allPlayers.size()));
                gameSession.submitAction(new PlayerAction(ai.id(), aiCard, target.id(), keptCard));
            }
        }
    }

    private void showResolutionUI() {
        phasePromptLabel.setText("Phase: Resolution");

        endTurnBtn.setDisable(false);
        endTurnBtn.setText("RESOLVE");
        endTurnBtn.setOnAction(e -> {
            gameSession.resolveActions();
            lastAiCardPlayed = null;
            updateUI();
        });
    }

    /** Returns the path under /assets/card-art/ for a given card. */
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

    /** Human-readable effect preview string for tooltip. */
    private String getCardEffectDescription(ActionCard card) {
        double mult = card.rarity().multiplier();
        return switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST ->
                String.format("Help a target:\n+%d Rep (you)  +%d Gold (you)\n+%d Wealth (target)",
                        Math.round(40 * mult), Math.round(15 * mult), Math.round(30 * mult));
            case STEAL, SIGNATURE_OPPORTUNIST ->
                String.format("Steal from a target:\n+%d Wealth (you)  -%d Rep (you)\n-%d Gold (target)",
                        Math.round(45 * mult), Math.round(10 * mult), Math.round(35 * mult));
            case SABOTAGE, SIGNATURE_SABOTEUR ->
                String.format("Sabotage a target:\n+%d Infamy (you)\nReduces target's gains by 50%%",
                        Math.round(50 * mult));
        };
    }

    /** Rarity accent color for the card border / banner. */
    private String getRarityColor(CardRarity rarity) {
        return switch (rarity) {
            case COMMON    -> "#9ca3af"; // grey
            case UNCOMMON  -> "#34d399"; // green
            case RARE      -> "#60a5fa"; // blue
            case SIGNATURE -> "#fbbf24"; // gold
        };
    }

    private void updateHandVisuals() {
        for (javafx.scene.Node node : handArea.getChildren()) {
            StackPane stack = (StackPane) node;
            ActionCard c = (ActionCard) stack.getUserData();
            ImageView artView = (ImageView) stack.lookup("#cardArt_" + c.id());
            Label keepLabel  = (Label)     stack.lookup("#keepLabel_" + c.id());

            boolean isSelected = (c == selectedCard);
            boolean isKept     = (c == keptCardToSave);

            // Glow / drop-shadow
            DropShadow glow = new DropShadow();
            if (isSelected) {
                glow.setColor(Color.web("#fbbf24"));
                glow.setRadius(22);
                glow.setSpread(0.4);
                stack.setScaleX(1.12);
                stack.setScaleY(1.12);
            } else {
                glow.setColor(Color.web(getRarityColor(c.rarity())));
                glow.setRadius(8);
                glow.setSpread(0.1);
                stack.setScaleX(1.0);
                stack.setScaleY(1.0);
            }
            stack.setEffect(glow);

            if (keepLabel != null) {
                keepLabel.setVisible(isKept);
            }
        }
    }

    private StackPane createHandCardView(ActionCard card) {
        StackPane stack = new StackPane();
        stack.setUserData(card);
        stack.setPrefSize(108, 155);
        stack.setMaxSize(108, 155);

        // ---- Art image ----
        ImageView artView = new ImageView();
        artView.setId("cardArt_" + card.id());
        artView.setFitWidth(108);
        artView.setFitHeight(155);
        artView.setPreserveRatio(false);
        artView.setSmooth(true);
        String artPath = getCardArtPath(card);
        var res = getClass().getResource(artPath);
        if (res != null) {
            artView.setImage(new Image(res.toExternalForm()));
        } else {
            // Fallback to placeholder
            var ph = getClass().getResource("/assets/card-art/placeholdercard.png");
            if (ph != null) artView.setImage(new Image(ph.toExternalForm()));
        }

        // Clip corners
        Rectangle clip = new Rectangle(108, 155);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        artView.setClip(clip);

        // ---- Rarity banner overlay (bottom strip) ----
        String rarityColor = getRarityColor(card.rarity());
        Label rarityBadge = new Label(card.rarity().name());
        rarityBadge.setStyle(
                "-fx-background-color: " + rarityColor + "cc; " +
                "-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; " +
                "-fx-padding: 2 6; -fx-background-radius: 0 0 7 7;");
        rarityBadge.setMaxWidth(Double.MAX_VALUE);
        rarityBadge.setAlignment(Pos.CENTER);
        StackPane.setAlignment(rarityBadge, Pos.BOTTOM_CENTER);

        // ---- Card name overlay (top strip) ----
        Label nameBadge = new Label(card.name());
        nameBadge.setWrapText(true);
        nameBadge.setAlignment(Pos.CENTER);
        nameBadge.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55); " +
                "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-padding: 3 5; -fx-background-radius: 7 7 0 0;");
        nameBadge.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(nameBadge, Pos.TOP_CENTER);

        // ---- Keep badge ----
        Label keepLabel = new Label("Ⓚ");
        keepLabel.setId("keepLabel_" + card.id());
        keepLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #10b981; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 10; -fx-padding: 1 4;");
        keepLabel.setVisible(false);
        StackPane.setAlignment(keepLabel, Pos.TOP_RIGHT);

        stack.getChildren().addAll(artView, nameBadge, rarityBadge, keepLabel);

        // Initial rarity glow
        DropShadow initGlow = new DropShadow();
        initGlow.setColor(Color.web(rarityColor));
        initGlow.setRadius(8);
        initGlow.setSpread(0.1);
        stack.setEffect(initGlow);

        // ---- Tooltip with effect description ----
        Tooltip tip = new Tooltip(getCardEffectDescription(card));
        tip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(stack, tip);

        // ---- Hover: lift the card ----
        TranslateTransition hoverUp   = new TranslateTransition(Duration.millis(120), stack);
        TranslateTransition hoverDown = new TranslateTransition(Duration.millis(120), stack);
        stack.setOnMouseEntered(e -> {
            hoverDown.stop();
            hoverUp.setToY(-12);
            hoverUp.play();
        });
        stack.setOnMouseExited(e -> {
            hoverUp.stop();
            hoverDown.setToY(0);
            hoverDown.play();
        });

        // ---- Click handler ----
        stack.setOnMouseClicked(e -> {
            if (gameSession.phase() == GamePhase.ACTION) {
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Right-click: toggle keep
                    keptCardToSave = (keptCardToSave == card) ? null : card;
                    updateHandVisuals();
                } else {
                    // Left-click: select / deselect
                    if (selectedCard == card) {
                        selectedCard = null;
                        phasePromptLabel.setText("Phase: Select Card, Right-Click to Keep 1, then Target");
                    } else {
                        selectedCard = card;
                        phasePromptLabel.setText("Target an Enemy!");
                    }
                    updateHandVisuals();
                }
            }
        });

        return stack;
    }

    private void showGameOverUI() {
        phasePromptLabel.setText("GAME OVER");
        
        Label winnerLabel = new Label(gameSession.winner()
            .map(w -> "Winner: " + w.playerId() + "!")
            .orElse("It's a tie!"));
        winnerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button returnBtn = new Button("Return to Menu");
        returnBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        returnBtn.setOnAction(e -> screens.show("menu_main"));

        VBox overBox = new VBox(20, winnerLabel, returnBtn);
        overBox.setAlignment(Pos.CENTER);
        overBox.setStyle("-fx-border-color: black; -fx-border-width: 4; -fx-padding: 40; -fx-background-color: white;");
        
        mainLayout.getChildren().add(overBox);
        AnchorPane.setTopAnchor(overBox, 200.0);
        AnchorPane.setLeftAnchor(overBox, 300.0);
        AnchorPane.setRightAnchor(overBox, 300.0);
    }
}
