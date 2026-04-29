package crab.features.devtools.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneTreeBuilderTest {
    @Test
    void buildsTreeFromAllJavaFxChildrenAndMarksInspectableNodes() {
        Group root = new Group();
        Group branch = new Group();
        Box leaf = new Box();
        root.getChildren().add(branch);
        branch.getChildren().add(leaf);

        Inspectable3DRegistry registry = new Inspectable3DRegistry();
        Inspectable3D inspectable = Inspectable3D.forNode("leaf.box", "Leaf Box", "demo", leaf);
        registry.register(inspectable);

        SceneTreeNode tree = new SceneTreeBuilder(registry).build("demo", root);

        assertSame(root, tree.node());
        assertEquals(1, tree.children().size());
        assertSame(branch, tree.children().getFirst().node());
        SceneTreeNode leafNode = tree.children().getFirst().children().getFirst();
        assertSame(leaf, leafNode.node());
        assertEquals(inspectable, leafNode.inspectable().orElseThrow());
        assertTrue(leafNode.displayName().contains("Leaf Box"));
    }

    @Test
    void findsNodeByTextFilterWhileKeepingMatchingAncestors() {
        Group root = new Group();
        Group branch = new Group();
        Box leaf = new Box();
        leaf.setId("shore-plane");
        root.getChildren().add(branch);
        branch.getChildren().add(leaf);

        SceneTreeNode tree = new SceneTreeBuilder(new Inspectable3DRegistry()).build("demo", root);
        SceneTreeNode filtered = tree.filter("shore").orElseThrow();

        assertSame(root, filtered.node());
        assertSame(branch, filtered.children().getFirst().node());
        assertSame(leaf, filtered.children().getFirst().children().getFirst().node());
    }

    @Test
    void appendsDetachedNodesSuchAsSubSceneCameras() {
        Group root = new Group();
        Box cameraNode = new Box();
        Inspectable3DRegistry registry = new Inspectable3DRegistry();
        Inspectable3D camera = Inspectable3D.forNode("demo.camera.dev", "Dev Camera", "demo", cameraNode);
        registry.register(camera);

        SceneTreeNode tree = new SceneTreeBuilder(registry).build("demo", root, List.of(cameraNode));

        assertEquals(1, tree.children().size());
        SceneTreeNode cameraTreeNode = tree.children().getFirst();
        assertSame(cameraNode, cameraTreeNode.node());
        assertEquals(camera, cameraTreeNode.inspectable().orElseThrow());
        assertTrue(cameraTreeNode.displayName().contains("Dev Camera"));
    }
}
