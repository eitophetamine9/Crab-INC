package crab.features.demo;

import com.almasb.fxgl.entity.Entity;
import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.features.demo.presentation.DemoPanelController;
import crab.features.demo.presentation.StylizedPhongMaterialController;
import crab.platform.javafx3d.GltfMeshLoader;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.net.URL;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Foundation demo feature proving FXGL, FXML, and JavaFX 3D can coexist.
 *
 * Design patterns:
 * - Adapter: demonstrates JavaFX/FXML/3D nodes hosted inside FXGL.
 *
 * SOLID:
 * - Single Responsibility: owns only the architecture proof scene.
 */
public final class DemoModule implements GameModule {
    private static final double MODEL_CENTER_X = 170;
    private static final double MODEL_CENTER_Y = 150;

    private Entity sampleEntity;
    private MeshView bunnyMesh;
    private Group bunnyPivot;
    private final Rotate bunnyImportCorrection = new Rotate(90, Rotate.X_AXIS);
    private final Rotate bunnyYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate bunnyPitch = new Rotate(-12, Rotate.X_AXIS);
    private final Rotate bunnyRoll = new Rotate(0, Rotate.Z_AXIS);
    private final StylizedPhongMaterialController materialController = new StylizedPhongMaterialController();

    @Override
    public void initialize(GameContext context) {
        context.register(DemoModule.class, this);
    }

    @Override
    public void start() {
        getGameScene().setBackgroundColor(Color.rgb(16, 22, 31));

        sampleEntity = entityBuilder()
                .at(140, 320)
                .view(new Rectangle(96, 96, Color.CRIMSON))
                .buildAndAttach();

        Label hint = new Label("FXGL entity: WASD moves this square");
        hint.setTextFill(Color.WHITE);
        hint.setTranslateX(32);
        hint.setTranslateY(666);
        getGameScene().addUINode(hint);

        onKey(KeyCode.W, () -> sampleEntity.translateY(-8));
        onKey(KeyCode.S, () -> sampleEntity.translateY(8));
        onKey(KeyCode.A, () -> sampleEntity.translateX(-8));
        onKey(KeyCode.D, () -> sampleEntity.translateX(8));
    }

    @Override
    public void initializeUi() {
        getGameScene().addUINode(loadDemoPanel());
        getGameScene().addUINode(createInteractive3dPanel());
    }

    @Override
    public void update(double tpf) {
    }

    @Override
    public void stop() {
    }

    private Parent loadDemoPanel() {
        URL resource = getClass().getResource("/fxml/demo-panel.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/demo-panel.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            DemoPanelController controller = loader.getController();
            controller.setStatusText("FXML loaded through DemoModule");
            controller.setYawConsumer(this::setYaw);
            controller.setPitchConsumer(this::setPitch);
            controller.setRollConsumer(this::setRoll);
            controller.setToonParameterConsumer(materialController::update);
            root.setTranslateX(32);
            root.setTranslateY(32);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("FXML load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    private Parent createInteractive3dPanel() {
        Node bunny = createBunnyModel();

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
        SubScene subScene = new SubScene(world3d, 340, 300, true, null);
        subScene.setFill(Color.rgb(26, 34, 46));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateX(170);
        camera.setTranslateY(150);
        camera.setTranslateZ(-520);
        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        subScene.setCamera(camera);

        Label label = new Label("JavaFX 3D: inspect bunny, tune material");
        label.setTextFill(Color.WHITE);
        StackPane.setMargin(label, new Insets(10, 0, 0, 0));

        StackPane wrapper = new StackPane(subScene, label);
        wrapper.setTranslateX(642);
        wrapper.setTranslateY(216);
        return wrapper;
    }

    private Node createBunnyModel() {
        URL gltfUrl = getClass().getResource("/assets/models/demo/stanford_bunny/scene.gltf");
        if (gltfUrl == null) {
            return new Label("Missing Stanford Bunny asset");
        }

        try {
            bunnyMesh = new MeshView(GltfMeshLoader.load(gltfUrl));
            bunnyMesh.setMaterial(materialController.material());
            bunnyMesh.setCullFace(CullFace.BACK);

            Group bunnyModel = new Group(bunnyMesh);
            bunnyModel.getTransforms().setAll(bunnyImportCorrection, bunnyYaw, bunnyPitch, bunnyRoll);

            bunnyPivot = new Group(bunnyModel);
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

    private void setYaw(double yaw) {
        bunnyYaw.setAngle(yaw);
    }

    private void setPitch(double pitch) {
        bunnyPitch.setAngle(pitch);
    }

    private void setRoll(double roll) {
        bunnyRoll.setAngle(roll);
    }
}
