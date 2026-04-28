package crab.features.gameplay.domain;

import java.util.Objects;

public record ActionCard(String id, String name, CardType type, CardRarity rarity, int baseEffect) {
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
        if (baseEffect < 0) {
            throw new IllegalArgumentException("Card base effect cannot be negative");
        }
    }
}
