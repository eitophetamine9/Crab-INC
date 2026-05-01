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

    @Test
    void togglesMultipleSelectedInspectablesAndKeepsPrimaryAsLatestSelection() {
        Inspectable3D crab = Inspectable3D.forNode("crab", "Crab", "demo_crab", new Group());
        Inspectable3D plane = Inspectable3D.forNode("plane", "Plane", "demo_crab", new Group());
        Inspectable3DSelection selection = new Inspectable3DSelection();

        selection.select(crab);
        selection.toggle(plane);

        assertEquals(2, selection.selectedItems().size());
        assertEquals(plane, selection.selected().orElseThrow());

        selection.toggle(plane);

        assertEquals(1, selection.selectedItems().size());
        assertEquals(crab, selection.selected().orElseThrow());
    }

    @Test
    void selectingWithoutToggleReplacesPreviousSelection() {
        Inspectable3D crab = Inspectable3D.forNode("crab", "Crab", "demo_crab", new Group());
        Inspectable3D plane = Inspectable3D.forNode("plane", "Plane", "demo_crab", new Group());
        Inspectable3DSelection selection = new Inspectable3DSelection();

        selection.select(crab);
        selection.select(plane);

        assertEquals(1, selection.selectedItems().size());
        assertEquals(plane, selection.selected().orElseThrow());
    }
}
