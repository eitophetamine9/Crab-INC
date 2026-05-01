package crab.features.devtools.interaction;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Rotate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ViewRelativeTransformTest {
    @Test
    void movesNodeAlongCameraRightAndUpVectors() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
        Group node = new Group();

        ViewRelativeTransform.move(node, camera, 10, -4, 1);

        assertEquals(0, node.getTranslateX(), 0.0001);
        assertEquals(-4, node.getTranslateY(), 0.0001);
        assertEquals(-10, node.getTranslateZ(), 0.0001);
    }

    @Test
    void scalesUniformlyFromCombinedDragDistance() {
        Group node = new Group();
        node.setScaleX(1);
        node.setScaleY(2);
        node.setScaleZ(3);

        ViewRelativeTransform.scaleUniform(node, 20, -10, 0.01);

        assertEquals(1.3, node.getScaleX(), 0.0001);
        assertEquals(2.3, node.getScaleY(), 0.0001);
        assertEquals(3.3, node.getScaleZ(), 0.0001);
    }

    @Test
    void clampsUniformScaleToPositiveValue() {
        Group node = new Group();

        ViewRelativeTransform.scaleUniform(node, -500, 0, 0.01);

        assertTrue(node.getScaleX() > 0);
        assertTrue(node.getScaleY() > 0);
        assertTrue(node.getScaleZ() > 0);
    }

    @Test
    void exposesCameraRightAndUpVectors() {
        PerspectiveCamera camera = new PerspectiveCamera(true);

        assertEquals(new Point3D(1, 0, 0), ViewRelativeTransform.cameraRight(camera));
        assertEquals(new Point3D(0, 1, 0), ViewRelativeTransform.cameraUp(camera));
    }
}
