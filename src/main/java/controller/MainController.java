package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import model.Player;
import java.util.Arrays;
import java.util.List;

public class MainController {

    @FXML private Label marketHealthLabel;
    @FXML private TableView<Player> playersTable;
    @FXML private TableColumn<Player, String> playerNameColumn;
    @FXML private TableColumn<Player, String> businessTierColumn;
    @FXML private TableColumn<Player, Double> capitalColumn;
    @FXML private Button investBtn;
    @FXML private Button sabotageBtn;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        playerNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        businessTierColumn.setCellValueFactory(new PropertyValueFactory<>("businessTier"));
        capitalColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedCapital"));

        List<Player> samplePlayers = Arrays.asList(
                new Player(1, "IslandInnovator", "Tier1", 120000.0),
                new Player(2, "MetropolisMogul", "Tier2", 450000.0),
                new Player(3, "RuralRising", "Tier1", 85000.0),
                new Player(4, "TechTitanPHP", "Tier3", 1000000.0)
        );

        playersTable.getItems().addAll(samplePlayers);
        marketHealthLabel.setText("Global Market Health: 100%");
        statusLabel.setText("Game Status: Awaiting player actions...");
    }

    @FXML
    public void handleInvestAction(ActionEvent event) {
        statusLabel.setText("Invested in Community! (+5 Market Health, Slow Progress)");
        String currentHealthText = marketHealthLabel.getText();
        int health = Integer.parseInt(currentHealthText.replaceAll("[^0-9]", ""));
        int newHealth = Math.min(100, health + 5);
        marketHealthLabel.setText("Global Market Health: " + newHealth + "%");
    }

    @FXML
    public void handleSabotageAction(ActionEvent event) {
        statusLabel.setText("Sabotage Initiated! (-15 Market Health, Fast Progress, Choose Target Soon)");
        String currentHealthText = marketHealthLabel.getText();
        int health = Integer.parseInt(currentHealthText.replaceAll("[^0-9]", ""));
        int newHealth = Math.max(0, health - 15);
        marketHealthLabel.setText("Global Market Health: " + newHealth + "%");
    }
}