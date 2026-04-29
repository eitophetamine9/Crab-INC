package crab.features.devtools.domain;

import java.util.List;

@FunctionalInterface
public interface DebugParameterSource {
    List<DebugParameterGroup> parameterGroups();

    default List<DebugParameter> parameters() {
        return parameterGroups().stream()
                .flatMap(group -> group.parameters().stream())
                .toList();
    }
}
