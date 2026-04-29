package crab.features.devtools.domain;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;
import java.util.Optional;

public final class SceneTreeBuilder {
    private final Inspectable3DRegistry registry;

    public SceneTreeBuilder(Inspectable3DRegistry registry) {
        this.registry = registry;
    }

    public SceneTreeNode build(String screenId, Node root) {
        return build(screenId, root, List.of());
    }

    public SceneTreeNode build(String screenId, Node root, List<Node> detachedNodes) {
        Optional<Inspectable3D> inspectable = registry.findExactForNode(root);
        List<SceneTreeNode> children = new java.util.ArrayList<>(
                childNodes(root).stream().map(child -> build(screenId, child)).toList()
        );
        children.addAll(detachedNodes.stream().map(node -> build(screenId, node)).toList());
        return new SceneTreeNode(
                screenId,
                root,
                displayName(root, inspectable),
                inspectable,
                children
        );
    }

    private static List<Node> childNodes(Node node) {
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable();
        }

        return List.of();
    }

    private static String displayName(Node node, Optional<Inspectable3D> inspectable) {
        if (inspectable.isPresent()) {
            return inspectable.get().name() + " [" + node.getClass().getSimpleName() + "]";
        }

        String id = node.getId();
        if (id != null && !id.isBlank()) {
            return id + " [" + node.getClass().getSimpleName() + "]";
        }

        return node.getClass().getSimpleName();
    }
}
