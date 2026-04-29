package crab.features.devtools.domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DebugParameterTest {
    @Test
    void clampsNumericParameterBeforeWritingIt() {
        AtomicReference<Double> value = new AtomicReference<>(0.5);
        DebugParameter parameter = DebugParameter.number(
                "camera.pitch",
                "Pitch",
                "Camera",
                -90,
                0,
                0.5,
                value::get,
                value::set
        );

        parameter.setValue(12);
        assertEquals(0, value.get());

        parameter.setValue(-120);
        assertEquals(-90, value.get());

        parameter.setValue(-45.25);
        assertEquals(-45.25, value.get());
    }
}
