package crab.features.devtools.input;

import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.Test;

import static crab.features.devtools.input.DevMousePressAction.CAMERA_LOOK;
import static crab.features.devtools.input.DevMousePressAction.NONE;
import static crab.features.devtools.input.DevMousePressAction.OBJECT_DRAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevMouseInteractionPolicyTest {
    private final DevMouseInteractionPolicy policy = new DevMouseInteractionPolicy();

    @Test
    void devCameraHoldDragLooksInsteadOfDraggingObject() {
        assertEquals(CAMERA_LOOK, policy.pressAction(true, true, MouseButton.PRIMARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(true, true, MouseButton.SECONDARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(true, true, MouseButton.MIDDLE));
    }

    @Test
    void sceneCameraPrimaryPressOnObjectStartsObjectDrag() {
        assertEquals(OBJECT_DRAG, policy.pressAction(false, true, MouseButton.PRIMARY));
    }

    @Test
    void sceneCameraSecondaryPressDoesNotDragObject() {
        assertEquals(NONE, policy.pressAction(false, true, MouseButton.SECONDARY));
    }
}
