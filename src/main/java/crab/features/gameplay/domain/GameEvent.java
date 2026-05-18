package crab.features.gameplay.domain;

public record GameEvent(String name, String description, String targetPlayerId, int clamsDelta, int wealthDelta, int reputationDelta) implements java.io.Serializable {
    public static GameEvent none() {
        return new GameEvent("Calm Current", "The waters are peaceful this round.", null, 0, 0, 0);
    }

    public static GameEvent crabHunt(String targetId, int clamsLoss) {
        return new GameEvent("Crab Hunt", "The wealthiest player is being hunted! Lose " + clamsLoss + " Clams.", targetId, -clamsLoss, 0, 0);
    }

    public static GameEvent travellingShop() {
        return new GameEvent("Travelling Shop", "A rare card merchant is passing by! Buy a Rare card for 50 Clams.", null, 0, 0, 0);
    }

    public static GameEvent marketCrash(int wealthLoss) {
        return new GameEvent("Market Crash", "The ocean market has plummeted! All players lose " + wealthLoss + " Wealth.", null, 0, -wealthLoss, 0);
    }

    public static GameEvent charityWave() {
        return new GameEvent("Charity Wave", "A wave of altruism! Help cards grant +20 Reputation next round.", null, 0, 0, 0);
    }
}
