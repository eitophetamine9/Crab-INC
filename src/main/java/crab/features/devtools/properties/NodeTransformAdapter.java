package crab.features.devtools.properties;

import javafx.scene.Node;
import javafx.scene.transform.Rotate;

public final class NodeTransformAdapter {
    private static final String ROTATE_X_KEY = "crab.devtools.rotateX";
    private static final String ROTATE_Y_KEY = "crab.devtools.rotateY";
    private static final String ROTATE_Z_KEY = "crab.devtools.rotateZ";

    private final Node node;
    private final Rotate rotateX;
    private final Rotate rotateY;
    private final Rotate rotateZ;

    public NodeTransformAdapter(Node node) {
        this.node = node;
        rotateX = getOrCreateRotate(ROTATE_X_KEY, Rotate.X_AXIS);
        rotateY = getOrCreateRotate(ROTATE_Y_KEY, Rotate.Y_AXIS);
        rotateZ = getOrCreateRotate(ROTATE_Z_KEY, Rotate.Z_AXIS);
    }

    public void setPosition(double x, double y, double z) {
        node.setTranslateX(x);
        node.setTranslateY(y);
        node.setTranslateZ(z);
    }

    public void setRotation(double x, double y, double z) {
        rotateX.setAngle(x);
        rotateY.setAngle(y);
        rotateZ.setAngle(z);
    }

    public void setScale(double x, double y, double z) {
        node.setScaleX(x);
        node.setScaleY(y);
        node.setScaleZ(z);
    }

    public TransformSnapshot snapshot() {
        return new TransformSnapshot(
                node.getTranslateX(),
                node.getTranslateY(),
                node.getTranslateZ(),
                rotateX.getAngle(),
                rotateY.getAngle(),
                rotateZ.getAngle(),
                node.getScaleX(),
                node.getScaleY(),
                node.getScaleZ()
        );
    }

    public record TransformSnapshot(
            double x,
            double y,
            double z,
            double rotateX,
            double rotateY,
            double rotateZ,
            double scaleX,
            double scaleY,
            double scaleZ
    ) {
    }

    private Rotate getOrCreateRotate(String key, javafx.geometry.Point3D axis) {
        Object existing = node.getProperties().get(key);
        if (existing instanceof Rotate rotate) {
            return rotate;
        }

        Rotate rotate = new Rotate(0, axis);
        node.getProperties().put(key, rotate);
        node.getTransforms().add(rotate);
        return rotate;
    }
}
