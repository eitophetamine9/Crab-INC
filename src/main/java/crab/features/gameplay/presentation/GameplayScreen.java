package crab.features.gameplay.presentation;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.domain.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Screen for the main gameplay loop.
 * Loads the layout from battle-screen.fxml and delegates UI logic
 * to GameplayScreenController.
 */
public final class GameplayScreen implements GameScreen {
    public static final String ID = "gameplay";

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    public static int requestedEnemyCount = 3;
    public static GameSession loadedSession = null;
    private GameSession gameSession;
    private PlayerState humanPlayer;
    private List<PlayerState> aiPlayers;

    public GameplayScreen(ScreenManager screens) {
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
        getGameScene().setBackgroundColor(Color.WHITE); // Plain white background

        if (loadedSession != null) {
            gameSession = loadedSession;
            humanPlayer = gameSession.players().stream().filter(p -> p.id().equals("human")).findFirst().orElse(null);
            aiPlayers = gameSession.players().stream().filter(p -> p.id().startsWith("ai_")).collect(java.util.stream.Collectors.toList());
            loadedSession = null; // consume it
        } else {
            initializeGameSession();
        }
        

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/battle-screen/battle-screen.fxml"));
            root = loader.load();
            GameplayScreenController controller = loader.getController();
            controller.initData(gameSession, screens, humanPlayer, aiPlayers);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load battle-screen.fxml", e);
        }
        
        getGameScene().addUINode(root);
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
    }

    private void initializeGameSession() {
        java.util.Random random = new java.util.Random();
        PlayerClass[] classes = PlayerClass.values();
        
        PlayerClass humanClass = classes[random.nextInt(classes.length)];
        humanPlayer = PlayerState.create("human", "Player", humanClass);

        aiPlayers = new ArrayList<>();
        List<PlayerState> allPlayers = new ArrayList<>();
        allPlayers.add(humanPlayer);

        for (int i = 0; i < requestedEnemyCount; i++) {
            PlayerClass aiClass = classes[random.nextInt(classes.length)];
            PlayerState ai = PlayerState.create("ai_" + i, "Crab Bot " + (i + 1), aiClass);
            aiPlayers.add(ai);
            allPlayers.add(ai);
        }

        gameSession = GameSession.newLocal(allPlayers, 15, List.of());
    }

    private List<ActionCard> createStandardDeck() {
        List<ActionCard> deck = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            deck.add(new ActionCard("take", "Take", CardType.STEAL, CardRarity.COMMON));
            deck.add(new ActionCard("give", "Give", CardType.HELP, CardRarity.COMMON));
            deck.add(new ActionCard("share", "Share", CardType.HELP, CardRarity.COMMON));
        }
        return deck;
    }
}
