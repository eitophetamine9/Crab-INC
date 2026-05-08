package crab.features.gameplay.presentation;

import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameplayScreenController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private Label topGoldLabel;

    @FXML
    private HBox battlefieldArea;

    @FXML
    private Label phasePromptLabel;

    @FXML
    private VBox heroAvatar;

    @FXML
    private Label heroClassLabel;

    @FXML
    private Label heroNameLabel;

    @FXML
    private Label heroWealthLabel;

    @FXML
    private Label heroReputationLabel;

    @FXML
    private Label heroInfamyLabel;

    @FXML
    private HBox handArea;

    @FXML
    private Button endTurnBtn;

    @FXML
    private Label roundGemLabel;

    private ScreenManager screens;
    private GameSession gameSession;
    private PlayerState humanPlayer;
    private PlayerState aiPlayer;
    private ActionCard lastAiCardPlayed;

    private final Random random = new Random();

    private String getGoalString(PlayerClass playerClass) {
        return switch (playerClass) {
            case OPPORTUNIST -> "Goal: Wealth";
            case ALTRUIST -> "Goal: Reputation";
            case SABOTEUR -> "Goal: Infamy";
        };
    }

    public void initData(GameSession gameSession, ScreenManager screens, PlayerState humanPlayer, PlayerState aiPlayer) {
        this.gameSession = gameSession;
        this.screens = screens;
        this.humanPlayer = humanPlayer;
        this.aiPlayer = aiPlayer;
        updateUI();
    }

    @FXML
    void handleSettings(ActionEvent event) {
        screens.show("menu_main");
    }

    private void updateUI() {
        if (gameSession == null) return;

        // Top Bar Updates (Human stats)
        topGoldLabel.setText("Gold: " + humanPlayer.gold());
        
        roundGemLabel.setText("Round: " + gameSession.currentRound());

        // Battlefield Updates (Enemy avatars/cards)
        battlefieldArea.getChildren().clear();
        battlefieldArea.getChildren().add(createEnemyCardView(aiPlayer));
        
        // Hero Avatar Updates
        heroClassLabel.setText(humanPlayer.playerClass().name() + "\n(" + getGoalString(humanPlayer.playerClass()) + ")");
        heroNameLabel.setText(humanPlayer.displayName());
        heroWealthLabel.setText("Wealth: " + humanPlayer.wealth());
        heroReputationLabel.setText("Reputation: " + humanPlayer.reputation());
        heroInfamyLabel.setText("Infamy: " + humanPlayer.infamy());

        // Hand Updates
        handArea.getChildren().clear();

        // Phase specific logic
        endTurnBtn.setDisable(true);
        endTurnBtn.setOnAction(null);

        switch (gameSession.phase()) {
            case DEVELOPMENT -> {
                gameSession.resolveDevelopment(java.util.Map.of(humanPlayer.id(), 0, aiPlayer.id(), 0), java.util.Set.of());
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

    private VBox createEnemyCardView(PlayerState enemy) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPrefSize(140, 200);
        cardBox.setStyle("-fx-border-color: black;");
        
        Label enemyClass = new Label(enemy.playerClass().name() + "\n(" + getGoalString(enemy.playerClass()) + ")");
        enemyClass.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label enemyName = new Label(enemy.displayName());
        Label enemyWealth = new Label("Wealth: " + enemy.wealth());
        Label enemyRep = new Label("Reputation: " + enemy.reputation());
        Label enemyInfamy = new Label("Infamy: " + enemy.infamy());
        
        cardBox.getChildren().addAll(enemyClass, enemyName, enemyWealth, enemyRep, enemyInfamy);
        return cardBox;
    }

    private void showActionUI() {
        phasePromptLabel.setText("Phase: Play a card");
        
        if (humanPlayer.hand().isEmpty()) {
            endTurnBtn.setDisable(false);
            endTurnBtn.setText("PASS");
            endTurnBtn.setOnAction(e -> {
                gameSession.submitAction(new PlayerAction(humanPlayer.id(), new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON, 0), aiPlayer.id()));
                submitAiAction();
                updateUI();
            });
            return;
        }

        endTurnBtn.setText("END PHASE");

        List<ActionCard> hand = new java.util.ArrayList<>(humanPlayer.hand());
        hand.sort(java.util.Comparator.comparingInt(c -> java.util.List.of("Take", "Give", "Share").indexOf(c.name())));
        for (ActionCard card : hand) {
            VBox cardView = createHandCardView(card, () -> {
                gameSession.submitAction(new PlayerAction(humanPlayer.id(), card, aiPlayer.id()));
                submitAiAction();
                updateUI();
            });
            handArea.getChildren().add(cardView);
        }
    }

    private void submitAiAction() {
        if (aiPlayer.hand().isEmpty()) {
            lastAiCardPlayed = new ActionCard("dummy", "Pass", CardType.HELP, CardRarity.COMMON, 0);
            gameSession.submitAction(new PlayerAction(aiPlayer.id(), lastAiCardPlayed, humanPlayer.id()));
            return;
        }
        ActionCard aiCard = aiPlayer.hand().get(random.nextInt(aiPlayer.hand().size()));
        lastAiCardPlayed = aiCard;
        gameSession.submitAction(new PlayerAction(aiPlayer.id(), aiCard, humanPlayer.id()));
    }

    private void showResolutionUI() {
        phasePromptLabel.setText("Phase: Resolution");
        
        if (lastAiCardPlayed != null) {
            VBox playedCard = createHandCardView(lastAiCardPlayed, null);
            battlefieldArea.getChildren().add(playedCard);
        }

        endTurnBtn.setDisable(false);
        endTurnBtn.setText("RESOLVE");
        endTurnBtn.setOnAction(e -> {
            gameSession.resolveActions();
            lastAiCardPlayed = null;
            updateUI();
        });
    }

    private VBox createHandCardView(ActionCard card, Runnable onClickAction) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPrefSize(100, 150);
        cardBox.setStyle("-fx-border-color: black;");
        
        Label name = new Label(card.name());
        name.setWrapText(true);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        Label type = new Label(card.type().name());
        Label desc = new Label("Power: " + card.baseEffect());
        
        cardBox.getChildren().addAll(name, spacer, type, desc);
        
        if (onClickAction != null) {
            cardBox.setOnMouseClicked(e -> onClickAction.run());
        }
        
        return cardBox;
    }

    private void showGameOverUI() {
        phasePromptLabel.setText("GAME OVER");
        
        Label winnerLabel = new Label(gameSession.winner()
            .map(w -> "Winner: " + (w.playerId().equals(humanPlayer.id()) ? "You!" : "Crab Bot!"))
            .orElse("It's a tie!"));

        Button returnBtn = new Button("Return to Menu");
        returnBtn.setOnAction(e -> screens.show("menu_main"));

        VBox overBox = new VBox(10, winnerLabel, returnBtn);
        overBox.setAlignment(Pos.CENTER);
        overBox.setStyle("-fx-border-color: black; -fx-padding: 20; -fx-background-color: white;");
        
        VBox centerStack = (VBox) mainLayout.getCenter();
        centerStack.getChildren().clear();
        centerStack.getChildren().add(overBox);
    }
}
