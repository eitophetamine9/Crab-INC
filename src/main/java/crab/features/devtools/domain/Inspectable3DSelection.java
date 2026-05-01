package crab.features.devtools.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Inspectable3DSelection {
    private final Set<Inspectable3D> selected = new LinkedHashSet<>();

    public void select(Inspectable3D item) {
        selected.clear();
        selected.add(item);
    }

    public void toggle(Inspectable3D item) {
        if (selected.contains(item)) {
            selected.remove(item);
            return;
        }

        selected.add(item);
    }

    public boolean contains(Inspectable3D item) {
        return selected.contains(item);
    }

    public List<Inspectable3D> selectedItems() {
        return List.copyOf(selected);
    }

    public Optional<Inspectable3D> selected() {
        if (selected.isEmpty()) {
            return Optional.empty();
        }

        List<Inspectable3D> items = new ArrayList<>(selected);
        return Optional.of(items.getLast());
    }

    public void clear() {
        selected.clear();
    }
}
