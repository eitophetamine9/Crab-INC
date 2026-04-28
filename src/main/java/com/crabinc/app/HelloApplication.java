package com.crabinc.app;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize the manager with your stage
        SceneManager.setStage(stage);
        stage.setTitle("Crab Inc. - Login");

        // Start with the login view
        SceneManager.switchScene("login-view.fxml");
    }

    public static void main(String[] args) {
        launch();
    }
}