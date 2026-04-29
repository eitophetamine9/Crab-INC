package crab.features.devtools.domain;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class Inspectable3DRegistry {
    private final List<Inspectable3D> items = new ArrayList<>();

    public void register(Inspectable3D item) {
        items.removeIf(existing -> existing.id().equals(item.id()) && existing.screenId().equals(item.screenId()));
        items.add(item);
    }

    public Optional<Inspectable3D> findForNode(Node node) {
        Node current = node;
        while (current != null) {
            Node candidate = current;
            Optional<Inspectable3D> match = items.stream()
                    .filter(item -> item.target() == candidate)
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }

            current = current.getParent();
        }

        return Optional.empty();
    }

    public List<Inspectable3D> items() {
        return List.copyOf(items);
    }

    public void clearScope(String screenId) {
        for (Iterator<Inspectable3D> iterator = items.iterator(); iterator.hasNext(); ) {
            if (iterator.next().screenId().equals(screenId)) {
                iterator.remove();
            }
        }
    }
}
