package crab.features.devtools.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScrubbableNumberTest {
    @Test
    void mapsHorizontalDragDistanceToNumericDelta() {
        assertEquals(12.5, ScrubbableNumber.valueForDrag(10, 50, 0.05));
        assertEquals(7.5, ScrubbableNumber.valueForDrag(10, -50, 0.05));
    }
}
