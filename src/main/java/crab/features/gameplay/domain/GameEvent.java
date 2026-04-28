package crab.features.gameplay.domain;

public record GameEvent(String name, String targetPlayerId, int goldDelta, int wealthDelta, int reputationDelta, int infamyDelta) {
    public static GameEvent none() {
        return new GameEvent("Calm Current", null, 0, 0, 0, 0);
    }

    public static GameEvent crabHunt(String targetPlayerId, int goldLoss) {
        if (goldLoss < 0) {
            throw new IllegalArgumentException("Gold loss must be positive");
        }

        return new GameEvent("Crab Hunt", targetPlayerId, -goldLoss, 0, 0, 0);
    }
}
