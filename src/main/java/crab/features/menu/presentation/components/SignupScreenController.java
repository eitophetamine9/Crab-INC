package crab.features.menu.presentation.components;

import crab.appcore.db.DatabaseUserCredentialsRepository;
import crab.features.menu.auth.AuthService;
import crab.features.menu.auth.DevFallbackUserCredentialsRepository;
import crab.features.menu.auth.PasswordHasher;
import crab.features.menu.auth.SignUpResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.Objects;

public final class SignupScreenController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button signupButton;
    @FXML
    private Button backToLoginButton;

    @FXML
    private void initialize() {
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        // Allow pressing Enter in any field to trigger sign-up
        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleSignup(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleSignup(); });
        confirmPasswordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleSignup(); });
    }

    private Runnable backToLoginAction = () -> {
    };
    private SignupSuccessHandler signupSuccessHandler = message -> {
    };
    private AuthService authService = createDefaultAuthService();

    public void setBackToLoginAction(Runnable action) {
        backToLoginAction = Objects.requireNonNull(action, "action");
    }

    public void setSignupSuccessHandler(SignupSuccessHandler handler) {
        signupSuccessHandler = Objects.requireNonNull(handler, "handler");
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
    private void handleSignup() {
        if (signupButton != null && signupButton.isDisable()) return; // block double clicks

        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // 1. Show loading state & disable controls
        if (signupButton != null) {
            signupButton.setDisable(true);
            signupButton.setText("Registering...");
        }
        if (backToLoginButton != null) backToLoginButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        confirmPasswordField.setDisable(true);
        errorLabel.setStyle("-fx-text-fill: #60a5fa; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        errorLabel.setText("Submitting details to server...");

        // 2. Perform DB write on a background thread using Task
        Task<SignUpResult> signupTask = new Task<>() {
            @Override
            protected SignUpResult call() throws Exception {
                // Runs strictly off-thread (no UI freeze!)
                return authService.signUp(username, password, confirm);
            }
        };

        // 3. Success callback (back on JavaFX Application Thread)
        signupTask.setOnSucceeded(e -> {
            // Restore control states
            if (signupButton != null) {
                signupButton.setDisable(false);
                signupButton.setText("Sign Up");
            }
            if (backToLoginButton != null) backToLoginButton.setDisable(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            confirmPasswordField.setDisable(false);

            SignUpResult result = signupTask.getValue();
            switch (result) {
                case CREATED -> {
                    errorLabel.setStyle("-fx-text-fill: #34d399; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                    signupSuccessHandler.onSignupSuccess("Account created. Please log in.");
                }
                case BLANK_INPUT -> {
                    errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                    errorLabel.setText("Please enter a username and password.");
                }
                case PASSWORD_MISMATCH -> {
                    errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                    errorLabel.setText("Passwords do not match.");
                }
                case USERNAME_TAKEN -> {
                    errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                    errorLabel.setText("Username already exists.");
                }
                case DATABASE_ERROR -> {
                    errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
                    errorLabel.setText("Database error. Check connection.");
                }
            }
        });

        // 4. Failure callback (back on JavaFX Application Thread)
        signupTask.setOnFailed(e -> {
            // Restore control states
            if (signupButton != null) {
                signupButton.setDisable(false);
                signupButton.setText("Sign Up");
            }
            if (backToLoginButton != null) backToLoginButton.setDisable(false);
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            confirmPasswordField.setDisable(false);

            errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
            errorLabel.setText("Database connection timed out or refused.");
            Throwable ex = signupTask.getException();
            if (ex != null) ex.printStackTrace();
        });

        // 5. Fire background thread
        Thread t = new Thread(signupTask);
        t.setDaemon(true); // lets JVM exit cleanly if app closed
        t.start();
    }

    @FXML
    private void handleBackToLogin() {
        if (signupButton != null && signupButton.isDisable()) return; // block navigation during query
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0); -fx-font-weight: bold;");
        errorLabel.setText("");
        backToLoginAction.run();
    }


    @FunctionalInterface
    public interface SignupSuccessHandler {
        void onSignupSuccess(String message);
    }
}
