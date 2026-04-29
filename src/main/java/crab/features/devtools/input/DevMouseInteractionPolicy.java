package crab.features.devtools.input;

import javafx.scene.input.MouseButton;

public final class DevMouseInteractionPolicy {
    public DevMousePressAction pressAction(boolean devCameraActive, boolean objectPicked, MouseButton button) {
        if (devCameraActive && isLookButton(button)) {
            return DevMousePressAction.CAMERA_LOOK;
        }

        if (objectPicked && button == MouseButton.PRIMARY) {
            return DevMousePressAction.OBJECT_DRAG;
        }

        return DevMousePressAction.NONE;
    }

    private static boolean isLookButton(MouseButton button) {
        return button == MouseButton.PRIMARY
                || button == MouseButton.SECONDARY
                || button == MouseButton.MIDDLE;
    }
}
