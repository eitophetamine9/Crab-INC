package crab.features.menu.presentation.components;

import crab.appcore.db.DatabaseUserCredentialsRepository;
import crab.features.menu.auth.AuthService;
import crab.features.menu.auth.DevFallbackUserCredentialsRepository;
import crab.features.menu.auth.PasswordHasher;
import crab.features.menu.auth.SignUpResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
        SignUpResult result = authService.signUp(
                usernameField.getText(),
                passwordField.getText(),
                confirmPasswordField.getText()
        );

        switch (result) {
            case CREATED -> signupSuccessHandler.onSignupSuccess("Account created. Please log in.");
            case BLANK_INPUT -> errorLabel.setText("Please enter a username and password.");
            case PASSWORD_MISMATCH -> errorLabel.setText("Passwords do not match.");
            case USERNAME_TAKEN -> errorLabel.setText("Username already exists.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        errorLabel.setText("");
        backToLoginAction.run();
    }

    @FunctionalInterface
    public interface SignupSuccessHandler {
        void onSignupSuccess(String message);
    }
}
