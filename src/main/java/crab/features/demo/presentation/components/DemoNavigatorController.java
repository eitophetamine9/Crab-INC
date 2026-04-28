package crab.features.demo.presentation.components;

import crab.features.demo.presentation.screens.BoxDemoScreen;
import crab.features.demo.presentation.screens.BunnyDemoScreen;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the shared bottom navigator used by demo screens.
 *
 * Design patterns:
 * - MVC Controller: mediates the shared navigation component state.
 *
 * SOLID:
 * - Single Responsibility: owns demo navigator UI controls only.
 */
public final class DemoNavigatorController {
    @FXML
    private Button boxButton;
    @FXML
    private Button bunnyButton;
    @FXML
    private Button menuButton;

    private Runnable showBoxAction = () -> {
    };
    private Runnable showBunnyAction = () -> {
    };
    private Runnable showMenuAction = () -> {
    };

    public void setNavigationActions(Runnable onShowBox, Runnable onShowBunny, Runnable onShowMenu) {
        showBoxAction = onShowBox;
        showBunnyAction = onShowBunny;
        showMenuAction = onShowMenu;
    }

    public void setActiveScreen(String screenId) {
        boxButton.setDisable(BoxDemoScreen.ID.equals(screenId));
        bunnyButton.setDisable(BunnyDemoScreen.ID.equals(screenId));
        menuButton.setDisable(false);
    }

    @FXML
    private void showBox() {
        showBoxAction.run();
    }

    @FXML
    private void showBunny() {
        showBunnyAction.run();
    }

    @FXML
    private void showMenu() {
        showMenuAction.run();
    }
}
