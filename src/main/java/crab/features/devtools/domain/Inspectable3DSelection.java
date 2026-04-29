package crab.features.devtools.domain;

import java.util.Optional;

public final class Inspectable3DSelection {
    private Inspectable3D selected;

    public void select(Inspectable3D item) {
        selected = item;
    }

    public Optional<Inspectable3D> selected() {
        return Optional.ofNullable(selected);
    }

    public void clear() {
        selected = null;
    }
}
