package crab.features.devtools.interaction;

import javafx.geometry.Point3D;
import javafx.scene.Node;

import java.util.Optional;

public final class DragPlaneProjector {
    private static final double EPSILON = 0.00001;

    private DragPlaneProjector() {
    }

    public static Optional<Point3D> intersectPlaneY(Point3D origin, Point3D direction, double planeY) {
        if (Math.abs(direction.getY()) < EPSILON) {
            return Optional.empty();
        }

        double t = (planeY - origin.getY()) / direction.getY();
        if (t < 0) {
            return Optional.empty();
        }

        return Optional.of(origin.add(direction.multiply(t)));
    }

    public static void applyPlaneDelta(Node node, Point3D delta) {
        node.setTranslateX(node.getTranslateX() + delta.getX());
        node.setTranslateZ(node.getTranslateZ() + delta.getZ());
    }
}
