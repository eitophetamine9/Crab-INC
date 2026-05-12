package crab.features.menu.presentation.components;

import crab.appcore.db.DatabaseUserCredentialsRepository;
import crab.features.menu.auth.AuthService;
import crab.features.menu.auth.PasswordHasher;
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
    public static String loggedInUser = "";

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private Runnable createAccountAction = () -> {
    };
    private Runnable loginSuccessAction = () -> {
    };
    private AuthService authService = new AuthService(
            new DatabaseUserCredentialsRepository(),
            new PasswordHasher()
    );

    public void setLoginSuccessAction(Runnable action) {
        loginSuccessAction = Objects.requireNonNull(action, "action");
    }

    public void setCreateAccountAction(Runnable action) {
        createAccountAction = Objects.requireNonNull(action, "action");
    }

    public void setStatusMessage(String message) {
        errorLabel.setText(message == null ? "" : message);
    }

    public void setAuthService(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    @FXML
    private void handleCreateAccount() {
        errorLabel.setText("");
        createAccountAction.run();
    }

    @FXML
    private void handleLogin() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setText("Please enter a username and password.");
            return;
        }

        String username = usernameField.getText().trim();
        if (!authService.signIn(username, passwordField.getText())) {
            errorLabel.setText("Invalid username or password.");
            return;
        }

        errorLabel.setText("");
        loggedInUser = username;
        loginSuccessAction.run();
    }
}
