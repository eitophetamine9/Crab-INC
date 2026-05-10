package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.gameplay.presentation.GameplayScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

public final class SetupScreen implements GameScreen {
    public static final String ID = "setup";
    private static final double APP_WIDTH = 1024;
    private static final double APP_HEIGHT = 720;
    private static final double PANEL_WIDTH = 560;

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    public SetupScreen(ScreenManager screens) {
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
        getGameScene().setBackgroundColor(Color.rgb(13, 22, 34));
        root = createView();
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

    private Parent createView() {
        Label title = new Label("Game Setup");
        title.getStyleClass().add("title-text");
        
        Label subtitle = new Label("Select Number of Enemies:");
        subtitle.getStyleClass().add("subtitle-text");

        Slider enemySlider = new Slider(3, 7, 3);
        enemySlider.setShowTickLabels(true);
        enemySlider.setShowTickMarks(true);
        enemySlider.setMajorTickUnit(1);
        enemySlider.setMinorTickCount(0);
        enemySlider.setSnapToTicks(true);
        enemySlider.setPrefWidth(300);
        
        Label enemyCountLabel = new Label("3 Enemies");
        enemyCountLabel.getStyleClass().add("subtitle-text");
        enemySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            enemyCountLabel.setText(newVal.intValue() + " Enemies");
        });

        Button startBtn = new Button("Start Game");
        startBtn.getStyleClass().add("menu-button");
        startBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #10b981, derive(#10b981, -30%));");
        startBtn.setPrefWidth(200);
        startBtn.setOnAction(e -> {
            GameplayScreen.requestedEnemyCount = (int) enemySlider.getValue();
            screens.show(GameplayScreen.ID);
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("menu-button");
        backBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #64748b, derive(#64748b, -30%));");
        backBtn.setPrefWidth(200);
        backBtn.setOnAction(e -> screens.show(MainMenuScreen.ID));

        VBox menu = new VBox(20, title, subtitle, enemySlider, enemyCountLabel, startBtn, backBtn);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(40));
        menu.setPrefWidth(PANEL_WIDTH);
        menu.setMaxWidth(PANEL_WIDTH);
        menu.getStyleClass().add("menu-panel");

        String cssPath = "/assets/ui/cartoon-style.css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            menu.getStylesheets().add(resource.toExternalForm());
        }

        menu.setTranslateX((APP_WIDTH - PANEL_WIDTH) / 2.0);
        menu.setTranslateY((APP_HEIGHT - 500) / 2.0);
        return menu;
    }
}
