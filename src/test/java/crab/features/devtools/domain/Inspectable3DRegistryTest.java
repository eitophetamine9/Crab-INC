package crab.features.devtools.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Inspectable3DRegistryTest {
    @Test
    void findsRegisteredInspectableFromClickedDescendant() {
        Group target = new Group();
        Box child = new Box();
        target.getChildren().add(child);
        Inspectable3D item = Inspectable3D.forNode("crab", "Crab", "demo_crab", target);

        Inspectable3DRegistry registry = new Inspectable3DRegistry();
        registry.register(item);

        assertSame(item, registry.findForNode(child).orElseThrow());
    }

    @Test
    void prefersNearestRegisteredAncestor() {
        Group parent = new Group();
        Group target = new Group();
        Box child = new Box();
        parent.getChildren().add(target);
        target.getChildren().add(child);
        Inspectable3D parentItem = Inspectable3D.forNode("root", "Root", "demo_crab", parent);
        Inspectable3D targetItem = Inspectable3D.forNode("crab", "Crab", "demo_crab", target);

        Inspectable3DRegistry registry = new Inspectable3DRegistry();
        registry.register(parentItem);
        registry.register(targetItem);

        assertSame(targetItem, registry.findForNode(child).orElseThrow());
    }

    @Test
    void clearsOnlyItemsForScreenScope() {
        Node crabNode = new Group();
        Node bunnyNode = new Group();
        Inspectable3D crab = Inspectable3D.forNode("crab", "Crab", "demo_crab", crabNode);
        Inspectable3D bunny = Inspectable3D.forNode("bunny", "Bunny", "demo_bunny", bunnyNode);

        Inspectable3DRegistry registry = new Inspectable3DRegistry();
        registry.register(crab);
        registry.register(bunny);
        registry.clearScope("demo_crab");

        assertTrue(registry.findForNode(crabNode).isEmpty());
        assertEquals(bunny, registry.findForNode(bunnyNode).orElseThrow());
    }

    @Test
    void carriesScreenSpecificDebugParameters() {
        AtomicReference<Double> value = new AtomicReference<>(1.0);
        DebugParameter parameter = DebugParameter.number(
                "battlefield.scale",
                "Scale",
                "Battlefield",
                0.5,
                6,
                0.01,
                value::get,
                value::set
        );

        Inspectable3D item = Inspectable3D.forNode(
                "battlefield",
                "Battlefield",
                "demo_crab",
                new Group(),
                List.of(parameter)
        );

        item.debugParameters().getFirst().setValue(3.5);

        assertEquals("Scale", item.debugParameters().getFirst().name());
        assertEquals(3.5, value.get());
    }
}
