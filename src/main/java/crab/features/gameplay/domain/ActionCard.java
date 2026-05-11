package crab.features.gameplay.domain;

import java.util.Objects;

public record ActionCard(String id, String name, CardType type, CardRarity rarity) implements java.io.Serializable {
    public ActionCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rarity, "rarity");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Card id cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Card name cannot be blank");
        }
    }
}
