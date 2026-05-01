package crab.features.devtools.interaction;

import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Node;

public final class ViewRelativeTransform {
    private static final double MIN_SCALE = 0.01;

    private ViewRelativeTransform() {
    }

    public static void move(Node node, Camera camera, double deltaX, double deltaY, double sensitivity) {
        Point3D delta = cameraRight(camera)
                .multiply(deltaX * sensitivity)
                .add(cameraUp(camera).multiply(deltaY * sensitivity));
        node.setTranslateX(node.getTranslateX() + delta.getX());
        node.setTranslateY(node.getTranslateY() + delta.getY());
        node.setTranslateZ(node.getTranslateZ() + delta.getZ());
    }

    public static void scaleUniform(Node node, double deltaX, double deltaY, double sensitivity) {
        double amount = (deltaX - deltaY) * sensitivity;
        node.setScaleX(Math.max(MIN_SCALE, node.getScaleX() + amount));
        node.setScaleY(Math.max(MIN_SCALE, node.getScaleY() + amount));
        node.setScaleZ(Math.max(MIN_SCALE, node.getScaleZ() + amount));
    }

    public static Point3D cameraRight(Camera camera) {
        if (camera == null) {
            return new Point3D(1, 0, 0);
        }
        return normalizeOr(camera.localToSceneTransformProperty().get().deltaTransform(1, 0, 0), new Point3D(1, 0, 0));
    }

    public static Point3D cameraUp(Camera camera) {
        if (camera == null) {
            return new Point3D(0, 1, 0);
        }
        return normalizeOr(camera.localToSceneTransformProperty().get().deltaTransform(0, 1, 0), new Point3D(0, 1, 0));
    }

    private static Point3D normalizeOr(Point3D vector, Point3D fallback) {
        if (!Double.isFinite(vector.getX()) || !Double.isFinite(vector.getY()) || !Double.isFinite(vector.getZ())) {
            return fallback;
        }
        if (vector.magnitude() < 0.0001) {
            return fallback;
        }
        return vector.normalize();
    }
}
