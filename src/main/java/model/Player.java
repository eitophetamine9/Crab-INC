package model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Player {
    private final IntegerProperty playerId;
    private final StringProperty username;
    private final StringProperty businessTier;
    private final DoubleProperty estimatedCapital;

    public Player(int playerId, String username, String businessTier, double estimatedCapital) {
        this.playerId = new SimpleIntegerProperty(playerId);
        this.username = new SimpleStringProperty(username);
        this.businessTier = new SimpleStringProperty(businessTier);
        this.estimatedCapital = new SimpleDoubleProperty(estimatedCapital);
    }

    public IntegerProperty playerIdProperty() { return playerId; }
    public int getPlayerId() { return playerId.get(); }

    public StringProperty usernameProperty() { return username; }
    public String getUsername() { return username.get(); }

    public StringProperty businessTierProperty() { return businessTier; }
    public String getBusinessTier() { return businessTier.get(); }

    public DoubleProperty estimatedCapitalProperty() { return estimatedCapital; }
    public double getEstimatedCapital() { return estimatedCapital.get(); }
}