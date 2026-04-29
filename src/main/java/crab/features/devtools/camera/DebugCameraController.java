package crab.features.devtools.camera;

import javafx.scene.Camera;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Rotate;

public final class DebugCameraController {
    private static final double BASE_MOVE_STEP = 24.0;
    private static final double BASE_MOVE_SPEED = 360.0;
    private static final double BOOST_MULTIPLIER = 4.0;

    private final SubScene subScene;
    private final Camera originalCamera;
    private final PerspectiveCamera devCamera = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
    private boolean devCameraActive;

    public DebugCameraController(SubScene subScene) {
        this.subScene = subScene;
        this.originalCamera = subScene.getCamera();
        devCamera.setNearClip(0.1);
        devCamera.setFarClip(5000);
        devCamera.setFieldOfView(55);
        devCamera.getTransforms().setAll(yaw, pitch);
        copyOriginalCamera();
        pointAtSceneRoot();
    }

    public PerspectiveCamera devCamera() {
        return devCamera;
    }

    public Camera originalCamera() {
        return originalCamera;
    }

    public boolean isDevCameraActive() {
        return devCameraActive;
    }

    public void activateDevCamera() {
        subScene.setCamera(devCamera);
        devCameraActive = true;
    }

    public void restoreOriginalCamera() {
        subScene.setCamera(originalCamera);
        devCameraActive = false;
    }

    public void moveForward(double direction, boolean boosted) {
        double step = movementStep(boosted) * direction;
        move(direction, 0, 0, boosted, step / movementSpeed(boosted));
    }

    public void moveRight(double direction, boolean boosted) {
        double step = movementStep(boosted) * direction;
        devCamera.setTranslateX(devCamera.getTranslateX() + step);
    }

    public void moveUp(double direction, boolean boosted) {
        double step = movementStep(boosted) * direction;
        devCamera.setTranslateY(devCamera.getTranslateY() - step);
    }

    public void move(double forward, double right, double up, boolean boosted, double tpf) {
        double length = Math.hypot(forward, right);
        if (length > 1.0) {
            forward /= length;
            right /= length;
        }

        double speed = movementSpeed(boosted) * tpf;
        double yawRadians = Math.toRadians(yaw.getAngle());
        double pitchRadians = Math.toRadians(pitch.getAngle());
        double cosPitch = Math.cos(pitchRadians);
        double forwardX = Math.sin(yawRadians) * cosPitch;
        double forwardY = -Math.sin(pitchRadians);
        double forwardZ = Math.cos(yawRadians) * cosPitch;
        double rightX = Math.cos(yawRadians);
        double rightZ = -Math.sin(yawRadians);

        devCamera.setTranslateX(devCamera.getTranslateX() + ((forwardX * forward) + (rightX * right)) * speed);
        devCamera.setTranslateZ(devCamera.getTranslateZ() + ((forwardZ * forward) + (rightZ * right)) * speed);
        devCamera.setTranslateY(devCamera.getTranslateY() + (forwardY * forward * speed) - (up * speed));
    }

    public void look(double deltaX, double deltaY) {
        yaw.setAngle(yaw.getAngle() + deltaX * 0.2);
        pitch.setAngle(Math.clamp(pitch.getAngle() - deltaY * 0.2, -89, 89));
    }

    public double yawAngle() {
        return yaw.getAngle();
    }

    public void setYawAngle(double angle) {
        yaw.setAngle(angle);
    }

    public double pitchAngle() {
        return pitch.getAngle();
    }

    public void setPitchAngle(double angle) {
        pitch.setAngle(Math.clamp(angle, -89, 89));
    }

    private void copyOriginalCamera() {
        if (originalCamera == null) {
            devCamera.setTranslateZ(-800);
            return;
        }

        devCamera.setTranslateX(originalCamera.getTranslateX());
        devCamera.setTranslateY(originalCamera.getTranslateY());
        devCamera.setTranslateZ(originalCamera.getTranslateZ());
        if (originalCamera instanceof PerspectiveCamera perspectiveCamera) {
            devCamera.setFieldOfView(perspectiveCamera.getFieldOfView());
            devCamera.setNearClip(perspectiveCamera.getNearClip());
            devCamera.setFarClip(perspectiveCamera.getFarClip());
        }
    }

    private void pointAtSceneRoot() {
        if (subScene.getRoot() == null) {
            return;
        }

        Bounds bounds = subScene.getRoot().getBoundsInLocal();
        if (bounds.isEmpty()) {
            return;
        }

        Point3D target = new Point3D(
                (bounds.getMinX() + bounds.getMaxX()) / 2.0,
                (bounds.getMinY() + bounds.getMaxY()) / 2.0,
                (bounds.getMinZ() + bounds.getMaxZ()) / 2.0
        );
        pointAt(target);
    }

    private void pointAt(Point3D target) {
        double deltaX = target.getX() - devCamera.getTranslateX();
        double deltaY = target.getY() - devCamera.getTranslateY();
        double deltaZ = target.getZ() - devCamera.getTranslateZ();
        double horizontalDistance = Math.hypot(deltaX, deltaZ);

        if (horizontalDistance < 0.0001 && Math.abs(deltaY) < 0.0001) {
            return;
        }

        yaw.setAngle(Math.toDegrees(Math.atan2(deltaX, deltaZ)));
        pitch.setAngle(Math.clamp(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)), -89, 89));
    }

    private static double movementStep(boolean boosted) {
        return boosted ? BASE_MOVE_STEP * BOOST_MULTIPLIER : BASE_MOVE_STEP;
    }

    private static double movementSpeed(boolean boosted) {
        return boosted ? BASE_MOVE_SPEED * BOOST_MULTIPLIER : BASE_MOVE_SPEED;
    }
}
