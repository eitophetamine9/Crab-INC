package model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GameEngine {
    private static GameEngine instance;
    private double marketHealth = 100.0;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    private GameEngine() {}

    public static synchronized GameEngine getInstance() {
        if (instance == null) instance = new GameEngine();
        return instance;
    }

    // Thread-safe update for Market Health
    public synchronized void updateMarketHealth(double change) {
        this.marketHealth = Math.max(0, Math.min(200, marketHealth + change));
    }

    public synchronized double getMarketHealth() { return marketHealth; }

    public ObservableList<Player> getPlayers() { return players; }

    // Helper to find the "Current Player" (for demo purposes, index 0)
    public Player getCurrentUser() {
        return players.isEmpty() ? null : players.get(0);
    }
}