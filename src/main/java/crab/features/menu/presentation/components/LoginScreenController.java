package crab.features.menu.presentation.components;

import crab.appcore.db.DatabaseUserCredentialsRepository;
import crab.features.menu.auth.AuthService;
import crab.features.menu.auth.CrabUser;
import crab.features.menu.auth.DevFallbackUserCredentialsRepository;
import crab.features.menu.auth.PasswordHasher;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
    @FXML
    private Button loginButton;
    @FXML
    private Button createAccountButton;

    @FXML
    private void initialize() {
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        // Allow pressing Enter in any field to trigger login
        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });
    }

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

    public void setStatusMessage(String message, boolean isSuccess) {
        errorLabel.setText(message == null ? "" : message);
        String color = isSuccess ? "#34d399" : "#f87171";
        errorLabel.setStyle("-fx-text-fill: " + color + "; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
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
        if (loginButton != null && loginButton.isDisable()) return; // block navigation during query
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        errorLabel.setText("");
        createAccountAction.run();
    }

    @FXML
    private void handleLogin() {
        if (loginButton != null && loginButton.isDisable()) return; // block double clicks

        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
            errorLabel.setText("Please enter a username and password.");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Guest login bypass (immediate)
        if ("guest".equalsIgnoreCase(username) && "guest".equals(password)) {
            currentUser = new CrabUser(999, "guest", "Guest Crab");
            loggedInUser = "guest";
            errorLabel.setText("");
            loginSuccessAction.run();
            return;
        }

        // 1. Show loading state & disable controls
        if (loginButton != null) {
            loginButton.setDisable(true);
            loginButton.setText("Connecting...");
        }
        if (createAccountButton != null) createAccountButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        errorLabel.setStyle("-fx-text-fill: #60a5fa; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        errorLabel.setText("Validating credentials with server...");

        // 2. Perform DB check on a background thread using Task
        Task<java.util.Optional<CrabUser>> loginTask = new Task<>() {
            @Override
            protected java.util.Optional<CrabUser> call() throws Exception {
                // Runs strictly off-thread (no UI freeze!)
                return authService.signInUser(username, password);
            }
        };

        // 3. Success callback (back on JavaFX Application Thread)
        loginTask.setOnSucceeded(e -> {
            // Restore control states
            if (loginButton != null) {
                loginButton.setDisable(false);
                loginButton.setText("Login");
            }
            if (createAccountButton != null) createAccountButton.setDisable(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);

            java.util.Optional<CrabUser> signedInUser = loginTask.getValue();
            if (signedInUser.isEmpty()) {
                errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                errorLabel.setText("Invalid username or password.");
            } else {
                errorLabel.setText("");
                currentUser = signedInUser.orElseThrow();
                loggedInUser = currentUser.username();
                loginSuccessAction.run();
            }
        });

        // 4. Failure callback (back on JavaFX Application Thread)
        loginTask.setOnFailed(e -> {
            // Restore control states
            if (loginButton != null) {
                loginButton.setDisable(false);
                loginButton.setText("Login");
            }
            if (createAccountButton != null) createAccountButton.setDisable(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);

            errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
            errorLabel.setText("Database connection timed out or refused.");
            Throwable ex = loginTask.getException();
            if (ex != null) ex.printStackTrace();
        });

        // 5. Fire background thread
        Thread t = new Thread(loginTask);
        t.setDaemon(true); // lets JVM exit cleanly if app closed
        t.start();
    }
}
