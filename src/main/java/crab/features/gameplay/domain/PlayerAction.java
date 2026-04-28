package crab.features.gameplay.domain;

import java.util.Objects;

public record PlayerAction(String playerId, ActionCard card, String targetPlayerId) {
    public PlayerAction {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(card, "card");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("Player id cannot be blank");
        }
    }
}
