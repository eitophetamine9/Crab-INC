package crab.features.gameplay.domain;

public record GameEvent(String name, String targetPlayerId, int clamsDelta, int wealthDelta, int reputationDelta, int infamyDelta) implements java.io.Serializable {
    public static GameEvent none() {
        return new GameEvent("Calm Current", null, 0, 0, 0, 0);
    }

    public static GameEvent crabHunt(String targetId, int clamsLoss) {
        return new GameEvent("Crab Hunt", targetId, -clamsLoss, 0, 0, 0);
    }

    public static GameEvent travellingShop() {
        return new GameEvent("Travelling Shop", null, 0, 0, 0, 0);
    }

    public static GameEvent marketCrash(int wealthLoss) {
        return new GameEvent("Market Crash", null, 0, -wealthLoss, 0, 0);
    }

    public static GameEvent charityWave() {
        return new GameEvent("Charity Wave", null, 0, 0, 0, 0);
    }
}
