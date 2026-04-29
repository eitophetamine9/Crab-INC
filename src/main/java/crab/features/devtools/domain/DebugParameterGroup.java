package crab.features.devtools.domain;

import java.util.List;
import java.util.Objects;

public record DebugParameterGroup(String name, List<DebugParameter> parameters) {
    public DebugParameterGroup {
        Objects.requireNonNull(name, "name");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }
}
