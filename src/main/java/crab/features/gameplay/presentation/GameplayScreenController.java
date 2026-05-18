package crab.features.gameplay.presentation;

import crab.appcore.concurrent.AssetCache;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import javafx.scene.shape.Circle;
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
    @FXML private Label topClamsLabel;
    @FXML private VBox battlefieldArea;
    @FXML private Label phasePromptLabel;
    @FXML private HBox heroArea;
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
    @FXML private ScrollPane actionLogScroll;
    @FXML private VBox actionLogContainer;

    private ScreenManager screens;
    private GameSession gameSession;
    private PlayerState humanPlayer;
    private List<PlayerState> aiPlayers;
    
    private ActionCard selectedCard;
    private ActionCard lastAiCardPlayed;
    private boolean isDiscardPopupOpen = false; // prevents stacking multiple discard popups
    private boolean crabPeakBorderActive = false; // ensures only one peak border overlay exists
    private javafx.scene.shape.Rectangle crabPeakBorderNode = null;
    
    private boolean isFemale = false;
    private boolean botsDisabled = false;
    private boolean eventsDisabled = false;
    private VBox pauseMenu;
    private int logRound = 0; // tracks last logged round to avoid duplicate log entries

    // Image assets are now fully managed globally via the AssetFlyweightFactory (Flyweight Design Pattern)

    private final Random random = new Random();

    /** Appends a line to the action log panel with optional color. */
    private void appendLog(String text, String cssColor) {
        if (actionLogContainer == null) return;
        Label entry = new Label(text);
        entry.setWrapText(true);
        entry.setMaxWidth(220);
        entry.setStyle("-fx-text-fill: " + cssColor + "; -fx-font-size: 11px;");
        actionLogContainer.getChildren().add(entry);
        // Auto-scroll to bottom
        if (actionLogScroll != null) {
            actionLogScroll.layout();
            actionLogScroll.setVvalue(1.0);
        }
    }

    /** Appends a white log entry. */
    private void appendLog(String text) {
        appendLog(text, "white");
    }

    private String getGoalString(PlayerClass playerClass) {
        return switch (playerClass) {
            case OPPORTUNIST -> "Wealth";
            case ALTRUIST -> "Reputation";
            case SABOTEUR -> "Infamy";
        };
    }

    private Image getHumanAvatarImage(PlayerClass playerClass, boolean female) {
        String baseName = switch (playerClass) {
            case SABOTEUR -> "saboteur";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "opportunist";
        };
        String suffix = female ? "fm.gif" : "m.gif";
        String path = "/assets/humanoid-art/" + baseName + suffix;
        String slotId = "gameplay_hero_" + baseName + "_" + suffix;

        // Retrieve downsampled image (160x200) via Flyweight Factory, saving 1.5GB of JVM heap!
        return crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, path, 160, 200);
    }

    private Image getCrabImage(PlayerClass playerClass) {
        String baseName = switch (playerClass) {
            case SABOTEUR -> "sabotuer";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "oppurtunist";
        };
        String path = "/assets/crab-art/" + baseName + "_idle.gif";
        String slotId = "static_crab_" + baseName;
        return crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, path, 100, 80);
    }

    private Image getEndingImage(PlayerClass playerClass) {
        String fileName = switch (playerClass) {
            case SABOTEUR -> "saboteur_end_screen.gif";
            case ALTRUIST -> "altruist_end_screen.gif";
            case OPPORTUNIST -> "opportunist_end_screen.png";
        };
        String path = "/assets/endings/" + fileName;
        String slotId = "ending_screen_" + fileName;
        return crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, path, 600, 450);
    }

    public void initData(GameSession gameSession, ScreenManager screens, PlayerState humanPlayer, List<PlayerState> aiPlayers) {
        this.gameSession = gameSession;
        this.screens = screens;
        this.humanPlayer = humanPlayer;
        this.aiPlayers = aiPlayers;

        // Reclaim native byte buffers and unreferenced parent nodes from the previous game session immediately
        System.gc();

        // Load background — synchronous but fast; AssetCache ensures it's only loaded once
        var bgUrl = getClass().getResource("/assets/textures/bg_beach.gif");
        if (bgUrl != null) backgroundImageView.setImage(AssetCache.getInstance().getOrLoad(bgUrl.toExternalForm()));
        backgroundImageView.fitWidthProperty().bind(mainLayout.widthProperty());
        backgroundImageView.fitHeightProperty().bind(mainLayout.heightProperty());

        this.isFemale = !GameplayScreen.isMale;

        if (enemyScrollContainer != null && enemyScrollPane != null) {
            enemyScrollContainer.getChildren().clear();
            enemyScrollContainer.getChildren().add(enemyScrollPane);
            enemyScrollPane.prefWidthProperty().bind(enemyScrollContainer.widthProperty());
            enemyScrollPane.prefHeightProperty().bind(enemyScrollContainer.heightProperty());
        }

        updateHeroAvatarImage();
        updateUI();

        String user = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
        if ("dev".equalsIgnoreCase(user) || "admin".equalsIgnoreCase(user)) {
            createDevPanel();
        }
    }

    private void updateHeroAvatarImage() {
        if (humanPlayer != null) {
            Image heroImg = getHumanAvatarImage(humanPlayer.playerClass(), isFemale);
            if (heroImg != null) {
                heroAvatarImage.setImage(heroImg);
            }
        }
    }



    private void createDevPanel() {
        VBox devBox = new VBox(5);
        devBox.setAlignment(Pos.CENTER);
        devBox.setStyle("-fx-padding: 8; -fx-background-color: rgba(255, 0, 0, 0.45); -fx-background-radius: 6; -fx-border-color: red; -fx-border-width: 1; -fx-border-radius: 6;");
        
        Label title = new Label("DEV MODE");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-font-family: 'Luckiest Guy';");

        Button addClamsBtn = new Button("+1000 Clams");
        addClamsBtn.setOnAction(e -> {
            humanPlayer.addClams(1000);
            updateUI();
        });

        Button addWealthBtn = new Button("+1000 Wealth");
        addWealthBtn.setOnAction(e -> {
            humanPlayer.addWealth(1000);
            updateUI();
        });

        Button addRepBtn = new Button("+50 Rep");
        addRepBtn.setOnAction(e -> {
            humanPlayer.addReputation(50);
            updateUI();
        });

        Button addInfamyBtn = new Button("+50 Infamy");
        addInfamyBtn.setOnAction(e -> {
            humanPlayer.addInfamy(50);
            updateUI();
        });

        Button skipEndingBtn = new Button("Skip to Ending");
        skipEndingBtn.setOnAction(e -> {
            switch (humanPlayer.playerClass()) {
                case OPPORTUNIST -> humanPlayer.addWealth(1000);
                case ALTRUIST -> humanPlayer.addReputation(1000);
                case SABOTEUR -> humanPlayer.addInfamy(1000);
            }
            gameSession.forceGameOver();
            updateUI();
        });

        Button addEnemyWealthBtn = new Button("+1000 Enemy Wealth");
        addEnemyWealthBtn.setOnAction(e -> {
            if (!aiPlayers.isEmpty()) {
                aiPlayers.get(0).addWealth(1000);
                updateUI();
            }
        });

        Button forceEnemyWinBtn = new Button("Force Enemy Win");
        forceEnemyWinBtn.setOnAction(e -> {
            if (!aiPlayers.isEmpty()) {
                PlayerState enemy = aiPlayers.get(0);
                switch (enemy.playerClass()) {
                    case OPPORTUNIST -> enemy.addWealth(1000);
                    case ALTRUIST -> enemy.addReputation(1000);
                    case SABOTEUR -> enemy.addInfamy(1000);
                }
                gameSession.forceGameOver();
                updateUI();
            }
        });

        String btnStyle = "-fx-font-size: 10px; -fx-padding: 3 8; -fx-cursor: hand;";
        addClamsBtn.setStyle(btnStyle);
        addWealthBtn.setStyle(btnStyle);
        addRepBtn.setStyle(btnStyle);
        addInfamyBtn.setStyle(btnStyle);
        addEnemyWealthBtn.setStyle(btnStyle);
        skipEndingBtn.setStyle(btnStyle + " -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        forceEnemyWinBtn.setStyle(btnStyle + " -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");

        Button toggleBotsBtn = new Button("Toggle Bots: Active");
        toggleBotsBtn.setStyle(btnStyle);
        toggleBotsBtn.setOnAction(e -> {
            botsDisabled = !botsDisabled;
            if (botsDisabled) {
                toggleBotsBtn.setText("Toggle Bots: Disabled");
                toggleBotsBtn.setStyle(btnStyle + " -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
                appendLog("Dev: Bots are now DISABLED! (They will Pass their turns)", "#ef4444");
            } else {
                toggleBotsBtn.setText("Toggle Bots: Active");
                toggleBotsBtn.setStyle(btnStyle);
                appendLog("Dev: Bots are now ENABLED!", "#10b981");
            }
        });

        Button toggleEventsBtn = new Button("Toggle Events: Active");
        toggleEventsBtn.setStyle(btnStyle);
        toggleEventsBtn.setOnAction(e -> {
            eventsDisabled = !eventsDisabled;
            if (eventsDisabled) {
                toggleEventsBtn.setText("Toggle Events: Disabled");
                toggleEventsBtn.setStyle(btnStyle + " -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
                appendLog("Dev: World Events are now DISABLED!", "#ef4444");
            } else {
                toggleEventsBtn.setText("Toggle Events: Active");
                toggleEventsBtn.setStyle(btnStyle);
                appendLog("Dev: World Events are now ENABLED!", "#10b981");
            }
        });

        Button devAddCardBtn = new Button("Add Card...");
        devAddCardBtn.setStyle(btnStyle + " -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
        devAddCardBtn.setOnAction(e -> showDevAddCardPicker());

        Button devForceEventBtn = new Button("Force Event...");
        devForceEventBtn.setStyle(btnStyle + " -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");
        devForceEventBtn.setOnAction(e -> showDevForceEventPicker());

        devBox.getChildren().addAll(
            title, 
            addClamsBtn, 
            addWealthBtn, 
            addRepBtn, 
            addInfamyBtn, 
            addEnemyWealthBtn,
            toggleBotsBtn,
            toggleEventsBtn,
            devAddCardBtn,
            devForceEventBtn,
            skipEndingBtn,
            forceEnemyWinBtn
        );

        mainLayout.getChildren().add(devBox);
        AnchorPane.setTopAnchor(devBox, 60.0);
        AnchorPane.setLeftAnchor(devBox, 10.0);
    }

    @FXML
    void handleSettings(ActionEvent event) {
        if (pauseMenu != null) {
            return; // Already paused
        }

        Label pauseLabel = new Label("PAUSED");
        pauseLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Luckiest Guy';");

        Button continueBtn = new Button("Continue");
        continueBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
        continueBtn.setOnAction(e -> {
            mainLayout.getChildren().remove(pauseMenu);
            pauseMenu = null;
        });

        Button encyclopediaBtn = new Button("Card Encyclopedia");
        encyclopediaBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-text-fill: #fbbf24;");
        encyclopediaBtn.setOnAction(e -> {
            GameplayScreen.returningFromEncyclopedia = true;
            crab.features.gameplay.presentation.EncyclopediaScreen.previousScreenId = GameplayScreen.ID;
            mainLayout.getChildren().remove(pauseMenu);
            pauseMenu = null;
            screens.show("encyclopedia");
        });

        Button saveBtn = new Button("Save and Return to Menu");
        saveBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-text-fill: #10b981;");
        saveBtn.setOnAction(e -> {
            String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
            String saveFileName = "savegame_" + username + ".dat";
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(saveFileName))) {
                oos.writeObject(gameSession);
                oos.flush();
                System.out.println("Successfully saved game session to " + saveFileName);
                // Attempt to register in DB, but don't fail if DB is down
                try {
                    crab.appcore.db.DatabaseManager.registerSave(username, saveFileName);
                } catch (Exception dbEx) {
                    System.err.println("Note: Could not register save in database, using local file only.");
                }
            } catch (Exception ex) {
                System.err.println("CRITICAL ERROR: Failed to serialize game session!");
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
            confirmLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Button yesBtn = new Button("Yes, Withdraw");
            yesBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-text-fill: red;");
            yesBtn.setOnAction(yesEvent -> {
                String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
                String saveFileName = "savegame_" + username + ".dat";
                
                // 1. Delete local save file
                java.io.File file = new java.io.File(saveFileName);
                if (file.exists()) {
                    file.delete();
                }

                // 2. Clear DB record
                try {
                    crab.appcore.db.DatabaseManager.registerSave(username, null);
                } catch (Exception dbEx) {}

                mainLayout.getChildren().remove(pauseMenu);
                pauseMenu = null;
                screens.show("menu_main");
            });

            Button noBtn = new Button("No, Cancel");
            noBtn.setStyle("-fx-font-size: 16px; -fx-padding: 10 20;");
            noBtn.setOnAction(noEvent -> {
                pauseMenu.getChildren().clear();
                pauseMenu.getChildren().addAll(pauseLabel, continueBtn, encyclopediaBtn, saveBtn, withdrawBtn);
            });

            HBox buttonBox = new HBox(20, yesBtn, noBtn);
            buttonBox.setAlignment(Pos.CENTER);

            pauseMenu.getChildren().addAll(confirmLabel, buttonBox);
        });

        pauseMenu = new VBox(20, pauseLabel, continueBtn, encyclopediaBtn, saveBtn, withdrawBtn);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setStyle("-fx-border-color: white; -fx-border-width: 4; -fx-padding: 40; -fx-background-color: rgba(13, 43, 62, 0.95); -fx-background-radius: 20; -fx-border-radius: 20;");
        
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

        if (gameSession.crabPeakActive()) {
            String triggerer = "";
            String triggeredById = gameSession.crabPeakTriggeredById();
            if (triggeredById != null) {
                triggerer = gameSession.players().stream()
                        .filter(p -> p.id().equals(triggeredById))
                        .map(PlayerState::displayName)
                        .findFirst().map(n -> " — Triggered by " + n).orElse("");
            }
            Label peakAlert = new Label("⚠ CRAB PEAK — FINAL ROUND" + triggerer);
            peakAlert.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Luckiest Guy';");
            peakAlert.setEffect(new javafx.scene.effect.DropShadow(15, Color.BLACK));
            battlefieldArea.getChildren().add(peakAlert);
        }
        
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
        heroNameLabel.setText(humanPlayer.displayName());
        heroInfamyLabel.setWrapText(true);
        // Build info below infamy — mirrors the bot stat-box layout
        heroWealthLabel.setText("Wealth: " + humanPlayer.wealth());
        heroReputationLabel.setText("Reputation: " + humanPlayer.reputation());
        heroInfamyLabel.setText(
                "Infamy: " + humanPlayer.infamy() +
                "\n\u25b2 Build Lv." + humanPlayer.buildLevel() +
                "\n   Income: +" + humanPlayer.income() + "/rd" +
                "\n   Stat Buff: +" + (int)(humanPlayer.statBonus() * 100) + "%");

        handArea.getChildren().clear();
        selectedCard = null;

        endTurnBtn.setDisable(true);
        endTurnBtn.setOnAction(null);

        switch (gameSession.phase()) {
            case DEVELOPMENT -> showDevelopmentUI();
            case DRAWING -> showDrawingUI();
            case ACTION -> showActionUI();
            case RESOLUTION -> showResolutionUI();
            case EVENT -> {
                if (eventsDisabled) {
                    appendLog("Dev: Event skipped (Events are disabled)", "#ef4444");
                    gameSession.applyEvent(GameEvent.none());
                    updateUI();
                } else {
                    GameEvent event = gameSession.selectWeightedEvent();
                    showEventUI(event);
                }
            }
            case ROUND_COMPLETE -> {
                appendLog("Round " + gameSession.currentRound() + " complete.", "#fbbf24");
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
        
        String crabArtBase = switch (player.playerClass()) {
            case SABOTEUR -> "sabotuer";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "oppurtunist";
        };

        // Note specific filenames: saboteur uses 'eur', altruist uses 'ist', oppurtunist uses 'unist'
        String peakArtBase = switch (player.playerClass()) {
            case SABOTEUR -> "saboteur";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "oppurtunist";
        };

        // Extrinsic State: the specific viewer slotId (player.id()) to prevent concurrent GIF freezes
        final String slotId = player.id();
        
        // Intrinsic States: the shared resource paths and dimensions (100x80)
        final String idlePath = "/assets/crab-art/" + crabArtBase + "_idle.gif";
        final String damagePath = "/assets/crab-art/" + crabArtBase + "_damage.gif";
        final String peakPath = "/assets/crab-art/" + peakArtBase + "_crab_peak.gif";

        // Retrieve downsampled (100x80) image flyweights
        Image idleImg   = crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, idlePath, 100, 80);
        Image damageImg = crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, damagePath, 100, 80);
        Image peakImg   = crab.appcore.concurrent.AssetFlyweightFactory.getSharedImage(slotId, peakPath, 100, 80);

        // Determine if player triggered the Crab Peak event
        boolean isTriggerer = gameSession.crabPeakActive()
                && player.id().equals(gameSession.crabPeakTriggeredById());

        // Replace idle with peak animation if they are the Crab Peak triggerer
        final Image activeIdleImg = (isTriggerer && peakImg != null) ? peakImg : idleImg;

        // Satisfy Java lambda lexical closure by creating effectively-final references
        final Image finalIdle = activeIdleImg;
        final Image finalDamage = damageImg;
        
        // Show damage art if targeted in resolution phase
        if (gameSession.phase() == GamePhase.RESOLUTION) {
            boolean isIncomingTarget = gameSession.pendingActions().values().stream()
                    .anyMatch(a -> player.id().equals(a.targetPlayerId()));
            crabAvatar.setImage(isIncomingTarget ? finalDamage : finalIdle);
        } else {
            crabAvatar.setImage(finalIdle);
        }

        // StackPane to hold crab avatar and overlay the bottom-right status indicator circle
        StackPane avatarPane = new StackPane(crabAvatar);
        avatarPane.setPrefSize(100, 80);
        avatarPane.setMaxSize(100, 80);

        // Status circle indicator on bottom-right of crab avatar window
        javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(5); // radius 5
        boolean humanTriggeredPeak = gameSession.crabPeakActive()
                && "human".equals(gameSession.crabPeakTriggeredById());

        if (humanTriggeredPeak) {
            indicator.setFill(Color.web("#fbbf24")); // glowing gold
            indicator.setStroke(Color.web("#d97706"));
            indicator.setStrokeWidth(1.2);
            indicator.setEffect(new javafx.scene.effect.DropShadow(3, Color.web("#fbbf24")));
        } else {
            indicator.setFill(Color.web("#94a3b8")); // flat slate grey
            indicator.setStroke(Color.web("#475569"));
            indicator.setStrokeWidth(1.0);
        }
        StackPane.setAlignment(indicator, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(indicator, new Insets(0, 4, 4, 0));
        avatarPane.getChildren().add(indicator);
        
        // 2. Stats Box (Has background) — wider to accommodate build level details
        VBox statsBox = new VBox(2);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setPrefWidth(185);
        statsBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: " + (isHuman ? "#10b981" : "white") + "; -fx-border-width: 2; -fx-padding: 5;");
        
        // Bot class label shows only class name; player also sees their goal
        Label pClass = new Label(isHuman
                ? player.playerClass().name() + " (" + getGoalString(player.playerClass()) + ")"
                : player.playerClass().name());
        pClass.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label pName = new Label(player.displayName());
        pName.setStyle("-fx-text-fill: " + (isHuman ? "#10b981" : "gold") + "; -fx-font-weight: bold;");
        Label pWealth = new Label("Wealth: " + player.wealth());
        pWealth.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        Label pRep = new Label("Reputation: " + player.reputation());
        pRep.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        Label pInfamy = new Label("Infamy: " + player.infamy());
        pInfamy.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");

        // Build level — bots show level only; only the player sees income/buff
        Label pBuildLv = new Label("\u25b2 Build Lv." + player.buildLevel());
        pBuildLv.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 9px; -fx-font-weight: bold;");

        if (isHuman) {
            Label pIncome   = new Label("  Income: +" + player.income() + " /rd");
            pIncome.setStyle("-fx-text-fill: #a3e635; -fx-font-size: 9px;");
            Label pStatBuff = new Label("  Stat Buff: +" + (int)(player.statBonus() * 100) + "%");
            pStatBuff.setStyle("-fx-text-fill: #f472b6; -fx-font-size: 9px;");
            statsBox.getChildren().addAll(pName, pClass, pWealth, pRep, pInfamy, pBuildLv, pIncome, pStatBuff);
        } else {
            // Bot: only show build level, keep it clean
            statsBox.getChildren().addAll(pName, pClass, pWealth, pRep, pInfamy, pBuildLv);
        }
        container.getChildren().addAll(avatarPane, statsBox);

        if (isTriggerer) {
            // If the HUMAN triggered peak: show a faint pulsing gold border ring around the entire layout
            if (isHuman) {
                applyOrUpdateCrabPeakBorder();
            } else {
                // AI triggerer: static red highlight on their stat box only
                statsBox.setStyle("-fx-background-color: rgba(100,0,0,0.7); -fx-border-color: #ef4444; -fx-border-width: 3; -fx-padding: 5;");
            }
        }

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
                gameSession.submitAction(new PlayerAction(humanPlayer.id(), selectedCard, player.id(), null));
                submitAiActions();
                updateUI();
            }
        });

        // Hover effect to show it's clickable
        container.setOnMouseEntered(e -> {
            if (gameSession.phase() == GamePhase.ACTION && selectedCard != null) {
                statsBox.setStyle("-fx-background-color: rgba(200,50,50,0.8); -fx-border-color: red; -fx-border-width: 3; -fx-padding: 4;");
                crabAvatar.setImage(finalDamage);
            }
        });
        container.setOnMouseExited(e -> {
            statsBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-border-color: " + (isHuman ? "#10b981" : "white") + "; -fx-border-width: 2; -fx-padding: 5;");
            if (gameSession.phase() != GamePhase.RESOLUTION) {
                crabAvatar.setImage(finalIdle);
            }
        });

        return container;
    }

    private void showActionUI() {
        endTurnBtn.setDisable(false);
        endTurnBtn.setText("PASS");
        endTurnBtn.setOnAction(e -> {
            gameSession.submitAction(new PlayerAction(humanPlayer.id(), new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON), aiPlayers.get(0).id(), null));
            submitAiActions();
            updateUI();
        });

        phasePromptLabel.setText("Phase: Select Card → Target Enemy  |  RMB to Discard");

        List<ActionCard> hand = new java.util.ArrayList<>(humanPlayer.hand());
        hand.sort(java.util.Comparator.comparingInt(c -> java.util.List.of("Take", "Give", "Share").indexOf(c.name())));

        for (ActionCard card : hand) {
            StackPane cardView = createHandCardView(card);
            handArea.getChildren().add(cardView);
        }
    }

    private void submitAiActions() {
        if (botsDisabled) {
            for (PlayerState ai : aiPlayers) {
                gameSession.submitAction(new PlayerAction(ai.id(), new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON), ai.id(), null));
            }
            return;
        }

        List<PlayerState> allPlayers = gameSession.players();
        String difficulty = GameplayScreen.difficulty;

        for (PlayerState ai : aiPlayers) {
            List<ActionCard> hand = new java.util.ArrayList<>(ai.hand());
            if (hand.isEmpty()) continue;

            ActionCard chosenCard;
            PlayerState target;

            // 1. Decide card to play based on difficulty and class
            List<ActionCard> idealCards = hand.stream()
                .filter(c -> isIdealForClass(c, ai.playerClass()))
                .collect(Collectors.toList());

            boolean playIdeal = false;
            if (difficulty.equals("Hard")) {
                playIdeal = !idealCards.isEmpty() && random.nextDouble() < 0.9;
            } else if (difficulty.equals("Medium")) {
                playIdeal = !idealCards.isEmpty() && random.nextDouble() < 0.6;
            }
            // Easy is always playIdeal = false (completely random)

            if (playIdeal) {
                chosenCard = idealCards.get(random.nextInt(idealCards.size()));
            } else {
                chosenCard = hand.get(random.nextInt(hand.size()));
            }

            // 2. Decide target based on card type and difficulty
            target = decideTarget(ai, chosenCard, allPlayers, difficulty);

            gameSession.submitAction(new PlayerAction(ai.id(), chosenCard, target.id(), null));
        }
    }

    private boolean isIdealForClass(ActionCard card, PlayerClass playerClass) {
        return switch (playerClass) {
            case OPPORTUNIST -> card.type() == CardType.STEAL || card.type() == CardType.SIGNATURE_OPPORTUNIST;
            case ALTRUIST -> card.type() == CardType.HELP || card.type() == CardType.SIGNATURE_ALTRUIST;
            case SABOTEUR -> card.type() == CardType.SABOTAGE || card.type() == CardType.SIGNATURE_SABOTEUR;
        };
    }

    private PlayerState decideTarget(PlayerState actor, ActionCard card, List<PlayerState> allPlayers, String difficulty) {
        List<PlayerState> enemies = allPlayers.stream()
                .filter(p -> !p.id().equals(actor.id()))
                .collect(Collectors.toList());

        if (enemies.isEmpty()) return actor;

        if (difficulty.equals("Easy")) return enemies.get(random.nextInt(enemies.size()));

        // Medium/Hard logic: Target selection based on card type
        return switch (card.type()) {
            case HELP, SIGNATURE_ALTRUIST -> 
                enemies.stream()
                        .min(java.util.Comparator.comparingInt(PlayerState::reputation))
                        .orElse(enemies.get(0));
            case STEAL, SIGNATURE_OPPORTUNIST -> 
                enemies.stream()
                        .max(java.util.Comparator.comparingInt(PlayerState::wealth))
                        .orElse(enemies.get(0));
            case SABOTAGE, SIGNATURE_SABOTEUR -> 
                enemies.stream()
                        .max(java.util.Comparator.comparingInt(p -> switch(p.playerClass()) {
                            case OPPORTUNIST -> p.wealth();
                            case ALTRUIST -> p.reputation();
                            case SABOTEUR -> p.infamy();
                        }))
                        .orElse(enemies.get(0));
        };
    }

    private void showDevelopmentUI() {
        phasePromptLabel.setText("DEVELOPMENT PHASE");
        
        StackPane overlay = new StackPane();
        overlay.setPrefSize(400, 300);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-background-radius: 15; -fx-border-color: #3b82f6; -fx-border-width: 3; -fx-border-radius: 15;");
        
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        
        Label title = new Label("Development Phase");
        title.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label levelLbl = new Label("Current Build Level: " + humanPlayer.buildLevel() + " (Max: 5)");
        levelLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        Label incomeLbl = new Label("Passive Income: " + humanPlayer.income() + " Clams/round");
        incomeLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 14px;");

        Button upgradeBtn = new Button("Upgrade Build (" + humanPlayer.upgradeCost() + " Clams)");
        upgradeBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        
        if (!humanPlayer.canUpgradeBuild()) {
            upgradeBtn.setDisable(true);
            if (humanPlayer.buildLevel() >= 5) {
                upgradeBtn.setText("MAX LEVEL REACHED");
            }
        }
        
        Button continueBtn = new Button("Continue");
        continueBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        
        java.util.Set<String> upgradeRequests = new java.util.HashSet<>();
        
        upgradeBtn.setOnAction(e -> {
            upgradeRequests.add(humanPlayer.id());
            upgradeBtn.setDisable(true);
            upgradeBtn.setText("Upgrading...");
            appendLog("Requested Build Upgrade to Level " + (humanPlayer.buildLevel() + 1), "#f59e0b");
        });
        
        continueBtn.setOnAction(e -> {
            // AI logic: 50% chance to upgrade if they have enough clams
            for (PlayerState ai : aiPlayers) {
                if (ai.canUpgradeBuild() && random.nextBoolean()) {
                    upgradeRequests.add(ai.id());
                }
            }
            
            appendLog("--- Round " + gameSession.currentRound() + " ---", "#fbbf24");
            Map<String, Integer> selections = gameSession.players().stream()
                    .collect(Collectors.toMap(PlayerState::id, p -> 0));
            gameSession.resolveDevelopment(selections, upgradeRequests);
            appendLog("Income granted. Draw phase complete.", "#34d399");
            mainLayout.getChildren().remove(overlay);
            updateUI();
        });
        
        content.getChildren().addAll(title, levelLbl, incomeLbl, upgradeBtn, continueBtn);
        overlay.getChildren().add(content);
        
        mainLayout.getChildren().add(overlay);
        AnchorPane.setTopAnchor(overlay, 150.0);
        AnchorPane.setLeftAnchor(overlay, 340.0);
    }

    private void showDrawingUI() {
        phasePromptLabel.setText("Drawing Cards...");
        
        // Resolve drawing in domain (adds 1 card to each player)
        gameSession.resolveDrawing();
        
        // Show temporary overlay
        StackPane overlay = new StackPane();
        overlay.setPrefSize(400, 200);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 20; -fx-border-color: #3b82f6; -fx-border-width: 3; -fx-border-radius: 20;");
        
        Label l = new Label("DRAW PHASE\n+1 Card Added to Hand");
        l.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-alignment: center;");
        overlay.getChildren().add(l);
        
        mainLayout.getChildren().add(overlay);
        AnchorPane.setTopAnchor(overlay, 200.0);
        AnchorPane.setLeftAnchor(overlay, 340.0);
        
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.0), overlay);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(javafx.util.Duration.seconds(1.0));
        ft.setOnFinished(e -> {
            mainLayout.getChildren().remove(overlay);
            updateUI(); // Refresh to show ACTION phase with new card
        });
        ft.play();
    }

    private void showEventUI(GameEvent event) {
        phasePromptLabel.setText("EVENT: " + event.name());
        
        StackPane overlay = new StackPane();
        overlay.setPrefSize(550, 450);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.9); -fx-background-radius: 20; -fx-border-color: #f87171; -fx-border-width: 4; -fx-border-radius: 20;");
        
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        
        Label title = new Label("--- WORLD EVENT ---");
        title.setStyle("-fx-text-fill: #f87171; -fx-font-size: 26px; -fx-font-weight: bold;");
        
        String imgPath = switch (event.name()) {
            case "Market Crash" -> "/assets/event-art/market_crash_event.png";
            case "Charity Wave" -> "/assets/event-art/charity_wave_event.png";
            case "Crab Hunt" -> "/assets/event-art/crab_hunt_event.png";
            case "Travelling Shop" -> "/assets/event-art/travelling_merchant_event.png";
            default -> "/assets/event-art/charity_wave_event.png";
        };
        
        ImageView eventImg = new ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(imgPath)));
        eventImg.setFitHeight(180);
        eventImg.setPreserveRatio(true);
        
        Label name = new Label(event.name());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Label desc = new Label(event.description());
        desc.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-text-alignment: center;");
        desc.setWrapText(true);
        desc.setMaxWidth(450);
        
        Button okBtn = new Button("CONTINUE");
        okBtn.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        
        if ("Travelling Shop".equals(event.name())) {
            // --- Stock display label ---
            int rareStock = gameSession.merchantRareStock();
            boolean hasSig = gameSession.merchantHasSignatureStock();

            Label stockLabel = new Label("Stock: " + rareStock + "x Rare" + (hasSig ? "  |  1x Signature" : ""));
            stockLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");

            // --- Rare buy button ---
            Button rareBtn = new Button("Buy RARE Card  —  50 Clams  (Stock: " + rareStock + ")");
            rareBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            if (rareStock <= 0) rareBtn.setDisable(true);

            // --- Signature buy button (only if stock exists) ---
            Button sigBtn = new Button("Buy SIGNATURE Card  —  120 Clams");
            sigBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            sigBtn.setVisible(hasSig);
            sigBtn.setManaged(hasSig);

            // --- Discard button (always shown, lets player make room) ---
            Button discardBtn = new Button("🗑  Discard a Card from Hand");
            discardBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: #f87171; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
            discardBtn.setDisable(humanPlayer.hand().isEmpty());

            // Shared helper to update button states after any purchase/discard
            Runnable refreshBtns = () -> {
                int curRare = gameSession.merchantRareStock();
                boolean handFull = humanPlayer.hand().size() >= PlayerState.MAX_HAND_SIZE;
                rareBtn.setText("Buy RARE Card  —  50 Clams  (Stock: " + curRare + ")");
                rareBtn.setDisable(curRare <= 0 || humanPlayer.clams() < 50);
                sigBtn.setDisable(!gameSession.merchantHasSignatureStock() || humanPlayer.clams() < 120);
                discardBtn.setDisable(humanPlayer.hand().isEmpty());
                topClamsLabel.setText("Clams: " + humanPlayer.clams());
                stockLabel.setText("Stock: " + curRare + "x Rare" +
                        (gameSession.merchantHasSignatureStock() ? "  |  1x Signature" : ""));
            };

            rareBtn.setOnAction(e -> {
                if (humanPlayer.hand().size() >= PlayerState.MAX_HAND_SIZE) {
                    appendLog("Hand is full! Discard a card first.", "#ef4444");
                    return;
                }
                if (gameSession.buyRareCard(humanPlayer.id())) {
                    appendLog("Travelling Shop: Purchased a Rare card!", "#3b82f6");
                    refreshBtns.run();
                } else {
                    appendLog("Not enough Clams for Rare card! (50 needed)", "#ef4444");
                }
            });

            sigBtn.setOnAction(e -> {
                if (humanPlayer.hand().size() >= PlayerState.MAX_HAND_SIZE) {
                    appendLog("Hand is full! Discard a card first.", "#ef4444");
                    return;
                }
                if (gameSession.buySignatureCard(humanPlayer.id())) {
                    appendLog("Travelling Shop: Purchased a Signature card!", "#fbbf24");
                    sigBtn.setVisible(false);
                    sigBtn.setManaged(false);
                    refreshBtns.run();
                } else {
                    appendLog("Not enough Clams for Signature card! (120 needed)", "#ef4444");
                }
            });

            discardBtn.setOnAction(e -> {
                // Show a mini discard picker if there are cards in hand
                if (humanPlayer.hand().isEmpty()) return;
                // Re-use showDiscardConfirmation if only 1 card, else show picker
                List<ActionCard> currentHand = new java.util.ArrayList<>(humanPlayer.hand());
                if (currentHand.size() == 1) {
                    showDiscardConfirmationWithCallback(currentHand.get(0), refreshBtns);
                } else {
                    showMerchantDiscardPicker(currentHand, refreshBtns);
                }
            });

            content.getChildren().addAll(title, eventImg, name, desc, stockLabel, rareBtn, sigBtn, discardBtn, okBtn);
        } else {
            content.getChildren().addAll(title, eventImg, name, desc, okBtn);
        }

        okBtn.setOnAction(e -> {
            if (event.targetPlayerId() == null && (event.clamsDelta() != 0 || event.wealthDelta() != 0)) {
                gameSession.applyEventToAll(event);
            } else {
                gameSession.applyEvent(event);
            }
            appendLog("EVENT: " + event.name() + " applied.", "#f87171");
            mainLayout.getChildren().remove(overlay);
            updateUI();
        });
        
        overlay.getChildren().add(content);
        
        mainLayout.getChildren().add(overlay);
        AnchorPane.setTopAnchor(overlay, 100.0);
        AnchorPane.setLeftAnchor(overlay, 265.0);
    }

    private void showResolutionUI() {
        phasePromptLabel.setText("Phase: Resolution");

        endTurnBtn.setDisable(false);
        endTurnBtn.setText("RESOLVE");
        endTurnBtn.setOnAction(e -> {
            // Log actions before they are cleared
            Map<String, PlayerAction> actions = gameSession.pendingActions();
            for (PlayerAction action : actions.values()) {
                String actorName = gameSession.players().stream()
                        .filter(p -> p.id().equals(action.playerId()))
                        .map(PlayerState::displayName)
                        .findFirst().orElse(action.playerId());
                
                String targetName = gameSession.players().stream()
                        .filter(p -> p.id().equals(action.targetPlayerId()))
                        .map(PlayerState::displayName)
                        .findFirst().orElse("nobody");

                String color = action.playerId().equals(humanPlayer.id()) ? "#60a5fa" : "white";
                appendLog(actorName + " used " + action.card().name() + " -> " + targetName, color);
            }

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
                String.format("Help a target:\n+%d Rep (you)  +%d Clams (you)\n+%d Wealth (target)",
                        Math.round(40 * mult), Math.round(15 * mult), Math.round(30 * mult));
            case STEAL, SIGNATURE_OPPORTUNIST ->
                String.format("Steal from a target:\n+%d Wealth (you)  -%d Rep (you)\n-%d Wealth (target)",
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

            boolean isSelected = (c == selectedCard);

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
        var artUrl = getClass().getResource(artPath);
        if (artUrl != null) {
            artView.setImage(AssetCache.getInstance().getOrLoad(artUrl.toExternalForm()));
        } else {
            // Fallback to placeholder
            var phUrl = getClass().getResource("/assets/card-art/placeholdercard.png");
            if (phUrl != null) artView.setImage(AssetCache.getInstance().getOrLoad(phUrl.toExternalForm()));
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

        stack.getChildren().addAll(artView, nameBadge, rarityBadge);

        // Initial rarity glow
        DropShadow initGlow = new DropShadow();
        initGlow.setColor(Color.web(rarityColor));
        initGlow.setRadius(8);
        initGlow.setSpread(0.1);
        stack.setEffect(initGlow);

        // ---- Tooltip: effect description + RMB hint ----
        String tooltipText = getCardEffectDescription(card) + "\n\n[Right Click (RMB) to Discard]";
        Tooltip tip = new Tooltip(tooltipText);
        tip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(stack, tip);

        // ---- RMB discard hint overlay (shows on hover) ----
        Label rmbHint = new Label("RMB to Discard");
        rmbHint.setStyle("-fx-background-color: rgba(220,38,38,0.85); -fx-text-fill: white; " +
                "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 5; " +
                "-fx-background-radius: 0 0 7 7;");
        rmbHint.setMaxWidth(Double.MAX_VALUE);
        rmbHint.setAlignment(Pos.CENTER);
        rmbHint.setVisible(false);
        StackPane.setAlignment(rmbHint, Pos.BOTTOM_CENTER);
        stack.getChildren().add(rmbHint);

        // ---- Hover: lift the card + show RMB hint ----
        TranslateTransition hoverUp   = new TranslateTransition(Duration.millis(120), stack);
        TranslateTransition hoverDown = new TranslateTransition(Duration.millis(120), stack);
        stack.setOnMouseEntered(e -> {
            hoverDown.stop();
            hoverUp.setToY(-12);
            hoverUp.play();
            rmbHint.setVisible(true);
        });
        stack.setOnMouseExited(e -> {
            hoverUp.stop();
            hoverDown.setToY(0);
            hoverDown.play();
            rmbHint.setVisible(false);
        });

        // ---- Click handler ----
        stack.setOnMouseClicked(e -> {
            if (gameSession.phase() == GamePhase.ACTION) {
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Right-click: show discard confirmation
                    showDiscardConfirmation(card);
                } else {
                    // Left-click: select / deselect
                    if (selectedCard == card) {
                        selectedCard = null;
                        phasePromptLabel.setText("Phase: Select Card → Target Enemy  |  RMB to Discard");
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

    /** Returns how many Clams the player receives for discarding a card of the given rarity. */
    private int getDiscardClamsReward(CardRarity rarity) {
        return switch (rarity) {
            case COMMON    -> 10;
            case UNCOMMON  -> 20;
            case RARE      -> 35;
            case SIGNATURE -> 75;
        };
    }

    /** Shows a confirmation popup before discarding a card. Only one popup allowed at a time. */
    private void showDiscardConfirmation(ActionCard card) {
        if (isDiscardPopupOpen) return; // block stacking
        isDiscardPopupOpen = true;

        int clamsReward = getDiscardClamsReward(card.rarity());

        VBox popup = new VBox(15);
        popup.setAlignment(Pos.CENTER);
        popup.setStyle("-fx-background-color: rgba(13,43,62,0.97); -fx-border-color: #ef4444; " +
                "-fx-border-width: 3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 30;");

        Label title = new Label("Discard Card?");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label cardName = new Label("\"" + card.name() + "\" (" + card.rarity().name() + ")");
        cardName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label rewardLbl = new Label("You will receive " + clamsReward + " Clams");
        rewardLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label warn = new Label("This card will be permanently removed from your hand.");
        warn.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        warn.setWrapText(true);
        warn.setMaxWidth(320);
        warn.setAlignment(Pos.CENTER);

        Button yesBtn = new Button("Yes, Discard (+" + clamsReward + " Clams)");
        yesBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-cursor: hand;");

        Button noBtn = new Button("Cancel");
        noBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox btnRow = new HBox(15, yesBtn, noBtn);
        btnRow.setAlignment(Pos.CENTER);

        popup.getChildren().addAll(title, cardName, rewardLbl, warn, btnRow);
        mainLayout.getChildren().add(popup);
        AnchorPane.setTopAnchor(popup, 220.0);
        AnchorPane.setLeftAnchor(popup, 350.0);
        AnchorPane.setRightAnchor(popup, 350.0);

        yesBtn.setOnAction(ev -> {
            mainLayout.getChildren().remove(popup);
            isDiscardPopupOpen = false;
            gameSession.discardCard(humanPlayer.id(), card);
            humanPlayer.addClams(clamsReward);
            if (selectedCard == card) selectedCard = null;
            appendLog("Discarded: " + card.name() + " (" + card.rarity().name() + ") — +" + clamsReward + " Clams", "#ef4444");
            topClamsLabel.setText("Clams: " + humanPlayer.clams());
            // Refresh hand display only
            handArea.getChildren().clear();
            List<ActionCard> hand = new java.util.ArrayList<>(humanPlayer.hand());
            hand.sort(java.util.Comparator.comparingInt(c -> java.util.List.of("Take", "Give", "Share").indexOf(c.name())));
            for (ActionCard c : hand) handArea.getChildren().add(createHandCardView(c));
        });
        noBtn.setOnAction(ev -> {
            mainLayout.getChildren().remove(popup);
            isDiscardPopupOpen = false;
        });
    }

    /**
     * Adds a faint pulsing gold border rectangle around the entire game view when the
     * human player triggers Crab Peak. Non-intrusive but clearly noticeable.
     * Safe to call multiple times — only one overlay is ever added.
     */
    private void applyOrUpdateCrabPeakBorder() {
        if (crabPeakBorderActive && crabPeakBorderNode != null) return; // already applied
        crabPeakBorderActive = true;

        javafx.scene.shape.Rectangle border = new javafx.scene.shape.Rectangle();
        // Bind size to mainLayout so it always covers the full screen
        border.widthProperty().bind(mainLayout.widthProperty());
        border.heightProperty().bind(mainLayout.heightProperty());
        border.setFill(javafx.scene.paint.Color.TRANSPARENT);
        border.setStroke(javafx.scene.paint.Color.web("#fbbf24")); // gold
        border.setStrokeWidth(6);
        border.setOpacity(0.55);
        border.setMouseTransparent(true); // never intercepts clicks
        border.setManaged(false);
        border.setLayoutX(0);
        border.setLayoutY(0);

        mainLayout.getChildren().add(border);
        crabPeakBorderNode = border;

        // Gentle pulse: opacity 0.55 <-> 0.15 over 1.2 s
        javafx.animation.Timeline pulse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(border.opacityProperty(), 0.55)),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(1200),
                new javafx.animation.KeyValue(border.opacityProperty(), 0.15))
        );
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    /** Discard confirmation that also runs a callback after the discard (e.g., to refresh merchant UI). */
    private void showDiscardConfirmationWithCallback(ActionCard card, Runnable callback) {
        if (isDiscardPopupOpen) return; // block stacking
        isDiscardPopupOpen = true;

        int clamsReward = getDiscardClamsReward(card.rarity());

        VBox popup = new VBox(15);
        popup.setAlignment(Pos.CENTER);
        popup.setStyle("-fx-background-color: rgba(13,43,62,0.97); -fx-border-color: #ef4444; " +
                "-fx-border-width: 3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 30;");

        Label title = new Label("Discard Card?");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label cardName = new Label("\"" + card.name() + "\" (" + card.rarity().name() + ")");
        cardName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        Label rewardLbl = new Label("You will receive " + clamsReward + " Clams");
        rewardLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label warn = new Label("This card will be permanently removed from your hand.");
        warn.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        warn.setWrapText(true); warn.setMaxWidth(320); warn.setAlignment(Pos.CENTER);

        Button yesBtn = new Button("Yes, Discard (+" + clamsReward + " Clams)");
        yesBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-cursor: hand;");
        Button noBtn = new Button("Cancel");
        noBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox btnRow = new HBox(15, yesBtn, noBtn);
        btnRow.setAlignment(Pos.CENTER);
        popup.getChildren().addAll(title, cardName, rewardLbl, warn, btnRow);
        mainLayout.getChildren().add(popup);
        AnchorPane.setTopAnchor(popup, 220.0);
        AnchorPane.setLeftAnchor(popup, 350.0);
        AnchorPane.setRightAnchor(popup, 350.0);

        yesBtn.setOnAction(ev -> {
            mainLayout.getChildren().remove(popup);
            isDiscardPopupOpen = false;
            gameSession.discardCard(humanPlayer.id(), card);
            humanPlayer.addClams(clamsReward);
            if (selectedCard == card) selectedCard = null;
            appendLog("Discarded: " + card.name() + " (" + card.rarity().name() + ") — +" + clamsReward + " Clams", "#ef4444");
            topClamsLabel.setText("Clams: " + humanPlayer.clams());
            if (callback != null) callback.run();
        });
        noBtn.setOnAction(ev -> {
            mainLayout.getChildren().remove(popup);
            isDiscardPopupOpen = false;
        });
    }

    private void showDevForceEventPicker() {
        VBox picker = new VBox(8);
        picker.setAlignment(Pos.CENTER);
        picker.setStyle("-fx-background-color: rgba(13,43,62,0.97); -fx-border-color: #3b82f6; " +
                "-fx-border-width: 3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 15;");

        Label title = new Label("Select Event to Force Trigger");
        title.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px; -fx-font-weight: bold;");
        picker.getChildren().add(title);

        List<String> events = List.of(
            "Market Crash",
            "Charity Wave",
            "Crab Hunt",
            "Travelling Shop",
            "Calm Current"
        );

        for (String eventName : events) {
            Button btn = new Button(eventName);
            btn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 6 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 200;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 6 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 200;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 6 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 200;"));
            
            btn.setOnAction(e -> {
                mainLayout.getChildren().remove(picker);
                GameEvent forcedEvent = gameSession.devTriggerEvent(eventName);
                appendLog("Dev: Force-triggered event \"" + eventName + "\"", "#3b82f6");
                showEventUI(forcedEvent);
            });
            picker.getChildren().add(btn);
        }

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 6 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> mainLayout.getChildren().remove(picker));
        picker.getChildren().add(cancelBtn);

        mainLayout.getChildren().add(picker);
        AnchorPane.setTopAnchor(picker, 150.0);
        AnchorPane.setLeftAnchor(picker, 430.0);
    }

    private void showDevAddCardPicker() {
        VBox picker = new VBox(8);
        picker.setAlignment(Pos.CENTER);
        picker.setStyle("-fx-background-color: rgba(13,43,62,0.97); -fx-border-color: #10b981; " +
                "-fx-border-width: 3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 15;");

        Label title = new Label("Select Card to Add to Hand");
        title.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold;");
        picker.getChildren().add(title);

        List<ActionCard> templates = List.of(
            new ActionCard("give_common", "Give [Common]", CardType.HELP, CardRarity.COMMON),
            new ActionCard("take_common", "Take [Common]", CardType.STEAL, CardRarity.COMMON),
            new ActionCard("sabotage_common", "Sabotage [Common]", CardType.SABOTAGE, CardRarity.COMMON),
            new ActionCard("give_uncommon", "Generous Give [Uncommon]", CardType.HELP, CardRarity.UNCOMMON),
            new ActionCard("take_uncommon", "Snatch [Uncommon]", CardType.STEAL, CardRarity.UNCOMMON),
            new ActionCard("sabotage_uncommon", "Scheme [Uncommon]", CardType.SABOTAGE, CardRarity.UNCOMMON),
            new ActionCard("give_rare", "Gracious Give [Rare]", CardType.HELP, CardRarity.RARE),
            new ActionCard("take_rare", "Heist [Rare]", CardType.STEAL, CardRarity.RARE),
            new ActionCard("sabotage_rare", "Conspiracy [Rare]", CardType.SABOTAGE, CardRarity.RARE),
            new ActionCard("give_signature", "Grand Gesture [Signature]", CardType.SIGNATURE_ALTRUIST, CardRarity.SIGNATURE),
            new ActionCard("take_signature", "Grand Heist [Signature]", CardType.SIGNATURE_OPPORTUNIST, CardRarity.SIGNATURE),
            new ActionCard("sabotage_signature", "Master Sabotage [Signature]", CardType.SIGNATURE_SABOTEUR, CardRarity.SIGNATURE)
        );

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setPrefViewportHeight(300);
        scrollPane.setPrefViewportWidth(320);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox list = new VBox(5);
        list.setAlignment(Pos.CENTER);

        for (ActionCard template : templates) {
            Button btn = new Button(template.name());
            btn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 5 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 280;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 5 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 280;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 11px; " +
                    "-fx-padding: 5 15; -fx-background-radius: 6; -fx-cursor: hand; -fx-min-width: 280;"));
            
            btn.setOnAction(e -> {
                mainLayout.getChildren().remove(picker);
                String realName = template.name().replaceAll(" \\[.*\\]", "");
                ActionCard newCard = new ActionCard(template.id(), realName, template.type(), template.rarity());
                humanPlayer.addCard(newCard);
                appendLog("Dev: Added \"" + realName + "\" to hand!", "#10b981");
                updateUI();
            });
            list.getChildren().add(btn);
        }
        scrollPane.setContent(list);
        picker.getChildren().add(scrollPane);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 6 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> mainLayout.getChildren().remove(picker));
        picker.getChildren().add(cancelBtn);

        mainLayout.getChildren().add(picker);
        AnchorPane.setTopAnchor(picker, 120.0);
        AnchorPane.setLeftAnchor(picker, 370.0);
    }

    /** Shows a card picker overlay so the player can choose which card to discard (for merchant). */
    private void showMerchantDiscardPicker(List<ActionCard> hand, Runnable callback) {
        if (isDiscardPopupOpen) return; // block stacking
        isDiscardPopupOpen = true;

        VBox picker = new VBox(12);
        picker.setAlignment(Pos.CENTER);
        picker.setStyle("-fx-background-color: rgba(13,43,62,0.97); -fx-border-color: #ef4444; " +
                "-fx-border-width: 3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 24;");

        Label title = new Label("Choose a Card to Discard");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 18px; -fx-font-weight: bold;");
        picker.getChildren().add(title);

        for (ActionCard c : hand) {
            int reward = getDiscardClamsReward(c.rarity());
            Button cardBtn = new Button(c.name() + "  [" + c.rarity().name() + "]  +" + reward + " Clams");
            cardBtn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 300;");
            cardBtn.setOnMouseEntered(e -> cardBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 300;"));
            cardBtn.setOnMouseExited(e -> cardBtn.setStyle("-fx-background-color: #1e3a52; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 300;"));
            cardBtn.setOnAction(e -> {
                mainLayout.getChildren().remove(picker);
                isDiscardPopupOpen = false; // picker closed, confirmation will re-set
                showDiscardConfirmationWithCallback(c, callback);
            });
            picker.getChildren().add(cardBtn);
        }

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            mainLayout.getChildren().remove(picker);
            isDiscardPopupOpen = false;
        });
        picker.getChildren().add(cancelBtn);

        mainLayout.getChildren().add(picker);
        AnchorPane.setTopAnchor(picker, 160.0);
        AnchorPane.setLeftAnchor(picker, 380.0);
        AnchorPane.setRightAnchor(picker, 380.0);
    }

    private void showGameOverUI() {
        phasePromptLabel.setText("GAME OVER");

        WinnerResult result = gameSession.winner().orElse(null);
        crab.features.gameplay.presentation.GameOverScreen.loadedResult = result;
        crab.features.gameplay.presentation.GameOverScreen.loadedSession = gameSession;
        screens.show(crab.features.gameplay.presentation.GameOverScreen.ID);
        
        /*
        // Full screen ending container
        StackPane endContainer = new StackPane();
        endContainer.prefWidthProperty().bind(mainLayout.widthProperty());
        endContainer.prefHeightProperty().bind(mainLayout.heightProperty());
        endContainer.setStyle("-fx-background-color: black;");

        String accentColor = "#fbbf24"; // Default gold
        String victoryTitle = "It's a Tie!";
        String winnerText = "No player met their class goal.";

        if (result != null) {
            victoryTitle = switch (result.winningClass()) {
                case OPPORTUNIST -> "Opportunist Victory!";
                case ALTRUIST -> "Altruist Victory!";
                case SABOTEUR -> "Saboteur Victory!";
            };
            accentColor = switch (result.winningClass()) {
                case OPPORTUNIST -> "#10b981"; // Emerald
                case ALTRUIST -> "#60a5fa"; // Sky Blue
                case SABOTEUR -> "#ec4899"; // Ruby/Rose
            };
            
            String winnerName = gameSession.players().stream()
                    .filter(p -> p.id().equals(result.playerId()))
                    .map(PlayerState::displayName)
                    .findFirst().orElse(result.playerId());

            winnerText = "Winner: " + winnerName + " - Score: " + result.winningValue();
        }

        // Epic full-screen ending image
        ImageView bgView = new ImageView();
        Image endImg = (result != null) ? getEndingImage(result.winningClass()) : getCrabImage(PlayerClass.ALTRUIST);
        if (endImg != null) {
            bgView.setImage(endImg);
            bgView.fitWidthProperty().bind(endContainer.widthProperty());
            bgView.fitHeightProperty().bind(endContainer.heightProperty());
            // Preserve ratio to true might leave black bars, but false will stretch. Let's use false to fill screen completely!
            bgView.setPreserveRatio(false); 
        }
        
        // Dimming overlay so text remains perfectly readable against the bright animations
        Rectangle dimmingOverlay = new Rectangle();
        dimmingOverlay.widthProperty().bind(endContainer.widthProperty());
        dimmingOverlay.heightProperty().bind(endContainer.heightProperty());
        dimmingOverlay.setFill(Color.color(0, 0, 0, 0.45)); // 45% dark

        // Center UI Layer (Text & Button)
        VBox uiLayer = new VBox(25);
        uiLayer.setAlignment(Pos.CENTER);
        
        Label titleLbl = new Label(victoryTitle);
        titleLbl.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 65px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + "; -fx-effect: dropshadow(three-pass-box, black, 15, 0.8, 0, 0);");

        Label nameLbl = new Label(winnerText);
        nameLbl.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, black, 10, 0.8, 0, 0);");
        
        Button returnBtn = new Button("RETURN TO MAIN MENU");
        returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; " +
                           "-fx-font-size: 24px; " +
                           "-fx-padding: 15 45; " +
                           "-fx-background-color: " + accentColor + "; " +
                           "-fx-text-fill: white; " +
                           "-fx-background-radius: 12; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 4);");

        String finalAccentColor = accentColor;
        returnBtn.setOnMouseEntered(e -> {
            returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; " +
                               "-fx-font-size: 24px; " +
                               "-fx-padding: 15 45; " +
                               "-fx-background-color: white; " +
                               "-fx-text-fill: " + finalAccentColor + "; " +
                               "-fx-background-radius: 12; " +
                               "-fx-cursor: hand; " +
                               "-fx-effect: dropshadow(three-pass-box, " + finalAccentColor + ", 20, 0.6, 0, 0);");
            
            TranslateTransition hoverUp = new TranslateTransition(Duration.millis(120), returnBtn);
            hoverUp.setToY(-5);
            hoverUp.play();
        });

        returnBtn.setOnMouseExited(e -> {
            returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; " +
                               "-fx-font-size: 24px; " +
                               "-fx-padding: 15 45; " +
                               "-fx-background-color: " + finalAccentColor + "; " +
                               "-fx-text-fill: white; " +
                               "-fx-background-radius: 12; " +
                               "-fx-cursor: hand; " +
                               "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 4);");
            
            TranslateTransition hoverDown = new TranslateTransition(Duration.millis(120), returnBtn);
            hoverDown.setToY(0);
            hoverDown.play();
        });

        returnBtn.setOnAction(e -> {
            // Clean up active savegame files so that completed game sessions cannot be resumed
            String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
            if (username != null) {
                String saveFileName = "savegame_" + username + ".dat";
                java.io.File file = new java.io.File(saveFileName);
                if (file.exists()) {
                    file.delete();
                }
                try {
                    crab.appcore.db.DatabaseManager.registerSave(username, null);
                } catch (Exception dbEx) {
                    System.err.println("Could not clear database save: " + dbEx.getMessage());
                }
            }
            screens.show("menu_main");
        });

        uiLayer.getChildren().addAll(titleLbl, nameLbl, returnBtn);
        
        endContainer.getChildren().addAll(bgView, dimmingOverlay, uiLayer);
        mainLayout.getChildren().add(endContainer);

        // Majestic fade-in animation for the full-screen ending
        endContainer.setOpacity(0.0);
        javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(Duration.millis(800), endContainer);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        */
    }
}
