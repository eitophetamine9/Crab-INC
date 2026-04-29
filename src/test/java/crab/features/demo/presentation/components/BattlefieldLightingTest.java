package crab.features.demo.presentation.components;

import javafx.scene.AmbientLight;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class BattlefieldLightingTest {
    @Test
    void createsAmbientLightScopedToBattlefieldPlane() {
        MeshView plane = new MeshView();

        AmbientLight light = BattlefieldLighting.createAmbientFor(plane, 0.72);

        assertSame(plane, light.getScope().getFirst());
        assertEquals(Color.gray(0.72), light.getColor());
    }

    @Test
    void clampsAmbientIntensityToColorRange() {
        MeshView plane = new MeshView();
        AmbientLight light = BattlefieldLighting.createAmbientFor(plane, 0.0);

        BattlefieldLighting.setAmbientIntensity(light, -1.0);
        assertEquals(Color.BLACK, light.getColor());

        BattlefieldLighting.setAmbientIntensity(light, 2.0);
        assertEquals(Color.WHITE, light.getColor());
    }
}
