package crab.features.gameplay.presentation;

import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;
import crab.features.menu.presentation.components.LoginScreenController;
import crab.appcore.db.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

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
    @FXML
    private Label topClamsLabel;
    @FXML
    private Label roundGemLabel;
    @FXML private Label heroWealthLabel;
    @FXML private Label heroReputationLabel;
    @FXML private Label heroInfamyLabel;
    @FXML private HBox handArea;
    @FXML private Button endTurnBtn;
    @FXML private StackPane enemyScrollContainer;
    @FXML private ScrollPane enemyScrollPane;
    @FXML private VBox actionLogContainer;
    @FXML private ScrollPane actionLogScroll;

    private ScreenManager screens;
    private GameSession gameSession;
    private PlayerState humanPlayer;
    private List<PlayerState> aiPlayers;
    
    private ActionCard selectedCard;
    private ActionCard keptCardToSave;
    private StackPane selectedCardNode;
    private StackPane keptCardNode;
    private ActionCard lastAiCardPlayed;
    
    private boolean isFemale = false;
    private VBox pauseMenu;
    private StackPane draftOverlay;
    private final Map<String, VBox> playerViews = new java.util.HashMap<>();

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
            String username = LoginScreenController.loggedInUser;
            String saveFileName = "savegame_" + username + ".dat";
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(saveFileName))) {
                oos.writeObject(gameSession);
                DatabaseManager.registerSave(username, saveFileName);
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

        topClamsLabel.setText("Clams: " + humanPlayer.clams());
        roundGemLabel.setText("Round: " + gameSession.currentRound());

        battlefieldArea.getChildren().clear();
        playerViews.clear();
        
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
        selectedCardNode = null;
        keptCardNode = null;

        endTurnBtn.setDisable(true);
        endTurnBtn.setOnAction(null);

        switch (gameSession.phase()) {
            case DEVELOPMENT -> showDevelopmentUI();
            case ACTION -> showActionUI();
            case RESOLUTION -> showResolutionUI();
            case EVENT -> {
                GameEvent event = generateRoundEvent();
                if (event.name().equals("Travelling Shop")) {
                    showTravellingShopUI();
                } else if (!event.name().equals("Calm Current")) {
                    showEventOverlay(event);
                } else {
                    updateUI();
                }
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

        playerViews.put(player.id(), container);
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
            for (PlayerAction action : gameSession.pendingActions().values()) {
                if (action.targetPlayerId() != null) {
                    String cardName = action.card().name();
            // User requirement: own cards show rarity, enemies don't in log
            if (!action.playerId().equals(humanPlayer.id())) {
                // Strip first word if it looks like a rarity (e.g. "Rare Steal" -> "Steal")
                String[] parts = cardName.split(" ", 2);
                if (parts.length > 1) {
                    cardName = parts[1];
                }
            }
            
            addLog(getDisplayName(action.playerId()) + " used " + cardName + " on " + getDisplayName(action.targetPlayerId()));
                    spawnEmoji(action.targetPlayerId(), action.card().type() == CardType.HELP ? "😊" : "😠");
                } else {
                    addLog(getDisplayName(action.playerId()) + " passed.");
                }
            }
            gameSession.resolveActions();
            lastAiCardPlayed = null;
            updateUI();
        });
    }

    private void showDevelopmentUI() {
        phasePromptLabel.setText("Phase: Development - Pick a Draft Card!");
        
        if (gameSession.currentDrafts().isEmpty()) {
            gameSession.resolveDevelopment(Map.of(), java.util.Set.of());
            updateUI();
            return;
        }

        List<ActionCard> humanDrafts = gameSession.currentDrafts().get(humanPlayer.id());
        if (humanDrafts == null || humanDrafts.isEmpty()) {
            Map<String, Integer> aiSelections = generateAiDraftSelections();
            gameSession.resolveDevelopment(aiSelections, java.util.Set.of());
            updateUI();
            return;
        }

        HBox draftBox = new HBox(20);
        draftBox.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < humanDrafts.size(); i++) {
            ActionCard draftCard = humanDrafts.get(i);
            int index = i;
            StackPane cardView = createHandCardView(draftCard);
            cardView.setOnMouseClicked(e -> {
                Map<String, Integer> selections = generateAiDraftSelections();
                selections.put(humanPlayer.id(), index);
                
                mainLayout.getChildren().remove(draftOverlay);
                draftOverlay = null;
                gameSession.resolveDevelopment(selections, java.util.Set.of());
                updateUI();
            });
            draftBox.getChildren().add(cardView);
        }
        
        Label title = new Label("Choose a card to add to your hand!");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");

        VBox overlayContent = new VBox(20, title, draftBox);
        overlayContent.setAlignment(Pos.CENTER);
        overlayContent.setStyle("-fx-background-color: rgba(0,0,0,0.8);");
        
        draftOverlay = new StackPane(overlayContent);
        AnchorPane.setTopAnchor(draftOverlay, 0.0);
        AnchorPane.setBottomAnchor(draftOverlay, 0.0);
        AnchorPane.setLeftAnchor(draftOverlay, 0.0);
        AnchorPane.setRightAnchor(draftOverlay, 0.0);
        
        mainLayout.getChildren().add(draftOverlay);
    }

    private Map<String, Integer> generateAiDraftSelections() {
        Map<String, Integer> selections = new java.util.HashMap<>();
        for (PlayerState ai : aiPlayers) {
            List<ActionCard> drafts = gameSession.currentDrafts().get(ai.id());
            if (drafts != null && !drafts.isEmpty()) {
                selections.put(ai.id(), random.nextInt(drafts.size()));
            }
        }
        return selections;
    }

    private GameEvent generateRoundEvent() {
        double roll = random.nextDouble();
        if (roll > 0.7) {
            return GameEvent.none();
        }
        
        if (roll < 0.15) {
            return GameEvent.marketCrash(50 + gameSession.currentRound() * 5);
        } else if (roll < 0.3) {
            return GameEvent.charityWave();
        } else if (roll < 0.45) {
            // Find player with highest wealth
            PlayerState richest = gameSession.players().stream()
                    .max(java.util.Comparator.comparingInt(PlayerState::wealth))
                    .orElse(humanPlayer);
            return GameEvent.crabHunt(richest.id(), 20 + gameSession.currentRound() * 3);
        } else if (roll < 0.6) {
            return GameEvent.travellingShop();
        } else {
            List<PlayerState> targets = gameSession.players();
            int totalInfamy = targets.stream().mapToInt(p -> Math.max(1, p.infamy())).sum();
            int targetRoll = random.nextInt(totalInfamy);
            int current = 0;
            PlayerState target = targets.get(0);
            for (PlayerState p : targets) {
                current += Math.max(1, p.infamy());
                if (targetRoll < current) {
                    target = p;
                    break;
                }
            }
            return new GameEvent("Shark Attack", target.id(), 0, 0, -20, 0);
        }
    }

    private void addLog(String message) {
        if (actionLogContainer == null) return;
        Label l = new Label(message);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        l.setWrapText(true);
        actionLogContainer.getChildren().add(l);
        if (actionLogContainer.getChildren().size() > 20) {
            actionLogContainer.getChildren().remove(0);
        }
        javafx.application.Platform.runLater(() -> actionLogScroll.setVvalue(1.0));
    }

    private void spawnEmoji(String playerId, String emoji) {
        VBox view = playerViews.get(playerId);
        if (view == null) {
            // Check if it's the human player
            if (playerId.equals(humanPlayer.id())) {
                // heroArea contains the human avatar and stats
                // we'll just use heroArea as the target
                spawnEmojiOnNode(heroArea, emoji);
            }
            return;
        }
        spawnEmojiOnNode(view, emoji);
    }

    private void spawnEmojiOnNode(javafx.scene.Node node, String emoji) {
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 40px;");
        emojiLabel.setMouseTransparent(true);
        
        mainLayout.getChildren().add(emojiLabel);
        
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        emojiLabel.setLayoutX(bounds.getMinX() + bounds.getWidth()/2 - 20);
        emojiLabel.setLayoutY(bounds.getMinY() - 50);

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2), emojiLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        
        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(2), emojiLabel);
        move.setByY(-100);
        
        fade.setOnFinished(e -> mainLayout.getChildren().remove(emojiLabel));
        
        fade.play();
        move.play();
    }

    private String getDisplayName(String playerId) {
        return gameSession.players().stream()
                .filter(p -> p.id().equals(playerId))
                .map(PlayerState::displayName)
                .findFirst()
                .orElse(playerId);
    }

    private void updateHandVisuals() {
        for (javafx.scene.Node node : handArea.getChildren()) {
            StackPane stack = (StackPane) node;
            VBox cardBox = (VBox) stack.getChildren().get(0);
            Label keepLabel = (Label) stack.getChildren().get(1);
            
            ActionCard c = (ActionCard) stack.getUserData();
            if (stack == selectedCardNode) {
                cardBox.setStyle("-fx-background-color: white; -fx-border-color: #eab308; -fx-border-width: 4; -fx-background-image: url('/assets/card-art/placeholdercard.png'); -fx-background-size: cover;");
            } else {
                cardBox.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2; -fx-background-image: url('/assets/card-art/placeholdercard.png'); -fx-background-size: cover;");
            }
            
            // Show K icon if kept
            if (stack == keptCardNode) {
                keepLabel.setVisible(true);
            } else {
                keepLabel.setVisible(false);
            }
        }
    }

    private StackPane createHandCardView(ActionCard card) {
        StackPane stack = new StackPane();
        stack.setUserData(card);

        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPrefSize(100, 140);
        cardBox.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2; -fx-background-image: url('/assets/card-art/placeholdercard.png'); -fx-background-size: cover;");
        
        Label name = new Label(card.name());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2;");
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        Label typeLabel = new Label(card.type().name());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2;");
        
        cardBox.getChildren().addAll(name, spacer, typeLabel);
        
        // Keep Icon "K"
        Label keepLabel = new Label("Ⓚ");
        keepLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-background-color: white; -fx-background-radius: 10;");
        keepLabel.setVisible(false);
        StackPane.setAlignment(keepLabel, Pos.TOP_RIGHT);
        
        stack.getChildren().addAll(cardBox, keepLabel);
        
        stack.setOnMouseClicked(e -> {
            if (gameSession.phase() == GamePhase.ACTION) {
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Right click -> Toggle Keep
                    if (keptCardNode == stack) {
                        keptCardNode = null;
                        keptCardToSave = null;
                    } else {
                        keptCardNode = stack;
                        keptCardToSave = card;
                    }
                    updateHandVisuals();
                } else {
                    // Left click -> Select or Unselect
                    if (selectedCardNode == stack) {
                        selectedCardNode = null;
                        selectedCard = null;
                        phasePromptLabel.setText("Phase: Select Card, Right-Click to Keep 1, then Target");
                    } else {
                        selectedCardNode = stack;
                        selectedCard = card;
                        phasePromptLabel.setText("Target an Enemy!");
                    }
                    updateHandVisuals();
                }
            }
        });
        
        return stack;
    }

    private void showTravellingShopUI() {
        phasePromptLabel.setText("Travelling Shop is Open!");
        
        VBox shopBox = new VBox(20);
        shopBox.setAlignment(Pos.CENTER);
        shopBox.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 30; -fx-border-color: #f59e0b; -fx-border-width: 5;");
        
        ImageView merchantImg = new ImageView(new Image(getClass().getResourceAsStream("/assets/textures/opportunistfm.png")));
        merchantImg.setFitHeight(250);
        merchantImg.setPreserveRatio(true);
        
        Label welcome = new Label("Welcome! What would you like to buy?");
        welcome.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Button upgradeBtn = new Button("Upgrade Build (30 Clams)");
        upgradeBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10; -fx-min-width: 250;");
        upgradeBtn.setDisable(humanPlayer.clams() < 30 || humanPlayer.buildLevel() >= 5);
        upgradeBtn.setOnAction(e -> {
            humanPlayer.deductClams(30);
            humanPlayer.incrementBuildLevel();
            
            mainLayout.getChildren().remove(draftOverlay);
            draftOverlay = null;
            gameSession.applyEvent(GameEvent.none()); // Advance phase
            updateUI();
        });
        
        Button rareBtn = new Button("Rare Card (15 Clams)");
        rareBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10; -fx-min-width: 250;");
        rareBtn.setDisable(humanPlayer.clams() < 15);
        rareBtn.setOnAction(e -> {
            humanPlayer.deductClams(15);
            ActionCard base = gameSession.generateCard(humanPlayer);
            ActionCard rare = new ActionCard(base.id(), base.name(), base.type(), CardRarity.RARE);
            humanPlayer.addCard(rare);
            
            mainLayout.getChildren().remove(draftOverlay);
            draftOverlay = null;
            gameSession.applyEvent(GameEvent.none()); // Advance phase
            updateUI();
        });
        
        Button leaveBtn = new Button("Leave");
        leaveBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10; -fx-min-width: 250;");
        leaveBtn.setOnAction(e -> {
            mainLayout.getChildren().remove(draftOverlay);
            draftOverlay = null;
            gameSession.applyEvent(GameEvent.none()); // Advance phase
            updateUI();
        });
        
        shopBox.getChildren().addAll(merchantImg, welcome, upgradeBtn, rareBtn, leaveBtn);
        
        draftOverlay = new StackPane(shopBox);
        AnchorPane.setTopAnchor(draftOverlay, 50.0);
        AnchorPane.setBottomAnchor(draftOverlay, 50.0);
        AnchorPane.setLeftAnchor(draftOverlay, 200.0);
        AnchorPane.setRightAnchor(draftOverlay, 200.0);
        
        mainLayout.getChildren().add(draftOverlay);
    }

    private void showEventOverlay(GameEvent event) {
        phasePromptLabel.setText("Event: " + event.name());
        
        VBox eventBox = new VBox(20);
        eventBox.setAlignment(Pos.CENTER);
        eventBox.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 30; -fx-border-color: white; -fx-border-width: 5;");
        
        String imagePath = "/assets/textures/altruistcrab.png"; // default
        String description = "Something happened!";
        
        if (event.name().equals("Market Crash")) {
            imagePath = "/assets/textures/opportunistfm.png";
            description = "Market Crash! Everyone loses wealth!";
        } else if (event.name().equals("Charity Wave")) {
            imagePath = "/assets/textures/altruistfm.png";
            description = "Charity Wave! Help cards are more effective next round!";
        } else if (event.name().equals("Crab Hunt")) {
            imagePath = "/assets/textures/saboteurfm.png";
            PlayerState target = gameSession.players().stream()
                    .filter(p -> p.id().equals(event.targetPlayerId()))
                    .findFirst().orElse(null);
            description = "Crab Hunt! " + (target != null ? target.displayName() : "The richest") + " loses clams!";
        } else if (event.name().equals("Shark Attack")) {
            imagePath = "/assets/textures/saboteurcrab.png";
            PlayerState target = gameSession.players().stream()
                    .filter(p -> p.id().equals(event.targetPlayerId()))
                    .findFirst().orElse(null);
            description = "Shark Attack targets " + (target != null ? target.displayName() : "someone") + "!";
        }
        
        ImageView eventImg = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
        eventImg.setFitHeight(250);
        eventImg.setPreserveRatio(true);
        
        Label title = new Label(event.name());
        title.setStyle("-fx-text-fill: gold; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        
        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 40;");
        okBtn.setOnAction(e -> {
            mainLayout.getChildren().remove(draftOverlay);
            draftOverlay = null;
            gameSession.applyEvent(event);
            addLog("Event: " + event.name());
            updateUI();
        });
        
        eventBox.getChildren().addAll(eventImg, title, desc, okBtn);
        
        draftOverlay = new StackPane(eventBox);
        AnchorPane.setTopAnchor(draftOverlay, 100.0);
        AnchorPane.setBottomAnchor(draftOverlay, 100.0);
        AnchorPane.setLeftAnchor(draftOverlay, 250.0);
        AnchorPane.setRightAnchor(draftOverlay, 250.0);
        
        mainLayout.getChildren().add(draftOverlay);
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
