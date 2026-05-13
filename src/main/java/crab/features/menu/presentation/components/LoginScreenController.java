package crab.features.menu.presentation.components;

import crab.appcore.db.DatabaseUserCredentialsRepository;
import crab.features.menu.auth.AuthService;
import crab.features.menu.auth.CrabUser;
import crab.features.menu.auth.DevFallbackUserCredentialsRepository;
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
    public static CrabUser currentUser = CrabUser.demo();

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
    private AuthService authService = createDefaultAuthService();

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

    private static AuthService createDefaultAuthService() {
        PasswordHasher passwordHasher = new PasswordHasher();
        return new AuthService(
                new DevFallbackUserCredentialsRepository(new DatabaseUserCredentialsRepository(), passwordHasher),
                passwordHasher
        );
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
        String password = passwordField.getText();

        // Guest login bypass
        if ("guest".equalsIgnoreCase(username) && "guest".equals(password)) {
            currentUser = new CrabUser(999, "guest", "Guest Crab");
            loggedInUser = "guest";
            errorLabel.setText("");
            loginSuccessAction.run();
            return;
        }

        java.util.Optional<CrabUser> signedInUser = authService.signInUser(username, password);
        if (signedInUser.isEmpty()) {
            errorLabel.setText("Invalid username or password.");
            return;
        }

        errorLabel.setText("");
        currentUser = signedInUser.orElseThrow();
        loggedInUser = currentUser.username();
        loginSuccessAction.run();
    }
}
