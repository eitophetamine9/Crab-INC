package crab.features.devtools.domain;

import javafx.scene.Node;

import java.util.List;
import java.util.Objects;

public record Inspectable3D(
        String id,
        String name,
        String screenId,
        Node target,
        List<DebugParameter> debugParameters
) {
    public Inspectable3D {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(target, "target");
        debugParameters = List.copyOf(Objects.requireNonNull(debugParameters, "debugParameters"));
    }

    public static Inspectable3D forNode(String id, String name, String screenId, Node target) {
        return new Inspectable3D(id, name, screenId, target, List.of());
    }

    public static Inspectable3D forNode(
            String id,
            String name,
            String screenId,
            Node target,
            List<DebugParameter> debugParameters
    ) {
        return new Inspectable3D(id, name, screenId, target, debugParameters);
    }

    @Override
    public String toString() {
        return name;
    }
}
