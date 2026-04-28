package crab.features.demo.presentation.components;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Controls the stylized Phong material used by the 3D demo.
 *
 * Design patterns:
 * - Strategy: encapsulates one material style behind a small update method.
 *
 * SOLID:
 * - Single Responsibility: maps UI parameters to JavaFX material properties.
 */
public final class StylizedPhongMaterialController {
    private final PhongMaterial material = new PhongMaterial();

    public StylizedPhongMaterialController() {
        update(0.65);
    }

    public PhongMaterial material() {
        return material;
    }

    public void update(double stylizeAmount) {
        double clamped = Math.max(0, Math.min(1, stylizeAmount));
        double hue = 182 - clamped * 18;
        double saturation = 0.42 + clamped * 0.42;
        double brightness = 0.58 + clamped * 0.24;

        material.setDiffuseColor(Color.hsb(hue, saturation, quantize(brightness, clamped)));
        material.setSpecularColor(Color.hsb(198, 0.18, 0.62 + clamped * 0.34));
        material.setSpecularPower(18 + clamped * 86);
    }

    private static double quantize(double value, double amount) {
        double bands = 3 + Math.round(amount * 5);
        return Math.round(value * bands) / bands;
    }
}
