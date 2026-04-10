package model;

import javafx.beans.property.*;

public class Player {
    private final IntegerProperty playerId;
    private final StringProperty username;
    private final StringProperty businessTier;
    private final DoubleProperty estimatedCapital;
    private final IntegerProperty victoryPoints; // Added for gameplay loop

    public Player(int playerId, String username, String businessTier, double estimatedCapital) {
        this.playerId = new SimpleIntegerProperty(playerId);
        this.username = new SimpleStringProperty(username);
        this.businessTier = new SimpleStringProperty(businessTier);
        this.estimatedCapital = new SimpleDoubleProperty(estimatedCapital);
        this.victoryPoints = new SimpleIntegerProperty(0);
    }

    // Getters and Property methods for JavaFX TableView
    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public String getBusinessTier() { return businessTier.get(); }
    public StringProperty businessTierProperty() { return businessTier; }

    public double getEstimatedCapital() { return estimatedCapital.get(); }
    public DoubleProperty estimatedCapitalProperty() { return estimatedCapital; }

    public int getVictoryPoints() { return victoryPoints.get(); }
    public IntegerProperty victoryPointsProperty() { return victoryPoints; }

    // Logic to update stats
    public void addCapital(double amount) { setEstimatedCapital(getEstimatedCapital() + amount); }
    public void setEstimatedCapital(double value) { this.estimatedCapital.set(value); }
    public void addVictoryPoints(int points) { this.victoryPoints.set(getVictoryPoints() + points); }
}