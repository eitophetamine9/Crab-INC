package com.crabinc.controllers;

import com.crabinc.app.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    protected void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Temporary logic for Week 1
        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Please enter all fields!");
        } else {
            try {
                System.out.println("Login attempted by: " + user);
                // Tells SceneManager to swap to the Menu
                SceneManager.switchScene("main-menu-view.fxml");
            } catch (IOException e) {
                errorLabel.setText("Error loading Main Menu.");
                e.printStackTrace();
            }
        }
    }
}