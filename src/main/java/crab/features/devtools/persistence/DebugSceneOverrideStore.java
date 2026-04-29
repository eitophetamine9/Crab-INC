package crab.features.devtools.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.properties.LightAdapter;
import crab.features.devtools.properties.MaterialAdapter;
import crab.features.devtools.properties.NodeTransformAdapter;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DebugSceneOverrideStore {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path directory;

    public DebugSceneOverrideStore(Path directory) {
        this.directory = directory;
    }

    public void saveInspectable(Inspectable3D item) {
        if (!item.persistent()) {
            return;
        }

        DebugSceneOverrides overrides = read(item.screenId());
        overrides.objects.put(item.id(), ObjectOverride.from(item));
        write(item.screenId(), overrides);
    }

    public void applyOverrides(Inspectable3D item) {
        if (!item.persistent()) {
            return;
        }

        ObjectOverride override = read(item.screenId()).objects.get(item.id());
        if (override != null) {
            override.applyTo(item);
        }
    }

    private DebugSceneOverrides read(String screenId) {
        Path file = fileFor(screenId);
        if (!Files.isRegularFile(file)) {
            return new DebugSceneOverrides();
        }

        try {
            return JSON.readValue(file.toFile(), DebugSceneOverrides.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read dev scene overrides: " + file, exception);
        }
    }

    private void write(String screenId, DebugSceneOverrides overrides) {
        Path file = fileFor(screenId);
        try {
            Files.createDirectories(directory);
            JSON.writeValue(file.toFile(), overrides);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to write dev scene overrides: " + file, exception);
        }
    }

    private Path fileFor(String screenId) {
        return directory.resolve(screenId + ".scene-debug.json");
    }

    public static final class DebugSceneOverrides {
        public Map<String, ObjectOverride> objects = new LinkedHashMap<>();
    }

    public static final class ObjectOverride {
        public TransformOverride transform = new TransformOverride();
        public Boolean visible;
        public String diffuseColor;
        public String specularColor;
        public String lightColor;
        public Map<String, Double> parameters = new LinkedHashMap<>();

        private static ObjectOverride from(Inspectable3D item) {
            ObjectOverride override = new ObjectOverride();
            override.transform = TransformOverride.from(new NodeTransformAdapter(item.target()).snapshot());
            override.visible = item.target().isVisible();
            MaterialAdapter.forNode(item.target()).ifPresent(adapter -> {
                override.diffuseColor = colorToString(adapter.diffuseColor());
                override.specularColor = colorToString(adapter.specularColor());
            });
            LightAdapter.forNode(item.target()).ifPresent(adapter -> override.lightColor = colorToString(adapter.color()));
            for (DebugParameter parameter : item.debugParameters()) {
                override.parameters.put(parameter.id(), parameter.value());
            }
            return override;
        }

        private void applyTo(Inspectable3D item) {
            NodeTransformAdapter transformAdapter = new NodeTransformAdapter(item.target());
            if (transform != null) {
                transformAdapter.setPosition(transform.x, transform.y, transform.z);
                transformAdapter.setRotation(transform.rotateX, transform.rotateY, transform.rotateZ);
                transformAdapter.setScale(transform.scaleX, transform.scaleY, transform.scaleZ);
            }
            if (visible != null) {
                item.target().setVisible(visible);
            }
            MaterialAdapter.forNode(item.target()).ifPresent(adapter -> {
                if (diffuseColor != null) {
                    adapter.setDiffuseColor(Color.web(diffuseColor));
                }
                if (specularColor != null) {
                    adapter.setSpecularColor(Color.web(specularColor));
                }
            });
            LightAdapter.forNode(item.target()).ifPresent(adapter -> {
                if (lightColor != null) {
                    adapter.setColor(Color.web(lightColor));
                }
            });
            for (DebugParameter parameter : item.debugParameters()) {
                Double value = parameters.get(parameter.id());
                if (value != null) {
                    parameter.setValue(value);
                }
            }
        }
    }

    public static final class TransformOverride {
        public double x;
        public double y;
        public double z;
        public double rotateX;
        public double rotateY;
        public double rotateZ;
        public double scaleX = 1;
        public double scaleY = 1;
        public double scaleZ = 1;

        private static TransformOverride from(NodeTransformAdapter.TransformSnapshot snapshot) {
            TransformOverride override = new TransformOverride();
            override.x = snapshot.x();
            override.y = snapshot.y();
            override.z = snapshot.z();
            override.rotateX = snapshot.rotateX();
            override.rotateY = snapshot.rotateY();
            override.rotateZ = snapshot.rotateZ();
            override.scaleX = snapshot.scaleX();
            override.scaleY = snapshot.scaleY();
            override.scaleZ = snapshot.scaleZ();
            return override;
        }
    }

    private static String colorToString(Color color) {
        if (color == null) {
            return null;
        }

        return String.format(
                "#%02X%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                (int) Math.round(color.getOpacity() * 255)
        );
    }
}
