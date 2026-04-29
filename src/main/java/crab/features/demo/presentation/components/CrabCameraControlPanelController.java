package crab.features.demo.presentation.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.util.function.DoubleConsumer;

/**
 * Controller for the crab demo camera controls.
 */
public final class CrabCameraControlPanelController {
    @FXML
    private Label xValueLabel;
    @FXML
    private Slider xSlider;
    @FXML
    private Label yValueLabel;
    @FXML
    private Slider ySlider;
    @FXML
    private Label zValueLabel;
    @FXML
    private Slider zSlider;
    @FXML
    private Label pitchValueLabel;
    @FXML
    private Slider pitchSlider;
    @FXML
    private Label yawValueLabel;
    @FXML
    private Slider yawSlider;
    @FXML
    private Label battlefieldScaleValueLabel;
    @FXML
    private Slider battlefieldScaleSlider;
    @FXML
    private Label battlefieldAmbientValueLabel;
    @FXML
    private Slider battlefieldAmbientSlider;

    private DoubleConsumer xConsumer = value -> {
    };
    private DoubleConsumer yConsumer = value -> {
    };
    private DoubleConsumer zConsumer = value -> {
    };
    private DoubleConsumer pitchConsumer = value -> {
    };
    private DoubleConsumer yawConsumer = value -> {
    };
    private DoubleConsumer battlefieldScaleConsumer = value -> {
    };
    private DoubleConsumer battlefieldAmbientConsumer = value -> {
    };

    @FXML
    private void initialize() {
        xSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyX(newValue.doubleValue()));
        ySlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyY(newValue.doubleValue()));
        zSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyZ(newValue.doubleValue()));
        pitchSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyPitch(newValue.doubleValue()));
        yawSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyYaw(newValue.doubleValue()));
        battlefieldScaleSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyBattlefieldScale(newValue.doubleValue()));
        battlefieldAmbientSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyBattlefieldAmbient(newValue.doubleValue()));

        applyX(xSlider.getValue());
        applyY(ySlider.getValue());
        applyZ(zSlider.getValue());
        applyPitch(pitchSlider.getValue());
        applyYaw(yawSlider.getValue());
        applyBattlefieldScale(battlefieldScaleSlider.getValue());
        applyBattlefieldAmbient(battlefieldAmbientSlider.getValue());
    }

    public void setXConsumer(DoubleConsumer consumer) {
        xConsumer = consumer;
        applyX(xSlider.getValue());
    }

    public void setYConsumer(DoubleConsumer consumer) {
        yConsumer = consumer;
        applyY(ySlider.getValue());
    }

    public void setZConsumer(DoubleConsumer consumer) {
        zConsumer = consumer;
        applyZ(zSlider.getValue());
    }

    public void setPitchConsumer(DoubleConsumer consumer) {
        pitchConsumer = consumer;
        applyPitch(pitchSlider.getValue());
    }

    public void setYawConsumer(DoubleConsumer consumer) {
        yawConsumer = consumer;
        applyYaw(yawSlider.getValue());
    }

    public void setBattlefieldScaleConsumer(DoubleConsumer consumer) {
        battlefieldScaleConsumer = consumer;
        applyBattlefieldScale(battlefieldScaleSlider.getValue());
    }

    public void setBattlefieldAmbientConsumer(DoubleConsumer consumer) {
        battlefieldAmbientConsumer = consumer;
        applyBattlefieldAmbient(battlefieldAmbientSlider.getValue());
    }

    public void setCameraX(double value) {
        xSlider.setValue(value);
    }

    public void setCameraY(double value) {
        ySlider.setValue(value);
    }

    public void setCameraZ(double value) {
        zSlider.setValue(value);
    }

    public void setPitch(double value) {
        pitchSlider.setValue(value);
    }

    public void setYaw(double value) {
        yawSlider.setValue(value);
    }

    public void setBattlefieldScale(double value) {
        battlefieldScaleSlider.setValue(value);
    }

    public void setBattlefieldAmbient(double value) {
        battlefieldAmbientSlider.setValue(value);
    }

    private void applyX(double value) {
        xValueLabel.setText(formatDistance(value));
        xConsumer.accept(value);
    }

    private void applyY(double value) {
        yValueLabel.setText(formatDistance(value));
        yConsumer.accept(value);
    }

    private void applyZ(double value) {
        zValueLabel.setText(formatDistance(value));
        zConsumer.accept(value);
    }

    private void applyPitch(double value) {
        pitchValueLabel.setText(formatAngle(value));
        pitchConsumer.accept(value);
    }

    private void applyYaw(double value) {
        yawValueLabel.setText(formatAngle(value));
        yawConsumer.accept(value);
    }

    private void applyBattlefieldScale(double value) {
        battlefieldScaleValueLabel.setText(String.format("%.2fx", value));
        battlefieldScaleConsumer.accept(value);
    }

    private void applyBattlefieldAmbient(double value) {
        battlefieldAmbientValueLabel.setText(String.format("%.2f", value));
        battlefieldAmbientConsumer.accept(value);
    }

    private static String formatDistance(double value) {
        return String.format("%.0f", value);
    }

    private static String formatAngle(double value) {
        return String.format("%.0f°", value);
    }
}
