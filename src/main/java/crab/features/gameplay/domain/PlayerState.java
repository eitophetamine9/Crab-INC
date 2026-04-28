package crab.features.gameplay.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PlayerState {
    public static final int MAX_HAND_SIZE = 3;

    private final String id;
    private final String displayName;
    private final PlayerClass playerClass;
    private final List<ActionCard> hand = new ArrayList<>();
    private int gold;
    private int wealth;
    private int reputation;
    private int infamy;
    private BuildLevel buildLevel = BuildLevel.ONE;

    private PlayerState(String id, String displayName, PlayerClass playerClass) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.playerClass = Objects.requireNonNull(playerClass, "playerClass");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Player id cannot be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }
    }

    public static PlayerState create(String id, String displayName, PlayerClass playerClass) {
        return new PlayerState(id, displayName, playerClass);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public PlayerClass playerClass() {
        return playerClass;
    }

    public int gold() {
        return gold;
    }

    public int wealth() {
        return wealth;
    }

    public int reputation() {
        return reputation;
    }

    public int infamy() {
        return infamy;
    }

    public BuildLevel buildLevel() {
        return buildLevel;
    }

    public List<ActionCard> hand() {
        return List.copyOf(hand);
    }

    public void addGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    public void addWealth(int amount) {
        wealth = Math.max(0, wealth + amount);
    }

    public void addReputation(int amount) {
        reputation += amount;
    }

    public void addInfamy(int amount) {
        infamy = Math.max(0, infamy + amount);
    }

    public void addCard(ActionCard card) {
        Objects.requireNonNull(card, "card");
        if (hand.size() < MAX_HAND_SIZE) {
            hand.add(card);
        }
    }

    public void removeCard(ActionCard card) {
        hand.remove(card);
    }

    public boolean upgradeBuild() {
        return buildLevel.next()
                .filter(next -> gold >= next.upgradeCost())
                .map(next -> {
                    gold -= next.upgradeCost();
                    buildLevel = next;
                    return true;
                })
                .orElse(false);
    }
}
