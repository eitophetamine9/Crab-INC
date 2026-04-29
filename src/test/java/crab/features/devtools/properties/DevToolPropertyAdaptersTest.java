package crab.features.devtools.properties;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevToolPropertyAdaptersTest {
    @Test
    void updatesNodeTransformProperties() {
        Group node = new Group();
        NodeTransformAdapter adapter = new NodeTransformAdapter(node);

        adapter.setPosition(12, 24, 36);
        adapter.setRotation(10, 20, 30);
        adapter.setScale(1.5, 2.0, 2.5);

        assertEquals(12, node.getTranslateX());
        assertEquals(24, node.getTranslateY());
        assertEquals(36, node.getTranslateZ());
        assertEquals(10, node.getRotationAxis().getX() == 1 ? node.getRotate() : adapter.snapshot().rotateX());
        assertEquals(20, adapter.snapshot().rotateY());
        assertEquals(30, adapter.snapshot().rotateZ());
        assertEquals(1.5, node.getScaleX());
        assertEquals(2.0, node.getScaleY());
        assertEquals(2.5, node.getScaleZ());
    }

    @Test
    void updatesPhongMaterialColors() {
        Box box = new Box();
        PhongMaterial material = new PhongMaterial(Color.RED);
        box.setMaterial(material);
        MaterialAdapter adapter = MaterialAdapter.forNode(box).orElseThrow();

        adapter.setDiffuseColor(Color.CORNFLOWERBLUE);
        adapter.setSpecularColor(Color.WHITE);

        assertEquals(Color.CORNFLOWERBLUE, material.getDiffuseColor());
        assertEquals(Color.WHITE, material.getSpecularColor());
    }

    @Test
    void updatesLightColorAndAmbientIntensity() {
        AmbientLight light = new AmbientLight(Color.GRAY);
        LightAdapter adapter = LightAdapter.forNode(light).orElseThrow();

        adapter.setColor(Color.ORANGE);
        assertEquals(Color.ORANGE, light.getColor());

        adapter.setAmbientIntensity(0.25);
        assertEquals(Color.gray(0.25), light.getColor());
    }
}
