package crab.features.menu.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.menu.presentation.components.SignupScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.particle.ParticleSystem;
import javafx.geometry.Point2D;
import com.almasb.fxgl.core.math.FXGLMath;

public final class SignupScreen implements GameScreen {
    public static final String ID = "menu_signup";

    private final ScreenManager screens;
    private Parent root;
    private boolean visible;

    private javafx.scene.image.ImageView backgroundImageView;
    private ParticleSystem particleSystem;

    public SignupScreen(ScreenManager screens) {
        this.screens = screens;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void show() {
        if (visible) {
            return;
        }

        visible = true;
        
        // GIF Background Integration
        try {
            var resource = getClass().getResource("/assets/textures/bg_underwater.gif");
            if (resource != null) {
                backgroundImageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(resource.toExternalForm()));
                backgroundImageView.setFitWidth(getAppWidth());
                backgroundImageView.setFitHeight(getAppHeight());
                backgroundImageView.setPreserveRatio(false);

                // Ensure it's behind UI
                getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        root = loadView();
        
        // 3D Tilt for Titles
        root.lookupAll(".title-text").forEach(node -> {
            node.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
            node.setRotate(10);
        });

        getGameScene().addUINode(root);
        addBubbles();
    }

    private void addBubbles() {
        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
        emitter.setStartColor(Color.web("rgba(255, 255, 255, 0.3)"));
        emitter.setEndColor(Color.TRANSPARENT);
        emitter.setNumParticles(2);
        emitter.setEmissionRate(0.2);
        emitter.setSize(1, 4);
        emitter.setVelocityFunction(i -> new Point2D(FXGLMath.random(-1, 1), -FXGLMath.random(5, 15)));
        emitter.setAccelerationFunction(() -> new Point2D(0, -0.05));
        emitter.setSpawnPointFunction(i -> new Point2D(FXGLMath.random(0, getAppWidth()), getAppHeight()));
        
        this.particleSystem = new ParticleSystem();
        this.particleSystem.addParticleEmitter(emitter, 0, getAppHeight());
        // Add at index 1 (behind UI at index 2, in front of video at index 0)
        getGameScene().getContentRoot().getChildren().add(1, particleSystem.getPane());
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        visible = false;
        if (root != null) {
            getGameScene().removeUINode(root);
            root = null;
        }

        if (backgroundImageView != null) {
            getGameScene().getContentRoot().getChildren().remove(backgroundImageView);
            backgroundImageView = null;
        }
        if (particleSystem != null) {
            getGameScene().getContentRoot().getChildren().remove(particleSystem.getPane());
            particleSystem = null;
        }
    }

    private Parent loadView() {
        URL resource = getClass().getResource("/fxml/menu/signup-screen.fxml");
        if (resource == null) {
            return fallbackLabel("Missing /fxml/menu/signup-screen.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent loadedRoot = loader.load();
            SignupScreenController controller = loader.getController();
            controller.setBackToLoginAction(() -> screens.show(LoginScreen.ID));
            controller.setSignupSuccessHandler(message -> {
                LoginScreen.setPendingStatusMessage(message);
                screens.show(LoginScreen.ID);
            });
            return loadedRoot;
        } catch (IOException exception) {
            return fallbackLabel("Signup screen load failed: " + exception.getMessage());
        }
    }

    private static Label fallbackLabel(String message) {
        Label fallback = new Label(message);
        fallback.setTextFill(Color.WHITE);
        return fallback;
    }
}
