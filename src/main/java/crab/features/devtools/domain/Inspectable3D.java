package crab.features.devtools.domain;

import javafx.scene.Node;

import java.util.List;
import java.util.Objects;

public record Inspectable3D(
        String id,
        String name,
        String screenId,
        Node target,
        List<DebugParameterGroup> debugParameterGroups,
        boolean persistent
) {
    public Inspectable3D {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(target, "target");
        debugParameterGroups = List.copyOf(Objects.requireNonNull(debugParameterGroups, "debugParameterGroups"));
    }

    public static Inspectable3D forNode(String id, String name, String screenId, Node target) {
        return new Inspectable3D(id, name, screenId, target, List.of(), true);
    }

    public static Inspectable3D forNode(
            String id,
            String name,
            String screenId,
            Node target,
            List<DebugParameter> debugParameters
    ) {
        return new Inspectable3D(id, name, screenId, target, List.of(new DebugParameterGroup("Debug Parameters", debugParameters)), true);
    }

    public static Inspectable3D forNodeWithGroups(
            String id,
            String name,
            String screenId,
            Node target,
            List<DebugParameterGroup> debugParameterGroups
    ) {
        return new Inspectable3D(id, name, screenId, target, debugParameterGroups, true);
    }

    public static Inspectable3D temporaryForNode(String screenId, Node target) {
        return new Inspectable3D(
                "node." + Integer.toHexString(System.identityHashCode(target)),
                target.getClass().getSimpleName(),
                screenId,
                target,
                List.of(),
                false
        );
    }

    public List<DebugParameter> debugParameters() {
        return debugParameterGroups.stream()
                .flatMap(group -> group.parameters().stream())
                .toList();
    }

    @Override
    public String toString() {
        return name;
    }
}
