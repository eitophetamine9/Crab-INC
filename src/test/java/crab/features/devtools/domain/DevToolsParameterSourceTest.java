package crab.features.devtools.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevToolsParameterSourceTest {
    @Test
    void flattensNamedParameterGroupsForInspectorRendering() {
        AtomicReference<Double> value = new AtomicReference<>(1.0);
        DebugParameter parameter = DebugParameter.number(
                "shore.waveSpeed",
                "Wave Speed",
                "Shoreline",
                0.1,
                2.5,
                0.1,
                value::get,
                value::set
        );
        DebugParameterSource source = () -> List.of(new DebugParameterGroup("Shoreline", List.of(parameter)));

        assertEquals(List.of(parameter), source.parameters());
    }
}
