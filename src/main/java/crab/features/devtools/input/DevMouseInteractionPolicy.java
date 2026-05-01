package crab.features.devtools.input;

import crab.features.devtools.domain.DevToolMode;
import javafx.scene.input.MouseButton;

public final class DevMouseInteractionPolicy {
    public DevMousePressAction pressAction(DevToolMode mode, boolean objectPicked, MouseButton button) {
        if (isLookButton(button) || (!objectPicked && button == MouseButton.PRIMARY)) {
            return DevMousePressAction.CAMERA_LOOK;
        }

        if (isTransformMode(mode) && objectPicked && button == MouseButton.PRIMARY) {
            return DevMousePressAction.OBJECT_DRAG;
        }

        return DevMousePressAction.NONE;
    }

    private static boolean isLookButton(MouseButton button) {
        return button == MouseButton.SECONDARY
                || button == MouseButton.MIDDLE;
    }

    private static boolean isTransformMode(DevToolMode mode) {
        return mode == DevToolMode.MOVE || mode == DevToolMode.ROTATE || mode == DevToolMode.SCALE;
    }
}
