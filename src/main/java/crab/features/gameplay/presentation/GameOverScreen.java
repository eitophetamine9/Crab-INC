package crab.features.gameplay.presentation;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.GameSession;
import crab.features.gameplay.domain.PlayerClass;
import crab.features.gameplay.domain.PlayerState;
import crab.features.gameplay.domain.WinnerResult;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

public final class GameOverScreen implements GameScreen {
    public static final String ID = "game_over";

    private final ScreenManager screens;
    private StackPane root;
    private boolean visible;

    public static WinnerResult loadedResult = null;
    public static GameSession loadedSession = null;

    public GameOverScreen(ScreenManager screens) {
        this.screens = screens;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void show() {
        if (visible) return;
        visible = true;

        WinnerResult result = loadedResult;
        GameSession gameSession = loadedSession;

        // Asynchronously save human player's career statistics to player_stats
        if (gameSession != null) {
            PlayerState human = gameSession.players().stream()
                    .filter(p -> p.id().equals("human"))
                    .findFirst()
                    .orElse(null);
            if (human != null) {
                int finalScore = switch (human.playerClass()) {
                    case OPPORTUNIST -> human.wealth();
                    case ALTRUIST -> human.reputation();
                    case SABOTEUR -> -human.reputation();
                };
                String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
                if (username != null && !username.isBlank() && !"guest".equalsIgnoreCase(username)) {
                    Thread t = new Thread(() -> {
                        crab.appcore.db.DatabaseManager.recordPlayerStats(username, finalScore);
                    });
                    t.setDaemon(true);
                    t.start();
                }
            }
        }

        root = new StackPane();
        root.setPrefSize(1080, 720);
        root.setStyle("-fx-background-color: black;");

        String accentColor = "#fbbf24";
        String victoryTitle = "It's a Tie!";
        String winnerText = "No player met their class goal.";

        if (result != null) {
            victoryTitle = switch (result.winningClass()) {
                case OPPORTUNIST -> "Opportunist Victory!";
                case ALTRUIST -> "Altruist Victory!";
                case SABOTEUR -> "Saboteur Victory!";
            };
            accentColor = switch (result.winningClass()) {
                case OPPORTUNIST -> "#10b981";
                case ALTRUIST -> "#60a5fa";
                case SABOTEUR -> "#ec4899";
            };
            
            String winnerName = gameSession.players().stream()
                    .filter(p -> p.id().equals(result.playerId()))
                    .map(PlayerState::displayName)
                    .findFirst().orElse(result.playerId());

            winnerText = "Winner: " + winnerName + " - Score: " + result.winningValue();
        }

        root.setMinSize(com.almasb.fxgl.dsl.FXGL.getAppWidth(), com.almasb.fxgl.dsl.FXGL.getAppHeight());
        root.setMaxSize(com.almasb.fxgl.dsl.FXGL.getAppWidth(), com.almasb.fxgl.dsl.FXGL.getAppHeight());

        // Background dimming overlay
        Rectangle dimmingOverlay = new Rectangle();
        dimmingOverlay.widthProperty().bind(root.widthProperty());
        dimmingOverlay.heightProperty().bind(root.heightProperty());
        dimmingOverlay.setFill(Color.color(0, 0, 0, 0.7));

        // Center Panel (The "Small Box")
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: rgba(13, 43, 62, 0.95); -fx-border-color: " + accentColor + "; -fx-border-width: 4; -fx-padding: 30; -fx-background-radius: 20; -fx-border-radius: 20;");
        panel.setMaxSize(700, 550);
        
        Label titleLbl = new Label(victoryTitle);
        titleLbl.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 50px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + "; -fx-effect: dropshadow(three-pass-box, black, 10, 0.8, 0, 0);");

        // The Ending Image inside the panel
        ImageView bgView = new ImageView();
        Image endImg = (result != null) ? getEndingImage(result.winningClass()) : getCrabImage(PlayerClass.ALTRUIST);
        if (endImg != null) {
            bgView.setImage(endImg);
            bgView.setFitWidth(600);
            bgView.setFitHeight(300);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            // Add a subtle border to the image
            bgView.setStyle("-fx-effect: dropshadow(three-pass-box, black, 10, 0.5, 0, 0);");
        }

        String nameColor = "white";
        if (result != null) {
            if ("human".equals(result.playerId())) {
                nameColor = "#22c55e"; // bright green if the player won
            } else {
                nameColor = "#ef4444"; // bright red if the enemy won
            }
        }

        Label nameLbl = new Label(winnerText);
        nameLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + nameColor + "; -fx-effect: dropshadow(three-pass-box, black, 5, 0.8, 0, 0);");
        
        Button returnBtn = new Button("RETURN TO MAIN MENU");
        returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 20px; -fx-padding: 10 30; -fx-background-color: " + accentColor + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        String finalAccentColor = accentColor;
        returnBtn.setOnMouseEntered(e -> returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 20px; -fx-padding: 10 30; -fx-background-color: white; -fx-text-fill: " + finalAccentColor + "; -fx-background-radius: 8; -fx-cursor: hand;"));
        returnBtn.setOnMouseExited(e -> returnBtn.setStyle("-fx-font-family: 'Luckiest Guy'; -fx-font-size: 20px; -fx-padding: 10 30; -fx-background-color: " + finalAccentColor + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));

        returnBtn.setOnAction(e -> {
            String username = crab.features.menu.presentation.components.LoginScreenController.loggedInUser;
            if (username != null) {
                String saveFileName = "savegame_" + username + ".dat";
                java.io.File file = new java.io.File(saveFileName);
                if (file.exists()) file.delete();
                try {
                    crab.appcore.db.DatabaseManager.registerSave(username, null);
                } catch (Exception dbEx) {}
            }
            screens.show("menu_main");
        });

        panel.getChildren().addAll(titleLbl, bgView, nameLbl, returnBtn);
        root.getChildren().addAll(dimmingOverlay, panel);
        
        getGameScene().addUINode(root);

        root.setOpacity(0.0);
        javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(Duration.millis(800), root);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    @Override
    public void hide() {
        if (!visible) return;
        visible = false;
        if (root != null) {
            getGameScene().removeUINode(root);
            root = null;
        }
    }

    private Image getEndingImage(PlayerClass playerClass) {
        String fileName = switch (playerClass) {
            case SABOTEUR -> "saboteur_end_screen.gif";
            case ALTRUIST -> "altruist_end_screen.gif";
            case OPPORTUNIST -> "opportunist_end_screen.png";
        };
        var res = getClass().getResource("/assets/endings/" + fileName);
        if (res != null) {
            return new Image(res.toExternalForm());
        }
        return null;
    }

    private Image getCrabImage(PlayerClass playerClass) {
        String base = switch (playerClass) {
            case SABOTEUR -> "sabotuer";
            case ALTRUIST -> "altruist";
            case OPPORTUNIST -> "oppurtunist";
        };
        var res = getClass().getResource("/assets/crab-art/" + base + "_idle.gif");
        if (res != null) {
            return new Image(res.toExternalForm());
        }
        return null;
    }
}
