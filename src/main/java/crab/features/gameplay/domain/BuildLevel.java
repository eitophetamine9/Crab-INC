package crab.features.gameplay.domain;

import java.util.Optional;

public enum     BuildLevel {
    ONE(0, 10, 0.0),
    TWO(40, 25, 0.05),
    THREE(80, 50, 0.10),
    FOUR(150, 90, 0.15),
    FIVE(250, 150, 0.25);

    private final int upgradeCost;
    private final int income;
    private final double positiveGainBonus;

    BuildLevel(int upgradeCost, int income, double positiveGainBonus) {
        this.upgradeCost = upgradeCost;
        this.income = income;
        this.positiveGainBonus = positiveGainBonus;
    }

    public int upgradeCost() {
        return upgradeCost;
    }

    public int income() {
        return income;
    }

    public double positiveGainBonus() {
        return positiveGainBonus;
    }

    public Optional<BuildLevel> next() {
        int nextOrdinal = ordinal() + 1;
        BuildLevel[] levels = values();
        if (nextOrdinal >= levels.length) {
            return Optional.empty();
        }

        return Optional.of(levels[nextOrdinal]);
    }
}
