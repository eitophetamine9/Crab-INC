package crab.features.devtools.domain;

import javafx.scene.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Inspectable3DSelectionTest {
    @Test
    void tracksAndClearsSelectedInspectable() {
        Inspectable3D item = Inspectable3D.forNode("crab", "Crab", "demo_crab", new Group());
        Inspectable3DSelection selection = new Inspectable3DSelection();

        selection.select(item);

        assertEquals(item, selection.selected().orElseThrow());

        selection.clear();

        assertTrue(selection.selected().isEmpty());
    }
}
