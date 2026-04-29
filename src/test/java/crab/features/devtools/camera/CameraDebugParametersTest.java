package crab.features.devtools.camera;

import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.persistence.DebugSceneOverrideStore;
import javafx.scene.PerspectiveCamera;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CameraDebugParametersTest {
    @TempDir
    private Path tempDir;

    @Test
    void exposesPerspectiveCameraFieldOfViewAndClipSpace() {
        PerspectiveCamera camera = new PerspectiveCamera(true);

        Map<String, DebugParameter> parameters = CameraDebugParameters.groupsFor("scene.camera", camera)
                .getFirst()
                .parameters()
                .stream()
                .collect(Collectors.toMap(DebugParameter::id, parameter -> parameter));

        parameters.get("scene.camera.fov").setValue(72);
        parameters.get("scene.camera.nearClip").setValue(0.25);
        parameters.get("scene.camera.farClip").setValue(12000);

        assertEquals(72, camera.getFieldOfView(), 0.0001);
        assertEquals(0.25, camera.getNearClip(), 0.0001);
        assertEquals(12000, camera.getFarClip(), 0.0001);
    }

    @Test
    void persistsPerspectiveCameraFieldOfViewAndClipSpaceAsDebugParameters() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        Inspectable3D item = Inspectable3D.forNodeWithGroups(
                "demo.camera.scene",
                "Scene Camera",
                "demo",
                camera,
                CameraDebugParameters.groupsFor("demo.camera.scene", camera)
        );
        parametersById(item).get("demo.camera.scene.fov").setValue(64);
        parametersById(item).get("demo.camera.scene.nearClip").setValue(0.5);
        parametersById(item).get("demo.camera.scene.farClip").setValue(9000);

        DebugSceneOverrideStore store = new DebugSceneOverrideStore(tempDir);
        store.saveInspectable(item);

        PerspectiveCamera restoredCamera = new PerspectiveCamera(true);
        Inspectable3D restored = Inspectable3D.forNodeWithGroups(
                "demo.camera.scene",
                "Scene Camera",
                "demo",
                restoredCamera,
                CameraDebugParameters.groupsFor("demo.camera.scene", restoredCamera)
        );
        store.applyOverrides(restored);

        assertEquals(64, restoredCamera.getFieldOfView(), 0.0001);
        assertEquals(0.5, restoredCamera.getNearClip(), 0.0001);
        assertEquals(9000, restoredCamera.getFarClip(), 0.0001);
    }

    private static Map<String, DebugParameter> parametersById(Inspectable3D item) {
        return item.debugParameters()
                .stream()
                .collect(Collectors.toMap(DebugParameter::id, parameter -> parameter));
    }
}
