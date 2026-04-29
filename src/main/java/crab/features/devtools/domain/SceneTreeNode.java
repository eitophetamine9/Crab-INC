package crab.features.devtools.domain;

import javafx.scene.Node;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record SceneTreeNode(
        String screenId,
        Node node,
        String displayName,
        Optional<Inspectable3D> inspectable,
        List<SceneTreeNode> children
) {
    public SceneTreeNode {
        Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(displayName, "displayName");
        inspectable = Objects.requireNonNull(inspectable, "inspectable");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public Optional<SceneTreeNode> filter(String query) {
        if (query == null || query.isBlank()) {
            return Optional.of(this);
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<SceneTreeNode> filteredChildren = children.stream()
                .map(child -> child.filter(query))
                .flatMap(Optional::stream)
                .toList();
        boolean matches = displayName.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || inspectable.map(item -> item.id().toLowerCase(Locale.ROOT).contains(normalizedQuery)).orElse(false)
                || Optional.ofNullable(node.getId()).map(id -> id.toLowerCase(Locale.ROOT).contains(normalizedQuery)).orElse(false);

        if (matches || !filteredChildren.isEmpty()) {
            return Optional.of(new SceneTreeNode(screenId, node, displayName, inspectable, filteredChildren));
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
