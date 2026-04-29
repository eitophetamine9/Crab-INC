package crab.features.demo.presentation.components;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Builds a horizontal 3D plane with UVs for a live shader render target.
 */
public final class ShaderTexturedPlane {
    private ShaderTexturedPlane() {
    }

    public static MeshView create(double width, double depth, Image texture) {
        float halfWidth = (float) width / 2.0f;
        float halfDepth = (float) depth / 2.0f;

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                -halfWidth, 0.0f, -halfDepth,
                halfWidth, 0.0f, -halfDepth,
                -halfWidth, 0.0f, halfDepth,
                halfWidth, 0.0f, halfDepth
        );
        mesh.getTexCoords().addAll(
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        );
        mesh.getFaces().addAll(
                0, 0, 2, 2, 1, 1,
                1, 1, 2, 2, 3, 3
        );

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(texture);
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(Color.BLACK);
        material.setSpecularPower(0);

        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setMaterial(material);
        return view;
    }
}
