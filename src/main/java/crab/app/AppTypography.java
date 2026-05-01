package crab.app;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

/**
 * Registers and applies the application-wide type system.
 */
public final class AppTypography {
    private static final String APP_ROOT_STYLE_CLASS = "crab-app-root";
    private static final String FONT_RESOURCE = "/assets/fonts/bunny_font.otf";
    private static final String STYLESHEET_RESOURCE = "/styles/application.css";

    private static boolean fontLoaded;

    private AppTypography() {
    }

    public static void initialize() {
        loadDefaultFont();
    }

    public static void applyTo(Parent root) {
        initialize();
        if (!root.getStyleClass().contains(APP_ROOT_STYLE_CLASS)) {
            root.getStyleClass().add(APP_ROOT_STYLE_CLASS);
        }
        stylesheetUrl().ifPresent(stylesheet -> {
            if (!root.getStylesheets().contains(stylesheet)) {
                root.getStylesheets().add(stylesheet);
            }
        });
    }

    public static void applyTo(Scene scene) {
        initialize();
        stylesheetUrl().ifPresent(stylesheet -> {
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        });
    }

    private static void loadDefaultFont() {
        if (fontLoaded) {
            return;
        }

        try (InputStream fontStream = AppTypography.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream != null) {
                Font.loadFont(fontStream, 12);
            }
        } catch (Exception ignored) {
            // JavaFX falls back to the platform default if the bundled font cannot be loaded.
        }

        fontLoaded = true;
    }

    private static Optional<String> stylesheetUrl() {
        URL resource = AppTypography.class.getResource(STYLESHEET_RESOURCE);
        return resource == null
                ? Optional.empty()
                : Optional.of(resource.toExternalForm());
    }
}
