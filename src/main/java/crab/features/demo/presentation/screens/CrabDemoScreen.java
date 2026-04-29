package crab.features.demo.presentation.screens;

import com.almasb.fxgl.scene3d.Model3D;
import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.devtools.DevToolsModule;
import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.demo.DemoDirectionalControls;
import crab.features.demo.presentation.components.BattlefieldLighting;
import crab.features.demo.presentation.components.CrabCameraControlPanelController;
import crab.features.demo.presentation.components.DemoNavigatorController;
import crab.features.demo.presentation.components.ShadertoyBackgroundView;
import crab.features.demo.presentation.components.ShaderTexturedPlane;
import crab.features.menu.presentation.screens.MainMenuScreen;
import javafx.fxml.FXMLLoader;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static com.almasb.fxgl.dsl.FXGL.getAssetLoader;
import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Demo screen for viewing the imported Cangrejo OBJ on a flat battlefield plane.
 */
public final class CrabDemoScreen implements GameScreen, DemoDirectionalControls {
    public static final String ID = "demo_crab";

    private static final double APP_WIDTH = 1024;
    private static final double APP_HEIGHT = 720;
    private static final double NAVIGATOR_WIDTH = 548;
    private static final double CAMERA_STEP = 24;

    private final ScreenManager screens;
    private final DevToolsModule devTools;
    private final Rotate cameraPitch = new Rotate(-90, Rotate.X_AXIS);
    private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
    private ShadertoyBackgroundView battlefieldShader;
    private MeshView battlefieldPlane;
    private AmbientLight battlefieldAmbientLight;
    private AmbientLight ambientLight;
    private PointLight keyLight;
    private PointLight fillLight;
    private Node crabModel;
    private PerspectiveCamera camera;
    private CrabCameraControlPanelController cameraControls;
    private Node sceneRoot;
    private Parent controlPanel;
    private Parent navigator;
    private boolean visible;

