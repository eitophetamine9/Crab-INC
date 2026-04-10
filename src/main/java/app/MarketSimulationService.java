package app;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import model.GameEngine;
import model.Player;

public class MarketSimulationService extends ScheduledService<Double> {
    public MarketSimulationService() {
        setPeriod(Duration.seconds(3)); // Updates every 3 seconds
    }

    @Override
    protected Task<Double> createTask() {
        return new Task<>() {
            @Override
            protected Double call() {
                GameEngine engine = GameEngine.getInstance();
                double healthFactor = engine.getMarketHealth() / 100.0;

                // Simulate passive income for all players based on market health
                for (Player p : engine.getPlayers()) {
                    double income = 1000 * healthFactor;
                    p.addCapital(income);
                }
                return engine.getMarketHealth();
            }
        };
    }
}