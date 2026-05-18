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
    public static PlayerClass selectedClass = PlayerClass.OPPORTUNIST;
    public static String difficulty = "Medium";
    public static boolean isMale = true;
    public static GameSession loadedSession = null;
    public static boolean returningFromEncyclopedia = false;
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
        getGameScene().setBackgroundColor(javafx.scene.paint.Color.web("#0e58d4")); // Ocean blue background

        if (returningFromEncyclopedia) {
            returningFromEncyclopedia = false;
            // Retain active gameSession, humanPlayer, and aiPlayers from mid-game state
        } else if (loadedSession != null) {
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

            // Add Event Filter to capture ESC key presses regardless of child focus
            root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    controller.handleSettings(null);
                    event.consume();
                }
            });
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
        
        humanPlayer = PlayerState.create("human", "Player", selectedClass);

        aiPlayers = new ArrayList<>();
        List<PlayerState> allPlayers = new ArrayList<>();
        allPlayers.add(humanPlayer);

        for (int i = 0; i < requestedEnemyCount; i++) {
            PlayerClass aiClass = classes[random.nextInt(classes.length)];
            PlayerState ai = PlayerState.create("ai_" + i, "Crab Bot " + (i + 1), aiClass);
            aiPlayers.add(ai);
            allPlayers.add(ai);
        }

        gameSession = GameSession.newLocal(allPlayers, 15, createStandardDeck());
    }

    private List<ActionCard> createStandardDeck() {
        List<ActionCard> deck = new ArrayList<>();

        // One of each card type/rarity combo for the rarity-based pull system
        deck.add(new ActionCard("give_common", "Give", CardType.HELP, CardRarity.COMMON));
        deck.add(new ActionCard("take_common", "Take", CardType.STEAL, CardRarity.COMMON));
        deck.add(new ActionCard("sabotage_common", "Sabotage", CardType.SABOTAGE, CardRarity.COMMON));

        deck.add(new ActionCard("give_uncommon", "Generous Give", CardType.HELP, CardRarity.UNCOMMON));
        deck.add(new ActionCard("take_uncommon", "Snatch", CardType.STEAL, CardRarity.UNCOMMON));
        deck.add(new ActionCard("sabotage_uncommon", "Scheme", CardType.SABOTAGE, CardRarity.UNCOMMON));

        deck.add(new ActionCard("give_rare", "Gracious Give", CardType.HELP, CardRarity.RARE));
        deck.add(new ActionCard("take_rare", "Heist", CardType.STEAL, CardRarity.RARE));
        deck.add(new ActionCard("sabotage_rare", "Conspiracy", CardType.SABOTAGE, CardRarity.RARE));
        
        deck.add(new ActionCard("give_signature", "Grand Gesture", CardType.SIGNATURE_ALTRUIST, CardRarity.SIGNATURE));
        deck.add(new ActionCard("take_signature", "Grand Heist", CardType.SIGNATURE_OPPORTUNIST, CardRarity.SIGNATURE));
        deck.add(new ActionCard("sabotage_signature", "Master Sabotage", CardType.SIGNATURE_SABOTEUR, CardRarity.SIGNATURE));

        return deck;
    }
}
