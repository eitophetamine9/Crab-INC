package crab.features.gameplay.domain;

public enum CardRarity {
    COMMON(1.0),
    UNCOMMON(1.5),
    RARE(2.5);

    private final double multiplier;

    CardRarity(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }
}
