package crab.features.devtools.camera;

import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.DebugParameterGroup;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;

import java.util.ArrayList;
import java.util.List;

public final class CameraDebugParameters {
    private static final double MIN_NEAR_CLIP = 0.01;
    private static final double MAX_NEAR_CLIP = 100;
    private static final double MIN_FAR_CLIP = 100;
    private static final double MAX_FAR_CLIP = 20_000;

    private CameraDebugParameters() {
    }

    public static List<DebugParameterGroup> groupsFor(String idPrefix, Camera camera) {
        List<DebugParameter> parameters = new ArrayList<>();
        parameters.add(DebugParameter.number(
                idPrefix + ".x", "Camera X", "Camera", -5000, 5000, 1, camera::getTranslateX, camera::setTranslateX));
        parameters.add(DebugParameter.number(
                idPrefix + ".y", "Camera Y", "Camera", -5000, 5000, 1, camera::getTranslateY, camera::setTranslateY));
        parameters.add(DebugParameter.number(
                idPrefix + ".z", "Camera Z", "Camera", -5000, 5000, 1, camera::getTranslateZ, camera::setTranslateZ));
        addPerspectiveParameters(idPrefix, camera, parameters);
        return List.of(new DebugParameterGroup("Camera", parameters));
    }

    public static List<DebugParameter> perspectiveParameters(String idPrefix, PerspectiveCamera camera) {
        List<DebugParameter> parameters = new ArrayList<>();
        addPerspectiveParameters(idPrefix, camera, parameters);
        return parameters;
    }

    private static void addPerspectiveParameters(String idPrefix, Camera camera, List<DebugParameter> parameters) {
        if (!(camera instanceof PerspectiveCamera perspectiveCamera)) {
            return;
        }

        parameters.add(DebugParameter.number(
                idPrefix + ".fov", "Field of View", "Camera", 1, 120, 0.25,
                perspectiveCamera::getFieldOfView,
                perspectiveCamera::setFieldOfView
        ));
        parameters.add(DebugParameter.number(
                idPrefix + ".nearClip", "Near Clip", "Camera", MIN_NEAR_CLIP, MAX_NEAR_CLIP, 0.01,
                perspectiveCamera::getNearClip,
                perspectiveCamera::setNearClip
        ));
        parameters.add(DebugParameter.number(
                idPrefix + ".farClip", "Far Clip", "Camera", MIN_FAR_CLIP, MAX_FAR_CLIP, 10,
                perspectiveCamera::getFarClip,
                perspectiveCamera::setFarClip
        ));
    }
}
