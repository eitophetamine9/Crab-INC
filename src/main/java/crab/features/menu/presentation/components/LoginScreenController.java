package crab.features.menu.presentation.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Objects;

/**
 * Controller for the login form FXML.
 *
 * Design patterns:
 * - MVC Controller: validates login input and delegates successful navigation.
 *
 * SOLID:
 * - Single Responsibility: owns login form interaction only.
 */
public final class LoginScreenController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private Runnable loginSuccessAction = () -> {
    };

    public void setLoginSuccessAction(Runnable action) {
        loginSuccessAction = Objects.requireNonNull(action, "action");
    }

    @FXML
    private void handleLogin() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setText("Please enter a username and password.");
            return;
        }

        errorLabel.setText("");
        loginSuccessAction.run();
    }
}
