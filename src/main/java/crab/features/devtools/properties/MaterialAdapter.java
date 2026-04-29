package crab.features.devtools.properties;

import com.almasb.fxgl.scene3d.Model3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;

import java.util.Optional;

public final class MaterialAdapter {
    private final PhongMaterial material;

    private MaterialAdapter(PhongMaterial material) {
        this.material = material;
    }

    public static Optional<MaterialAdapter> forNode(Node node) {
        if (node instanceof Shape3D shape && shape.getMaterial() instanceof PhongMaterial phongMaterial) {
            return Optional.of(new MaterialAdapter(phongMaterial));
        }

        if (node instanceof Model3D model && model.getMaterial() instanceof PhongMaterial phongMaterial) {
            return Optional.of(new MaterialAdapter(phongMaterial));
        }

        return Optional.empty();
    }

    public Color diffuseColor() {
        return material.getDiffuseColor();
    }

    public Color specularColor() {
        return material.getSpecularColor();
    }

    public void setDiffuseColor(Color color) {
        material.setDiffuseColor(color);
    }

    public void setSpecularColor(Color color) {
        material.setSpecularColor(color);
    }
}
