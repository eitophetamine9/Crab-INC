package controller;

import app.MarketSimulationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.GameEngine;
import model.Player;

public class MainController {
    @FXML private Label marketHealthLabel;
    @FXML private TableView<Player> playersTable;
    @FXML private TableColumn<Player, String> nameCol;
    @FXML private TableColumn<Player, Double> capitalCol;
    @FXML private TableColumn<Player, Integer> pointsCol;
    @FXML private Label statusLabel;

    private final GameEngine engine = GameEngine.getInstance();

    @FXML
    public void initialize() {
        // Mapping columns to Player properties
        nameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        capitalCol.setCellValueFactory(new PropertyValueFactory<>("estimatedCapital"));
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("victoryPoints"));

        // Setup Sample Data
        engine.getPlayers().add(new Player(1, "You (CEO)", "Tier 1", 50000));
        engine.getPlayers().add(new Player(2, "Rival_Crab", "Tier 1", 50000));
        playersTable.setItems(engine.getPlayers());

        // Start Multithreading Service
        MarketSimulationService service = new MarketSimulationService();
        service.setOnSucceeded(e -> marketHealthLabel.setText(
                String.format("Global Market Health: %.1f%%", engine.getMarketHealth())));
        service.start();
    }

    @FXML
    public void handleInvest() {
        Player user = engine.getCurrentUser();
        if (user != null) {
            user.addVictoryPoints(50); // Concept: +50 VP
            engine.updateMarketHealth(5.0); // Concept: Increases health
            statusLabel.setText("Status: Bayanihan! +50 VP. Market is recovering.");
        }
    }

    @FXML
    public void handleSabotage() {
        Player user = engine.getCurrentUser();
        if (user != null) {
            user.addVictoryPoints(150); // Concept: +150 VP
            engine.updateMarketHealth(-15.0); // Concept: Damages health
            statusLabel.setText("Status: Crab Mentality! +150 VP. Market is crashing!");
        }
    }
}