package crab.features.devtools.interaction;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.shape.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransformGizmo3DTest {
    @Test
    void placesGizmoAtSelectedObjectOriginInSceneRootCoordinates() {
        Group sceneRoot = new Group();
        Group parent = new Group();
        Box target = new Box(20, 30, 40);
        parent.setTranslateX(25);
        parent.setTranslateY(-10);
        parent.setTranslateZ(5);
        target.setTranslateX(12);
        target.setTranslateY(4);
        target.setTranslateZ(-8);
        parent.getChildren().add(target);
        sceneRoot.getChildren().add(parent);

        TransformGizmo3D gizmo = new TransformGizmo3D();
        gizmo.attachTo(sceneRoot);
        gizmo.showFor(target, sceneRoot);

        Point3D origin = gizmo.currentOrigin();
        assertEquals(37, origin.getX(), 0.0001);
        assertEquals(-6, origin.getY(), 0.0001);
        assertEquals(-3, origin.getZ(), 0.0001);
        assertTrue(gizmo.node().isVisible());
    }

    @Test
    void resolvesAxisHandleFromPickedDescendant() {
        TransformGizmo3D gizmo = new TransformGizmo3D();

        assertEquals(GizmoAxis.X, gizmo.axisFor(gizmo.axisHandleFor(GizmoAxis.X)).orElseThrow());
        assertEquals(GizmoAxis.Y, gizmo.axisFor(gizmo.axisHandleFor(GizmoAxis.Y)).orElseThrow());
        assertEquals(GizmoAxis.Z, gizmo.axisFor(gizmo.axisHandleFor(GizmoAxis.Z)).orElseThrow());
    }

    @Test
    void appliesAxisDeltasToMatchingTranslateComponent() {
        Group node = new Group();

        GizmoAxis.X.applyDelta(node, 3);
        GizmoAxis.Y.applyDelta(node, -4);
        GizmoAxis.Z.applyDelta(node, 5);

        assertEquals(3, node.getTranslateX(), 0.0001);
        assertEquals(-4, node.getTranslateY(), 0.0001);
        assertEquals(5, node.getTranslateZ(), 0.0001);
    }

    @Test
    void marksOwnNodesSoSceneTreeCanSkipTooling() {
        TransformGizmo3D gizmo = new TransformGizmo3D();

        assertTrue(TransformGizmo3D.isGizmoNode(gizmo.node()));
        assertTrue(TransformGizmo3D.isGizmoNode(gizmo.axisHandleFor(GizmoAxis.X)));
        assertFalse(TransformGizmo3D.isGizmoNode(new Group()));
    }

    @Test
    void attachesOnlyOnceToSceneRoot() {
        Group sceneRoot = new Group();
        TransformGizmo3D gizmo = new TransformGizmo3D();

        gizmo.attachTo(sceneRoot);
        gizmo.attachTo(sceneRoot);

        assertEquals(1, sceneRoot.getChildren().size());
        assertSame(gizmo.node(), sceneRoot.getChildren().getFirst());
    }
}
