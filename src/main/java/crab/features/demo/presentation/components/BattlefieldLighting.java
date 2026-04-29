package crab.features.demo.presentation.components;

import javafx.scene.AmbientLight;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Lighting helpers for the shader-textured battlefield plane.
 */
public final class BattlefieldLighting {
    private BattlefieldLighting() {
    }

    public static AmbientLight createAmbientFor(Node target, double intensity) {
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.getScope().add(target);
        setAmbientIntensity(ambientLight, intensity);
        return ambientLight;
    }

    public static void setAmbientIntensity(AmbientLight ambientLight, double intensity) {
        double clampedIntensity = Math.clamp(intensity, 0.0, 1.0);
        ambientLight.setColor(Color.gray(clampedIntensity));
    }
}
