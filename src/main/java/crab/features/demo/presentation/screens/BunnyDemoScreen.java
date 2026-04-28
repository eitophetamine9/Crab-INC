package crab.features.demo.presentation.screens;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import crab.features.demo.presentation.components.BunnyControlPanelController;
import crab.features.demo.presentation.components.DemoNavigatorController;
import crab.features.demo.presentation.components.ShadertoyBackgroundView;
import crab.features.demo.presentation.components.StylizedPhongMaterialController;
import crab.platform.javafx3d.GltfMeshLoader;
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
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Objects;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;

/**
 * Demo screen focused only on the centered Stanford Bunny mesh.
 *
 * Design patterns:
 * - State: one navigable screen implementation in the application flow.
 *
 * SOLID:
 * - Single Responsibility: owns only the bunny demo presentation.
 */
public final class BunnyDemoScreen implements GameScreen {
    public static final String ID = "demo_bunny";

    private static final double PANEL_WIDTH = 720;
    private static final double PANEL_HEIGHT = 560;
    private static final double APP_WIDTH = 1024;
    private static final double APP_HEIGHT = 720;
    private static final double MODEL_CENTER_X = PANEL_WIDTH / 2.0;
    private static final double MODEL_CENTER_Y = PANEL_HEIGHT / 2.0;
    private final ScreenManager screens;
    private final Rotate bunnyImportCorrection = new Rotate(90, Rotate.X_AXIS);
    private final Rotate bunnyYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate bunnyPitch = new Rotate(-12, Rotate.X_AXIS);
    private final Rotate bunnyRoll = new Rotate(0, Rotate.Z_AXIS);
    private final StylizedPhongMaterialController materialController = new StylizedPhongMaterialController();
    private ShadertoyBackgroundView shaderBackground;
    private Node modelPanel;
    private Parent controlPanel;
    private Parent navigator;
    private boolean visible;

    public BunnyDemoScreen(ScreenManager screens) {
        this.screens = screens;
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
        getGameScene().setBackgroundColor(Color.rgb(16, 22, 31));
        modelPanel = createModelPanel();
        controlPanel = loadControlPanel();
        getGameScene().addUINode(modelPanel);
        getGameScene().addUINode(controlPanel);
        navigator = loadNavigator();
        getGameScene().addUINode(navigator);
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        visible = false;
        if (modelPanel != null) {
            getGameScene().removeUINode(modelPanel);
            modelPanel = null;
        }

        if (controlPanel != null) {
            getGameScene().removeUINode(controlPanel);
            controlPanel = null;
        }

        if (navigator != null) {
            getGameScene().removeUINode(navigator);
            navigator = null;
        }

        if (shaderBackground != null) {
            shaderBackground.dispose();
            shaderBackground = null;
        }
    }

    @Override
    public void update(double tpf) {
        if (visible && shaderBackground != null) {
            shaderBackground.update(tpf);
        }
    }

    private Node createModelPanel() {
        Node bunny = createBunnyModel();
        shaderBackground = new ShadertoyBackgroundView(
                (int) APP_WIDTH,
                (int) APP_HEIGHT,
                loadShader("/assets/shaders/stylized_shoreline.frag")
        );

        AmbientLight ambientLight = new AmbientLight(Color.rgb(72, 92, 116, 0.72));

        PointLight keyLight = new PointLight(Color.rgb(230, 246, 255));
        keyLight.setTranslateX(-150);
        keyLight.setTranslateY(-170);
        keyLight.setTranslateZ(-260);

        PointLight specularLight = new PointLight(Color.rgb(255, 247, 220));
        specularLight.setTranslateX(220);
        specularLight.setTranslateY(-90);
        specularLight.setTranslateZ(-180);

        Group world3d = new Group(bunny, ambientLight, keyLight, specularLight);
        world3d.setDepthTest(DepthTest.ENABLE);

        SubScene subScene = new SubScene(world3d, PANEL_WIDTH, PANEL_HEIGHT, true, null);
        subScene.setFill(Color.TRANSPARENT);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateX(MODEL_CENTER_X);
        camera.setTranslateY(MODEL_CENTER_Y);
        camera.setTranslateZ(-620);
        camera.setNearClip(0.1);
        camera.setFarClip(1400);
        subScene.setCamera(camera);
        subScene.setTranslateX((APP_WIDTH - PANEL_WIDTH) / 2.0);
        subScene.setTranslateY((APP_HEIGHT - PANEL_HEIGHT) / 2.0);

        Group panel = new Group(shaderBackground, subScene);
        return panel;
    }

    private Node createBunnyModel() {
        URL gltfUrl = getClass().getResource("/assets/models/demo/stanford_bunny/scene.gltf");
        if (gltfUrl == null) {
            return new Label("Missing Stanford Bunny asset");
        }

        try {
            MeshView bunnyMesh = new MeshView(GltfMeshLoader.load(gltfUrl));
            bunnyMesh.setMaterial(materialController.material());
            bunnyMesh.setCullFace(CullFace.NONE);

            Group bunnyModel = new Group(bunnyMesh);
            bunnyModel.getTransforms().setAll(bunnyImportCorrection, bunnyYaw, bunnyPitch, bunnyRoll);

            Group bunnyPivot = new Group(bunnyModel);
            bunnyPivot.setTranslateX(MODEL_CENTER_X);
            bunnyPivot.setTranslateY(MODEL_CENTER_Y);
            return bunnyPivot;
        } catch (IOException | RuntimeException exception) {
            Label fallback = new Label("Bunny load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            fallback.setTranslateX(36);
            fallback.setTranslateY(140);
            return fallback;
        }
    }

    private Parent loadControlPanel() {
        URL resource = getClass().getResource("/fxml/components/bunny-control-panel.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/components/bunny-control-panel.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            BunnyControlPanelController controller = loader.getController();
            controller.setStatusText("FXML loaded through BunnyDemoScreen");
            controller.setOrientationTitle("Bunny Orientation");
            controller.setDescriptionText("Use the sliders to inspect the Stanford Bunny mesh and tune the stylized material response.");
            controller.setYawConsumer(bunnyYaw::setAngle);
            controller.setPitchConsumer(bunnyPitch::setAngle);
            controller.setRollConsumer(bunnyRoll::setAngle);
            controller.setToonParameterConsumer(materialController::update);
            controller.setWaveSpeedConsumer(shaderBackground::setWaveSpeed);
            controller.setWaveHeightConsumer(shaderBackground::setWaveHeight);
            controller.setFoamConsumer(shaderBackground::setFoamAmount);
            controller.setShoreConsumer(shaderBackground::setShoreOffset);
            controller.setChromaticConsumer(shaderBackground::setChromaticAmount);
            root.setTranslateX(32);
            root.setTranslateY(32);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Control panel load failed: " + exception.getMessage());
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
                    () -> screens.show(BunnyDemoScreen.ID)
            );
            controller.setActiveScreen(ID);
            root.setTranslateX((APP_WIDTH - 272) / 2.0);
            root.setTranslateY(APP_HEIGHT - 72);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Navigator load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
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
