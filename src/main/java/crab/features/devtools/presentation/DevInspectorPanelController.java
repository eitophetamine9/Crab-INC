package crab.features.devtools.presentation;

import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.properties.LightAdapter;
import crab.features.devtools.properties.MaterialAdapter;
import crab.features.devtools.properties.NodeTransformAdapter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class DevInspectorPanelController {
    @FXML
    private Label nameLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<Inspectable3D> objectPicker;
    @FXML
    private TextField positionXField;
    @FXML
    private TextField positionYField;
    @FXML
    private TextField positionZField;
    @FXML
    private TextField rotateXField;
    @FXML
    private TextField rotateYField;
    @FXML
    private TextField rotateZField;
    @FXML
    private TextField scaleXField;
    @FXML
    private TextField scaleYField;
    @FXML
    private TextField scaleZField;
    @FXML
    private VBox debugParametersSection;
    @FXML
    private VBox debugParameterRows;
    @FXML
    private VBox materialSection;
    @FXML
    private ColorPicker diffuseColorPicker;
    @FXML
    private ColorPicker specularColorPicker;
    @FXML
    private VBox lightSection;
    @FXML
    private ColorPicker lightColorPicker;

    private Inspectable3D selected;
    private Consumer<Inspectable3D> selectionConsumer = item -> {
    };
    private boolean updatingPicker;
    private NodeTransformAdapter transformAdapter;
    private Optional<MaterialAdapter> materialAdapter = Optional.empty();
    private Optional<LightAdapter> lightAdapter = Optional.empty();

    @FXML
    private void initialize() {
        objectPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingPicker && newValue != null) {
                selectionConsumer.accept(newValue);
            }
        });
        installTransformScrubbers();
        clear();
    }

    public void setInspectableItems(List<Inspectable3D> items, Consumer<Inspectable3D> onSelect) {
        updatingPicker = true;
        selectionConsumer = onSelect;
        objectPicker.setItems(FXCollections.observableArrayList(items));
        if (selected != null && items.contains(selected)) {
            objectPicker.setValue(selected);
        } else {
            objectPicker.setValue(null);
        }
        updatingPicker = false;
    }

    public void inspect(Inspectable3D item) {
        selected = item;
        updatingPicker = true;
        objectPicker.setValue(item);
        updatingPicker = false;
        transformAdapter = new NodeTransformAdapter(item.target());
        materialAdapter = MaterialAdapter.forNode(item.target());
        lightAdapter = LightAdapter.forNode(item.target());

        nameLabel.setText(item.name());
        typeLabel.setText(item.target().getClass().getSimpleName());
        statusLabel.setText("Runtime only");
        materialSection.setVisible(materialAdapter.isPresent());
        materialSection.setManaged(materialAdapter.isPresent());
        lightSection.setVisible(lightAdapter.isPresent());
        lightSection.setManaged(lightAdapter.isPresent());
        populateDebugParameters(item.debugParameters());

        NodeTransformAdapter.TransformSnapshot snapshot = transformAdapter.snapshot();
        setText(positionXField, snapshot.x());
        setText(positionYField, snapshot.y());
        setText(positionZField, snapshot.z());
        setText(rotateXField, snapshot.rotateX());
        setText(rotateYField, snapshot.rotateY());
        setText(rotateZField, snapshot.rotateZ());
        setText(scaleXField, snapshot.scaleX());
        setText(scaleYField, snapshot.scaleY());
        setText(scaleZField, snapshot.scaleZ());

        materialAdapter.ifPresent(adapter -> {
            diffuseColorPicker.setValue(adapter.diffuseColor());
            specularColorPicker.setValue(adapter.specularColor());
        });
        lightAdapter.ifPresent(adapter -> lightColorPicker.setValue(adapter.color()));
    }

    public void clear() {
        selected = null;
        updatingPicker = true;
        objectPicker.setValue(null);
        updatingPicker = false;
        transformAdapter = null;
        materialAdapter = Optional.empty();
        lightAdapter = Optional.empty();
        nameLabel.setText("No 3D object selected");
        typeLabel.setText("Click a registered 3D object");
        statusLabel.setText("Runtime only");
        materialSection.setVisible(false);
        materialSection.setManaged(false);
        lightSection.setVisible(false);
        lightSection.setManaged(false);
        debugParameterRows.getChildren().clear();
        debugParametersSection.setVisible(false);
        debugParametersSection.setManaged(false);
    }

    @FXML
    private void applyTransform() {
        if (selected == null || transformAdapter == null) {
            return;
        }

        transformAdapter.setPosition(
                parse(positionXField, selected.target().getTranslateX()),
                parse(positionYField, selected.target().getTranslateY()),
                parse(positionZField, selected.target().getTranslateZ())
        );
        NodeTransformAdapter.TransformSnapshot snapshot = transformAdapter.snapshot();
        transformAdapter.setRotation(
                parse(rotateXField, snapshot.rotateX()),
                parse(rotateYField, snapshot.rotateY()),
                parse(rotateZField, snapshot.rotateZ())
        );
        transformAdapter.setScale(
                parse(scaleXField, selected.target().getScaleX()),
                parse(scaleYField, selected.target().getScaleY()),
                parse(scaleZField, selected.target().getScaleZ())
        );
    }

    @FXML
    private void applyMaterialColors() {
        materialAdapter.ifPresent(adapter -> {
            adapter.setDiffuseColor(diffuseColorPicker.getValue());
            adapter.setSpecularColor(specularColorPicker.getValue());
        });
    }

    @FXML
    private void applyLightColor() {
        lightAdapter.ifPresent(adapter -> adapter.setColor(lightColorPicker.getValue()));
    }

    private void installTransformScrubbers() {
        installScrubber(positionXField, 0.5, this::applyTransform);
        installScrubber(positionYField, 0.5, this::applyTransform);
        installScrubber(positionZField, 0.5, this::applyTransform);
        installScrubber(rotateXField, 0.1, this::applyTransform);
        installScrubber(rotateYField, 0.1, this::applyTransform);
        installScrubber(rotateZField, 0.1, this::applyTransform);
        installScrubber(scaleXField, 0.01, this::applyTransform);
        installScrubber(scaleYField, 0.01, this::applyTransform);
        installScrubber(scaleZField, 0.01, this::applyTransform);
    }

    private void populateDebugParameters(List<DebugParameter> parameters) {
        debugParameterRows.getChildren().clear();
        boolean hasParameters = !parameters.isEmpty();
        debugParametersSection.setVisible(hasParameters);
        debugParametersSection.setManaged(hasParameters);

        for (DebugParameter parameter : parameters) {
            Label label = new Label(parameter.name());
            label.setMinWidth(116);
            label.setStyle("-fx-text-fill: #cbd5e1;");

            TextField field = new TextField();
            field.setPrefWidth(92);
            field.setTooltip(new Tooltip(parameter.group() + " / " + parameter.id()));
            setText(field, parameter.value());

            Runnable apply = () -> applyDebugParameter(parameter, field);
            field.setOnAction(event -> apply.run());
            field.focusedProperty().addListener((observable, oldValue, focused) -> {
                if (!focused) {
                    apply.run();
                }
            });
            installScrubber(field, parameter.step(), apply);

            HBox row = new HBox(8, label, field);
            row.setFillHeight(false);
            debugParameterRows.getChildren().add(row);
        }
    }

    private void applyDebugParameter(DebugParameter parameter, TextField field) {
        parameter.setValue(parse(field, parameter.value()));
        setText(field, parameter.value());
    }

    private void installScrubber(TextField field, double step, Runnable onChange) {
        double[] startX = new double[1];
        double[] startValue = new double[1];
        field.setCursor(Cursor.H_RESIZE);
        field.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            startX[0] = event.getScreenX();
            startValue[0] = parse(field, 0);
        });
        field.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            double value = ScrubbableNumber.valueForDrag(startValue[0], event.getScreenX() - startX[0], step);
            setText(field, value);
            onChange.run();
            event.consume();
        });
    }

    private static void setText(TextField field, double value) {
        field.setText(String.format("%.2f", value));
    }

    private static double parse(TextField field, double fallback) {
        try {
            double value = Double.parseDouble(field.getText());
            if (!Double.isFinite(value)) {
                throw new NumberFormatException("Non-finite value");
            }
            return value;
        } catch (NumberFormatException exception) {
            setText(field, fallback);
            return fallback;
        }
    }
}
