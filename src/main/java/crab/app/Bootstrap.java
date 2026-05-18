package crab.app;

import com.almasb.fxgl.app.GameSettings;
import crab.appcore.context.GameContext;
import crab.appcore.context.ModuleRegistry;
import crab.appcore.screen.ScreenManager;

import crab.features.menu.MenuModule;
import crab.features.menu.presentation.screens.LoginScreen;
import javafx.scene.Cursor;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Builds the project-owned application foundation on top of FXGL.
 *
 * Design patterns:
 * - Facade: centralizes startup/shutdown for the application layer.
 *
 * SOLID:
 * - Single Responsibility: wires settings, context, and modules.
 * - Open/Closed: new modules can be registered without changing FXGL entrypoint code.
 */
public final class Bootstrap {
    private final GameContext context = new GameContext();
    private final ModuleRegistry modules = new ModuleRegistry();
    private final ScreenManager screens = new ScreenManager();
    private boolean modulesInitialized;

    public void configure(GameSettings settings) {
        settings.setWidth(1080);
        settings.setHeight(720);
        settings.setTitle("Crab Inc.");
        settings.setVersion("0.1");
        settings.setDeveloperMenuEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        settings.setIntroEnabled(false);
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(true);
        settings.setSceneFactory(new CrabSceneFactory());
    }

    public void initializeGame() {
        if (!modulesInitialized) {
            context.register(ScreenManager.class, screens);
            modules.register(new MenuModule());
            modules.register(new crab.features.gameplay.GameplayModule());

            modules.initialize(context);
            modulesInitialized = true;
        }

        modules.start();
    }

    public void initializeUi() {
        AppTypography.applyTo(getGameScene().getRoot());
        
        // Prevent stretching/scaling: force ContentRoot and UiRoot to remain exactly 1.0 scale
        getGameScene().getContentRoot().scaleXProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() != 1.0) getGameScene().getContentRoot().setScaleX(1.0);
        });
        getGameScene().getContentRoot().scaleYProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() != 1.0) getGameScene().getContentRoot().setScaleY(1.0);
        });
        getGameScene().getUIRoot().scaleXProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() != 1.0) getGameScene().getUIRoot().setScaleX(1.0);
        });
        getGameScene().getUIRoot().scaleYProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() != 1.0) getGameScene().getUIRoot().setScaleY(1.0);
        });

        modules.initializeUi();
        getGameScene().setCursor(Cursor.DEFAULT);
        getGameScene().setBackgroundColor(javafx.scene.paint.Color.web("#0e58d4"));
        if (screens.currentId().isEmpty()) {
            java.io.File sessionFile = new java.io.File("session.txt");
            boolean sessionLoaded = false;
            if (sessionFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(sessionFile))) {
                    String username = reader.readLine();
                    String displayName = reader.readLine();
                    String idStr = reader.readLine();
                    if (username != null && displayName != null && idStr != null) {
                        long id = Long.parseLong(idStr.trim());
                        crab.features.menu.presentation.components.LoginScreenController.currentUser = 
                                new crab.features.menu.auth.CrabUser(id, username, displayName);
                        crab.features.menu.presentation.components.LoginScreenController.loggedInUser = username;
                        sessionLoaded = true;
                    }
                } catch (Exception ex) {
                    System.err.println("Could not restore persistent session: " + ex.getMessage());
                }
            }

            if (sessionLoaded) {
                screens.show("menu_main");
            } else {
                screens.show(LoginScreen.ID);
            }
        }
    }

    public void update(double tpf) {
        modules.update(tpf);
        screens.update(tpf);
    }

    public void shutdown() {
        screens.clear();
        modules.stop();
    }
}
