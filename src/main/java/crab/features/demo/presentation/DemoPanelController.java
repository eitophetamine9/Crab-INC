package crab.features.demo.presentation;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.util.function.DoubleConsumer;

/**
 * Controller for the FXML sample panel in the foundation demo.
 *
 * Design patterns:
 * - MVC Controller: mediates the FXML view state.
 *
 * SOLID:
 * - Single Responsibility: owns demo panel UI controls only.
 */
public final class DemoPanelController {
    @FXML
    private Label statusLabel;
    @FXML
    private Label yawValueLabel;
    @FXML
    private Slider yawSlider;
    @FXML
    private Label pitchValueLabel;
    @FXML
    private Slider pitchSlider;
    @FXML
    private Label rollValueLabel;
    @FXML
    private Slider rollSlider;
    @FXML
    private Label toonValueLabel;
    @FXML
    private Slider toonSlider;

    private DoubleConsumer yawConsumer = value -> {
    };
    private DoubleConsumer pitchConsumer = value -> {
    };
    private DoubleConsumer rollConsumer = value -> {
    };
    private DoubleConsumer toonParameterConsumer = value -> {
    };

    @FXML
    private void initialize() {
        yawSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyYaw(newValue.doubleValue()));
        pitchSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyPitch(newValue.doubleValue()));
        rollSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyRoll(newValue.doubleValue()));
        toonSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyToonParameter(newValue.doubleValue()));
        applyYaw(yawSlider.getValue());
        applyPitch(pitchSlider.getValue());
        applyRoll(rollSlider.getValue());
        applyToonParameter(toonSlider.getValue());
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void setYawConsumer(DoubleConsumer consumer) {
        yawConsumer = consumer;
        applyYaw(yawSlider.getValue());
    }

    public void setPitchConsumer(DoubleConsumer consumer) {
        pitchConsumer = consumer;
        applyPitch(pitchSlider.getValue());
    }

    public void setRollConsumer(DoubleConsumer consumer) {
        rollConsumer = consumer;
        applyRoll(rollSlider.getValue());
    }

    public void setToonParameterConsumer(DoubleConsumer consumer) {
        toonParameterConsumer = consumer;
        applyToonParameter(toonSlider.getValue());
    }

    private void applyYaw(double value) {
        yawValueLabel.setText(formatAngle(value));
        yawConsumer.accept(value);
    }

    private void applyPitch(double value) {
        pitchValueLabel.setText(formatAngle(value));
        pitchConsumer.accept(value);
    }

    private void applyRoll(double value) {
        rollValueLabel.setText(formatAngle(value));
        rollConsumer.accept(value);
    }

    private void applyToonParameter(double value) {
        toonValueLabel.setText(String.format("%.2f", value));
        toonParameterConsumer.accept(value);
    }

    private String formatAngle(double value) {
        return String.format("%.0f°", value);
    }
}
