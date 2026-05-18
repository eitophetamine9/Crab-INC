package crab.features.gameplay.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PlayerState implements java.io.Serializable {
    public static final int MAX_HAND_SIZE = 3;

    private final String id;
    private final String displayName;
    private final PlayerClass playerClass;
    private final List<ActionCard> hand = new ArrayList<>();
    private int clams;
    private int wealth;
    private int reputation;
    private int infamy;
    private int buildLevel = 1;

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

    public int clams() {
        return clams;
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

    public int buildLevel() {
        return buildLevel;
    }

    public int income() {
        return switch (buildLevel) {
            case 1 -> 15;
            case 2 -> 40;
            case 3 -> 85;
            case 4 -> 150;
            case 5 -> 250;
            default -> 250;
        };
    }

    public double statBonus() {
        return switch (buildLevel) {
            case 1 -> 0.0;
            case 2 -> 0.10;
            case 3 -> 0.22;
            case 4 -> 0.38;
            case 5 -> 0.60;
            default -> 0.60;
        };
    }

    public int upgradeCost() {
        return switch (buildLevel) {
            case 1 -> 30;
            case 2 -> 60;
            case 3 -> 110;
            case 4 -> 180;
            case 5 -> -1; // Max level reached
            default -> -1;
        };
    }

    public boolean canUpgradeBuild() {
        int cost = upgradeCost();
        return cost != -1 && clams >= cost;
    }

    public void upgradeBuild() {
        if (canUpgradeBuild()) {
            deductClams(upgradeCost());
            incrementBuildLevel();
        }
    }

    public void incrementBuildLevel() {
        if (buildLevel < 5) {
            buildLevel++;
        }
    }
    public List<ActionCard> hand() {
        return List.copyOf(hand);
    }

    public void addClams(int amount) {
        clams = Math.max(0, clams + amount);
    }

    public void deductClams(int amount) {
        clams = Math.max(0, clams - amount);
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
        } else {
            hand.remove(0); // Replace oldest card (FIFO)
            hand.add(card);
        }
    }

    public void removeCard(ActionCard card) {
        hand.remove(card);
    }

    /** Discards (removes) a specific card from the hand. Alias for removeCard for clarity. */
    public boolean discardCard(ActionCard card) {
        return hand.remove(card);
    }

    public void clearHand() {
        hand.clear();
    }
}
