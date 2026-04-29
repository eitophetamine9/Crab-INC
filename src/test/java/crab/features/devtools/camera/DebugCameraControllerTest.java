package crab.features.devtools.camera;

import javafx.application.Platform;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.Group;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DebugCameraControllerTest {
    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean();

    @BeforeAll
    static void startToolkit() throws Exception {
        if (!TOOLKIT_STARTED.compareAndSet(false, true)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
    }

    @Test
    void switchesToDevCameraAndRestoresOriginalCamera() {
        PerspectiveCamera original = new PerspectiveCamera(true);
        SubScene subScene = new SubScene(new Group(), 320, 240, true, null);
        subScene.setCamera(original);

        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();

        assertTrue(controller.isDevCameraActive());

        controller.restoreOriginalCamera();

        assertSame(original, subScene.getCamera());
    }

    @Test
    void movesDevCameraWithSpeedMultiplier() {
        SubScene subScene = new SubScene(new Group(), 320, 240, true, null);
        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();

        controller.moveForward(1.0, true);

        assertTrue(subScene.getCamera().getTranslateZ() > -720);
    }

    @Test
    void updatesMovementContinuouslyUsingTpf() {
        SubScene subScene = new SubScene(new Group(), 320, 240, true, null);
        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();

        controller.move(1.0, 0.0, 0.0, false, 0.5);

        assertEquals(-620.0, subScene.getCamera().getTranslateZ(), 0.0001);
    }

    @Test
    void movesForwardRelativeToYaw() {
        SubScene subScene = new SubScene(new Group(), 320, 240, true, null);
        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();
        controller.look(450, 0);

        controller.move(1.0, 0.0, 0.0, false, 0.5);

        assertTrue(subScene.getCamera().getTranslateX() > 170);
        assertEquals(-800.0, subScene.getCamera().getTranslateZ(), 0.0001);
    }

    @Test
    void movesForwardAlongCurrentPitch() {
        Group root = new Group(new Box(100, 100, 100));
        PerspectiveCamera original = new PerspectiveCamera(true);
        original.setTranslateX(0);
        original.setTranslateY(-500);
        original.setTranslateZ(0);
        SubScene subScene = new SubScene(root, 320, 240, true, null);
        subScene.setCamera(original);

        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();

        controller.move(1.0, 0.0, 0.0, false, 0.5);

        assertTrue(subScene.getCamera().getTranslateY() > -330);
        assertEquals(0.0, subScene.getCamera().getTranslateZ(), 4.0);
    }

    @Test
    void pointsDevCameraAtSceneRootWhenActivated() {
        Group root = new Group(new Box(100, 100, 100));
        PerspectiveCamera original = new PerspectiveCamera(true);
        original.setTranslateX(0);
        original.setTranslateY(-500);
        original.setTranslateZ(0);
        SubScene subScene = new SubScene(root, 320, 240, true, null);
        subScene.setCamera(original);

        DebugCameraController controller = new DebugCameraController(subScene);
        controller.activateDevCamera();

        Camera camera = subScene.getCamera();
        Rotate yaw = (Rotate) camera.getTransforms().get(0);
        Rotate pitch = (Rotate) camera.getTransforms().get(1);

        assertEquals(0.0, yaw.getAngle(), 0.1);
        assertTrue(pitch.getAngle() < -80.0);
    }

    @Test
    void exposesDevCameraYawAndPitchForPersistence() {
        SubScene subScene = new SubScene(new Group(), 320, 240, true, null);
        DebugCameraController controller = new DebugCameraController(subScene);

        controller.setYawAngle(42);
        controller.setPitchAngle(-24);

        assertEquals(42, controller.yawAngle(), 0.0001);
        assertEquals(-24, controller.pitchAngle(), 0.0001);
    }
}
