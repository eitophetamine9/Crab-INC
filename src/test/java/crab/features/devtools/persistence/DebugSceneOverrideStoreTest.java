package crab.features.devtools.persistence;

import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.DebugParameterGroup;
import crab.features.devtools.domain.Inspectable3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class DebugSceneOverrideStoreTest {
    @TempDir
    private Path tempDir;

    @Test
    void savesAndLoadsPersistedObjectOverridesByStableId() {
        AtomicReference<Double> waveSpeed = new AtomicReference<>(1.0);
        Box box = new Box();
        PhongMaterial material = new PhongMaterial(Color.RED);
        box.setMaterial(material);
        box.setTranslateX(12);
        box.setTranslateY(24);
        box.setTranslateZ(36);
        box.setVisible(false);
        Inspectable3D item = Inspectable3D.forNodeWithGroups(
                "shore.plane",
                "Shore Plane",
                "demo",
                box,
                List.of(new DebugParameterGroup("Shoreline", List.of(DebugParameter.number(
                        "shore.waveSpeed",
                        "Wave Speed",
                        "Shoreline",
                        0.1,
                        2.5,
                        0.1,
                        waveSpeed::get,
                        waveSpeed::set
                ))))
        );
        waveSpeed.set(2.0);

        DebugSceneOverrideStore store = new DebugSceneOverrideStore(tempDir);
        store.saveInspectable(item);

        Box restoredBox = new Box();
        AtomicReference<Double> restoredWaveSpeed = new AtomicReference<>(1.0);
        Inspectable3D restored = Inspectable3D.forNodeWithGroups(
                "shore.plane",
                "Shore Plane",
                "demo",
                restoredBox,
                List.of(new DebugParameterGroup("Shoreline", List.of(DebugParameter.number(
                        "shore.waveSpeed",
                        "Wave Speed",
                        "Shoreline",
                        0.1,
                        2.5,
                        0.1,
                        restoredWaveSpeed::get,
                        restoredWaveSpeed::set
                ))))
        );
        store.applyOverrides(restored);

        assertEquals(12, restoredBox.getTranslateX());
        assertEquals(24, restoredBox.getTranslateY());
        assertEquals(36, restoredBox.getTranslateZ());
        assertFalse(restoredBox.isVisible());
        assertEquals(2.0, restoredWaveSpeed.get());
    }

    @Test
    void ignoresUnknownObjectIdsWhenApplyingOverrides() {
        DebugSceneOverrideStore store = new DebugSceneOverrideStore(tempDir);
        Inspectable3D other = Inspectable3D.forNode("missing", "Missing", "demo", new Group());

        store.applyOverrides(other);

        assertEquals(0, other.target().getTranslateX());
    }
}
