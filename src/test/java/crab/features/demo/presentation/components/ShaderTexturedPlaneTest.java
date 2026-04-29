package crab.features.demo.presentation.components;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ShaderTexturedPlaneTest {
    @Test
    void createsHorizontalMeshWithDiffuseShaderTexture() {
        WritableImage texture = new WritableImage(8, 8);

        MeshView plane = ShaderTexturedPlane.create(1200, 960, texture);
        TriangleMesh mesh = (TriangleMesh) plane.getMesh();
        PhongMaterial material = (PhongMaterial) plane.getMaterial();

        assertEquals(12, mesh.getPoints().size());
        assertEquals(8, mesh.getTexCoords().size());
        assertEquals(12, mesh.getFaces().size());
        assertSame(texture, material.getDiffuseMap());
        assertEquals(Color.BLACK, material.getSpecularColor());
        assertEquals(0.0, material.getSpecularPower());
    }
}
