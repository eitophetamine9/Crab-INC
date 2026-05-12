package crab.app;

import com.almasb.fxgl.app.scene.StartupScene;
import javafx.scene.paint.Color;

/**
 * Empty startup scene that suppresses FXGL's default blue startup symbol.
 *
 * Design patterns:
 * - Null Object style scene: fulfills startup contract without extra presentation.
 *
 * SOLID:
 * - Single Responsibility: removes framework startup branding only.
 */
public final class BlankStartupScene extends StartupScene {
    public BlankStartupScene(int width, int height) {
        super(width, height);
        setBackgroundColor(Color.BLACK);

        try {
            var resource = getClass().getResource("/assets/textures/underwater_scene.mp4");
            if (resource != null) {
                javafx.scene.media.Media media = new javafx.scene.media.Media(resource.toExternalForm());
                javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
                mediaPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);

                javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(mediaPlayer);
                mediaView.setPreserveRatio(false);
                mediaView.setFitWidth(width);
                mediaView.setFitHeight(height);

                getContentRoot().getChildren().add(0, mediaView);
                mediaPlayer.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
