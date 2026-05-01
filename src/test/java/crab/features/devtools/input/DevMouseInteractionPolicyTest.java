package crab.features.devtools.input;

import crab.features.devtools.domain.DevToolMode;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.Test;

import static crab.features.devtools.input.DevMousePressAction.CAMERA_LOOK;
import static crab.features.devtools.input.DevMousePressAction.NONE;
import static crab.features.devtools.input.DevMousePressAction.OBJECT_DRAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevMouseInteractionPolicyTest {
    private final DevMouseInteractionPolicy policy = new DevMouseInteractionPolicy();

    @Test
    void flyCameraSecondaryAndMiddlePressLookWithoutBlockingPrimarySelection() {
        assertEquals(NONE, policy.pressAction(DevToolMode.FLY_CAMERA, true, MouseButton.PRIMARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.FLY_CAMERA, true, MouseButton.SECONDARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.FLY_CAMERA, true, MouseButton.MIDDLE));
    }

    @Test
    void primaryPressOnEmptySceneNavigatesByDefaultInTransformModes() {
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.MOVE, false, MouseButton.PRIMARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.ROTATE, false, MouseButton.PRIMARY));
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.SCALE, false, MouseButton.PRIMARY));
    }

    @Test
    void secondaryAndMiddlePressNavigateInEveryToolMode() {
        for (DevToolMode mode : DevToolMode.values()) {
            assertEquals(CAMERA_LOOK, policy.pressAction(mode, true, MouseButton.SECONDARY));
            assertEquals(CAMERA_LOOK, policy.pressAction(mode, true, MouseButton.MIDDLE));
        }
    }

    @Test
    void moveModePrimaryPressOnObjectStartsObjectDragEvenInDevCameraView() {
        assertEquals(OBJECT_DRAG, policy.pressAction(DevToolMode.MOVE, true, MouseButton.PRIMARY));
    }

    @Test
    void selectInspectRotateAndScaleDoNotStartPlaneDrag() {
        assertEquals(NONE, policy.pressAction(DevToolMode.SELECT, true, MouseButton.PRIMARY));
        assertEquals(NONE, policy.pressAction(DevToolMode.INSPECT, true, MouseButton.PRIMARY));
    }

    @Test
    void rotateAndScalePrimaryPressOnObjectStartsObjectDrag() {
        assertEquals(OBJECT_DRAG, policy.pressAction(DevToolMode.ROTATE, true, MouseButton.PRIMARY));
        assertEquals(OBJECT_DRAG, policy.pressAction(DevToolMode.SCALE, true, MouseButton.PRIMARY));
    }

    @Test
    void moveModeSecondaryPressNavigatesInsteadOfDraggingObject() {
        assertEquals(CAMERA_LOOK, policy.pressAction(DevToolMode.MOVE, true, MouseButton.SECONDARY));
    }
}
