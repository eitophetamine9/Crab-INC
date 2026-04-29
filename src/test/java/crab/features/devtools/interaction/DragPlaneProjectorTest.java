package crab.features.devtools.interaction;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DragPlaneProjectorTest {
    @Test
    void intersectsRayWithHorizontalPlane() {
        Point3D hit = DragPlaneProjector.intersectPlaneY(
                new Point3D(0, 10, 0),
                new Point3D(2, -2, 4),
                0
        ).orElseThrow();

        assertEquals(10, hit.getX());
        assertEquals(0, hit.getY());
        assertEquals(20, hit.getZ());
    }

    @Test
    void appliesPlaneDeltaToNodeXzOnly() {
        Group node = new Group();
        node.setTranslateX(5);
        node.setTranslateY(10);
        node.setTranslateZ(15);

        DragPlaneProjector.applyPlaneDelta(node, new Point3D(2, 0, -4));

        assertEquals(7, node.getTranslateX());
        assertEquals(10, node.getTranslateY());
        assertEquals(11, node.getTranslateZ());
    }
}
