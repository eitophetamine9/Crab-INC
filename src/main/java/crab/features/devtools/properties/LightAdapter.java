package crab.features.devtools.properties;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.LightBase;

import java.util.Optional;

public final class LightAdapter {
    private final LightBase light;

    private LightAdapter(LightBase light) {
        this.light = light;
    }

    public static Optional<LightAdapter> forNode(Node node) {
        if (node instanceof LightBase light) {
            return Optional.of(new LightAdapter(light));
        }

        return Optional.empty();
    }

    public Color color() {
        return light.getColor();
    }

    public void setColor(Color color) {
        light.setColor(color);
    }

    public void setAmbientIntensity(double intensity) {
        light.setColor(Color.gray(Math.clamp(intensity, 0.0, 1.0)));
    }
}