    public CrabDemoScreen(ScreenManager screens, DevToolsModule devTools) {
        this.screens = screens;
        this.devTools = devTools;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void show() {
        if (visible) {
            return;
        }

        visible = true;
        getGameScene().setBackgroundColor(Color.rgb(11, 19, 27));
        sceneRoot = createSceneRoot();
        controlPanel = loadControlPanel();
        navigator = loadNavigator();
        getGameScene().addUINode(sceneRoot);
        getGameScene().addUINode(controlPanel);
        getGameScene().addUINode(navigator);
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        visible = false;
        if (sceneRoot != null) {
            getGameScene().removeUINode(sceneRoot);
            sceneRoot = null;
        }

        if (controlPanel != null) {
            getGameScene().removeUINode(controlPanel);
            controlPanel = null;
            cameraControls = null;
        }

        if (navigator != null) {
            getGameScene().removeUINode(navigator);
            navigator = null;
        }

        if (battlefieldShader != null) {
            battlefieldShader.dispose();
            battlefieldShader = null;
        }

        if (devTools != null) {
            devTools.clearScope(ID);
        }
    }

    @Override
    public void update(double tpf) {
        if (visible && battlefieldShader != null) {
            battlefieldShader.update(tpf);
        }
    }

    private Node createSceneRoot() {
        Node plane = createBattlefieldPlane();
        Node crab = createCrabModel();
        Group world = new Group(plane, crab, createLighting());
        world.setDepthTest(DepthTest.ENABLE);

        SubScene subScene = new SubScene(world, APP_WIDTH, APP_HEIGHT, true, null);
        subScene.setFill(Color.rgb(24, 42, 52));
        camera = createCamera();
        subScene.setCamera(camera);
        registerDevTools(subScene);
        return subScene;
    }

    private Node createBattlefieldPlane() {
        battlefieldShader = new ShadertoyBackgroundView(
                1024,
                1024,
                loadShader("/assets/shaders/stylized_shoreline.frag"),
                true
        );
        battlefieldShader.setShoreOffset(0.42);
        battlefieldShader.setWaveHeight(1.12);
        battlefieldShader.setFoamAmount(1.2);

        MeshView plane = ShaderTexturedPlane.create(1200, 960, battlefieldShader.textureImage());
        plane.setTranslateY(100);
        battlefieldPlane = plane;
        return plane;
    }

    private Node createCrabModel() {
        try {
            Model3D crab = getAssetLoader().loadModel3D("characters/Cangrejo.obj");
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.rgb(183, 62, 48));
            material.setSpecularColor(Color.rgb(255, 188, 128, 0.45));
            material.setSpecularPower(52);
            crab.setMaterial(material);
            crab.setScaleX(1450);
            crab.setScaleY(1450);
            crab.setScaleZ(1450);
            crab.setTranslateX(34);
            crab.setTranslateY(12);
            crab.setTranslateZ(0);
            crabModel = crab;
            return crab;
        } catch (RuntimeException exception) {
            Label fallback = new Label("Crab OBJ load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            fallback.setTranslateX(-360);
            fallback.setTranslateY(-80);
            fallback.setTranslateZ(0);
            return fallback;
        }
    }

    private Node createLighting() {
        ambientLight = new AmbientLight(Color.rgb(112, 138, 150, 0.72));
        battlefieldAmbientLight = BattlefieldLighting.createAmbientFor(battlefieldPlane, 0.82);

        keyLight = new PointLight(Color.rgb(255, 232, 196));
        keyLight.setTranslateX(-240);
        keyLight.setTranslateY(-420);
        keyLight.setTranslateZ(-260);

        fillLight = new PointLight(Color.rgb(128, 224, 232, 0.45));
        fillLight.setTranslateX(260);
        fillLight.setTranslateY(-220);
        fillLight.setTranslateZ(260);

        return new Group(ambientLight, battlefieldAmbientLight, keyLight, fillLight);
    }

    private PerspectiveCamera createCamera() {
        PerspectiveCamera perspectiveCamera = new PerspectiveCamera(true);
        perspectiveCamera.setNearClip(0.1);
        perspectiveCamera.setFarClip(2400);
        perspectiveCamera.setFieldOfView(42);
        perspectiveCamera.getTransforms().setAll(cameraYaw, cameraPitch);
        perspectiveCamera.setTranslateX(0);
        perspectiveCamera.setTranslateY(-760);
        perspectiveCamera.setTranslateZ(-20);
        return perspectiveCamera;
    }

    private Parent loadControlPanel() {
        URL resource = getClass().getResource("/fxml/components/crab-camera-control-panel.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/components/crab-camera-control-panel.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            cameraControls = loader.getController();
            cameraControls.setXConsumer(this::setCameraX);
            cameraControls.setYConsumer(this::setCameraY);
            cameraControls.setZConsumer(this::setCameraZ);
            cameraControls.setPitchConsumer(this::setCameraPitch);
            cameraControls.setYawConsumer(this::setCameraYaw);
            cameraControls.setBattlefieldScaleConsumer(this::setBattlefieldScale);
            cameraControls.setBattlefieldAmbientConsumer(this::setBattlefieldAmbient);
            root.setTranslateX(32);
            root.setTranslateY(32);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Camera panel load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    private Parent loadNavigator() {
        URL resource = getClass().getResource("/fxml/components/demo-navigator.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/components/demo-navigator.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            DemoNavigatorController controller = loader.getController();
            controller.setNavigationActions(
                    () -> screens.show(BoxDemoScreen.ID),
                    () -> screens.show(BunnyDemoScreen.ID),
                    () -> screens.show(CrabDemoScreen.ID),
                    () -> screens.show(MainMenuScreen.ID)
            );
            controller.setActiveScreen(ID);
            root.setTranslateX((APP_WIDTH - NAVIGATOR_WIDTH) / 2.0);
            root.setTranslateY(APP_HEIGHT - 72);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Navigator load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    @Override
    public void moveUp() {
        moveCamera(0, -CAMERA_STEP);
    }

    @Override
    public void moveDown() {
        moveCamera(0, CAMERA_STEP);
    }

    @Override
    public void moveLeft() {
        moveCamera(-CAMERA_STEP, 0);
    }

    @Override
    public void moveRight() {
        moveCamera(CAMERA_STEP, 0);
    }

    private void moveCamera(double x, double z) {
        if (!visible || camera == null) {
            return;
        }

        double nextX = camera.getTranslateX() + x;
        double nextZ = camera.getTranslateZ() + z;
        setCameraX(nextX);
        setCameraZ(nextZ);
    }

    private void setCameraX(double value) {
        if (camera == null) {
            return;
        }

        camera.setTranslateX(value);
        if (cameraControls != null) {
            cameraControls.setCameraX(value);
        }
    }

    private void setCameraY(double value) {
        if (camera == null) {
            return;
        }

        camera.setTranslateY(value);
        if (cameraControls != null) {
            cameraControls.setCameraY(value);
        }
    }

    private void setCameraZ(double value) {
        if (camera == null) {
            return;
        }

        camera.setTranslateZ(value);
        if (cameraControls != null) {
            cameraControls.setCameraZ(value);
        }
    }

    private void setCameraPitch(double value) {
        cameraPitch.setAngle(value);
        if (cameraControls != null) {
            cameraControls.setPitch(value);
        }
    }

    private void setCameraYaw(double value) {
        cameraYaw.setAngle(value);
        if (cameraControls != null) {
            cameraControls.setYaw(value);
        }
    }

    private void setBattlefieldScale(double scale) {
        if (battlefieldPlane == null) {
            return;
        }

        battlefieldPlane.setScaleX(scale);
        battlefieldPlane.setScaleZ(scale);
        if (cameraControls != null) {
            cameraControls.setBattlefieldScale(scale);
        }
    }

    private void setBattlefieldAmbient(double intensity) {
        if (battlefieldAmbientLight == null) {
            return;
        }

        BattlefieldLighting.setAmbientIntensity(battlefieldAmbientLight, intensity);
        if (cameraControls != null) {
            cameraControls.setBattlefieldAmbient(intensity);
        }
    }

    private void registerDevTools(SubScene subScene) {
        if (devTools == null) {
            return;
        }

        devTools.attachSubScene(ID, subScene, battlefieldPlane == null ? 100 : battlefieldPlane.getTranslateY());
        devTools.registerInspectable(Inspectable3D.forNode(
                "crab.battlefield",
                "Battlefield Plane",
                ID,
                battlefieldPlane,
                battlefieldDebugParameters()
        ));
        if (crabModel != null) {
            devTools.registerInspectable(Inspectable3D.forNode("crab.model", "Cangrejo Model", ID, crabModel));
        }
        devTools.registerInspectable(Inspectable3D.forNode("crab.camera", "Top Camera", ID, camera, cameraDebugParameters()));
        devTools.registerInspectable(Inspectable3D.forNode("crab.ambient.scene", "Scene Ambient Light", ID, ambientLight));
        devTools.registerInspectable(Inspectable3D.forNode(
                "crab.ambient.battlefield",
                "Battlefield Ambient Light",
                ID,
                battlefieldAmbientLight,
                List.of(DebugParameter.number(
                        "battlefield.ambient",
                        "Intensity",
                        "Battlefield",
                        0,
                        1,
                        0.01,
                        () -> BattlefieldLighting.ambientIntensity(battlefieldAmbientLight),
                        this::setBattlefieldAmbient
                ))
        ));
        devTools.registerInspectable(Inspectable3D.forNode("crab.light.key", "Key Light", ID, keyLight));
        devTools.registerInspectable(Inspectable3D.forNode("crab.light.fill", "Fill Light", ID, fillLight));
    }

    private List<DebugParameter> cameraDebugParameters() {
        return List.of(
                DebugParameter.number("camera.x", "Camera X", "Camera", -420, 420, 1, camera::getTranslateX, this::setCameraX),
                DebugParameter.number("camera.y", "Camera Y", "Camera", -1200, -120, 1, camera::getTranslateY, this::setCameraY),
                DebugParameter.number("camera.z", "Camera Z", "Camera", -420, 420, 1, camera::getTranslateZ, this::setCameraZ),
                DebugParameter.number("camera.pitch", "Pitch", "Camera", -90, -20, 0.25, cameraPitch::getAngle, this::setCameraPitch),
                DebugParameter.number("camera.yaw", "Yaw", "Camera", -180, 180, 0.25, cameraYaw::getAngle, this::setCameraYaw)
        );
    }

    private List<DebugParameter> battlefieldDebugParameters() {
        return List.of(
                DebugParameter.number(
                        "battlefield.scale",
                        "Scale",
                        "Battlefield",
                        0.5,
                        6,
                        0.01,
                        battlefieldPlane::getScaleX,
                        this::setBattlefieldScale
                ),
                DebugParameter.number(
                        "battlefield.ambient",
                        "Ambient",
                        "Battlefield",
                        0,
                        1,
                        0.01,
                        () -> BattlefieldLighting.ambientIntensity(battlefieldAmbientLight),
                        this::setBattlefieldAmbient
                )
        );
    }

    private String loadShader(String path) {
        try (var input = getClass().getResourceAsStream(path)) {
            Objects.requireNonNull(input, "Missing " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load shader: " + path, exception);
        }
    }
}
