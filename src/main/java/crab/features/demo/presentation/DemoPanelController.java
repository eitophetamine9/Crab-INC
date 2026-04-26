package crab.features.demo.presentation;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the FXML sample panel in the foundation demo.
 *
 * Design patterns:
 * - MVC Controller: mediates the FXML view state.
 *
 * SOLID:
 * - Single Responsibility: owns demo panel UI text updates only.
 */
public final class DemoPanelController {
    @FXML
    private Label statusLabel;

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }
}
