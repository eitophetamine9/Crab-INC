package crab.features.devtools.domain;

import java.util.List;

public record Inspectable3DSnapshot(
        String id,
        String name,
        String screenId,
        String type,
        List<EditablePropertySnapshot> properties
) {
}
