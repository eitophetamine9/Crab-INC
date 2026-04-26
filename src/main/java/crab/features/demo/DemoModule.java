package crab.features.demo;

import com.almasb.fxgl.entity.Entity;
import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.features.demo.presentation.DemoPanelController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
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
    private Entity sampleEntity;
    private Box box3d;
    private double elapsedSeconds;

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

        onKey(javafx.scene.input.KeyCode.W, () -> sampleEntity.translateY(-8));
        onKey(javafx.scene.input.KeyCode.S, () -> sampleEntity.translateY(8));
        onKey(javafx.scene.input.KeyCode.A, () -> sampleEntity.translateX(-8));
        onKey(javafx.scene.input.KeyCode.D, () -> sampleEntity.translateX(8));
    }

    @Override
    public void initializeUi() {
        getGameScene().addUINode(loadDemoPanel());
        getGameScene().addUINode(createInteractive3dPanel());
    }

    @Override
    public void update(double tpf) {
        elapsedSeconds += tpf;

        if (box3d != null) {
            box3d.setRotationAxis(Rotate.Y_AXIS);
            box3d.setRotate(elapsedSeconds * 28);
        }
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
        box3d = new Box(120, 120, 120);
        box3d.setMaterial(new PhongMaterial(Color.DARKTURQUOISE));
        box3d.setTranslateX(170);
        box3d.setTranslateY(150);
        box3d.setTranslateZ(0);
        box3d.setOnMouseClicked(event ->
                box3d.setMaterial(new PhongMaterial(Color.hsb(Math.random() * 360, 0.72, 0.92))));

        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(80);
        light.setTranslateY(20);
        light.setTranslateZ(-220);

        Group world3d = new Group(box3d, light);
        SubScene subScene = new SubScene(world3d, 340, 300, true, null);
        subScene.setFill(Color.rgb(26, 34, 46));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateX(170);
        camera.setTranslateY(150);
        camera.setTranslateZ(-520);
        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        subScene.setCamera(camera);

        Label label = new Label("JavaFX 3D: click the cube");
        label.setTextFill(Color.WHITE);
        StackPane.setMargin(label, new Insets(10, 0, 0, 0));

        StackPane wrapper = new StackPane(subScene, label);
        wrapper.setTranslateX(642);
        wrapper.setTranslateY(216);
        return wrapper;
    }
}
