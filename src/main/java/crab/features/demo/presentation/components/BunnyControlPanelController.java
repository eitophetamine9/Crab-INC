package crab.features.demo.presentation.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.util.function.DoubleConsumer;

/**
 * Controller for the bunny-only control panel in the foundation demo.
 *
 * Design patterns:
 * - MVC Controller: mediates the FXML view state.
 *
 * SOLID:
 * - Single Responsibility: owns demo panel UI controls only.
 */
public final class BunnyControlPanelController {
    @FXML
    private Label statusLabel;
    @FXML
    private Label orientationTitleLabel;
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
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label waveSpeedValueLabel;
    @FXML
    private Slider waveSpeedSlider;
    @FXML
    private Label waveHeightValueLabel;
    @FXML
    private Slider waveHeightSlider;
    @FXML
    private Label foamValueLabel;
    @FXML
    private Slider foamSlider;
    @FXML
    private Label shoreValueLabel;
    @FXML
    private Slider shoreSlider;
    @FXML
    private Label chromaticValueLabel;
    @FXML
    private Slider chromaticSlider;

    private DoubleConsumer yawConsumer = value -> {
    };
    private DoubleConsumer pitchConsumer = value -> {
    };
    private DoubleConsumer rollConsumer = value -> {
    };
    private DoubleConsumer toonParameterConsumer = value -> {
    };
    private DoubleConsumer waveSpeedConsumer = value -> {
    };
    private DoubleConsumer waveHeightConsumer = value -> {
    };
    private DoubleConsumer foamConsumer = value -> {
    };
    private DoubleConsumer shoreConsumer = value -> {
    };
    private DoubleConsumer chromaticConsumer = value -> {
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
        waveSpeedSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyWaveSpeed(newValue.doubleValue()));
        waveHeightSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyWaveHeight(newValue.doubleValue()));
        foamSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyFoam(newValue.doubleValue()));
        shoreSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyShore(newValue.doubleValue()));
        chromaticSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                applyChromatic(newValue.doubleValue()));
        applyYaw(yawSlider.getValue());
        applyPitch(pitchSlider.getValue());
        applyRoll(rollSlider.getValue());
        applyToonParameter(toonSlider.getValue());
        applyWaveSpeed(waveSpeedSlider.getValue());
        applyWaveHeight(waveHeightSlider.getValue());
        applyFoam(foamSlider.getValue());
        applyShore(shoreSlider.getValue());
        applyChromatic(chromaticSlider.getValue());
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void setOrientationTitle(String text) {
        orientationTitleLabel.setText(text);
    }

    public void setDescriptionText(String text) {
        descriptionLabel.setText(text);
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

    public void setWaveSpeedConsumer(DoubleConsumer consumer) {
        waveSpeedConsumer = consumer;
        applyWaveSpeed(waveSpeedSlider.getValue());
    }

    public void setWaveHeightConsumer(DoubleConsumer consumer) {
        waveHeightConsumer = consumer;
        applyWaveHeight(waveHeightSlider.getValue());
    }

    public void setFoamConsumer(DoubleConsumer consumer) {
        foamConsumer = consumer;
        applyFoam(foamSlider.getValue());
    }

    public void setShoreConsumer(DoubleConsumer consumer) {
        shoreConsumer = consumer;
        applyShore(shoreSlider.getValue());
    }

    public void setChromaticConsumer(DoubleConsumer consumer) {
        chromaticConsumer = consumer;
        applyChromatic(chromaticSlider.getValue());
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

    private void applyWaveSpeed(double value) {
        waveSpeedValueLabel.setText(String.format("%.2fx", value));
        waveSpeedConsumer.accept(value);
    }

    private void applyWaveHeight(double value) {
        waveHeightValueLabel.setText(String.format("%.2f", value));
        waveHeightConsumer.accept(value);
    }

    private void applyFoam(double value) {
        foamValueLabel.setText(String.format("%.2f", value));
        foamConsumer.accept(value);
    }

    private void applyShore(double value) {
        shoreValueLabel.setText(String.format("%.2f", value));
        shoreConsumer.accept(value);
    }

    private void applyChromatic(double value) {
        chromaticValueLabel.setText(String.format("%.2f", value));
        chromaticConsumer.accept(value);
    }

    private String formatAngle(double value) {
        return String.format("%.0f°", value);
    }
}
