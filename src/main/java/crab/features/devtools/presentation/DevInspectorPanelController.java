package crab.features.devtools.presentation;

import crab.features.devtools.domain.DebugParameter;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.domain.SceneTreeNode;
import crab.features.devtools.properties.LightAdapter;
import crab.features.devtools.properties.MaterialAdapter;
import crab.features.devtools.properties.NodeTransformAdapter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
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
    private TextField treeSearchField;
    @FXML
    private TreeView<SceneTreeNode> sceneTreeView;
    @FXML
    private ComboBox<String> cameraPicker;
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
    private SceneTreeNode unfilteredSceneTree;
    private Consumer<SceneTreeNode> treeSelectionConsumer = item -> {
    };
    private Consumer<SceneTreeNode> visibilityConsumer = item -> {
    };
    private Consumer<String> cameraSelectionConsumer = item -> {
    };
    private Consumer<Inspectable3D> changeConsumer = item -> {
    };
    private boolean updatingTree;
    private boolean updatingCameraPicker;
    private NodeTransformAdapter transformAdapter;
    private Optional<MaterialAdapter> materialAdapter = Optional.empty();
    private Optional<LightAdapter> lightAdapter = Optional.empty();

    @FXML
    private void initialize() {
        sceneTreeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        sceneTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingTree && newValue != null) {
                treeSelectionConsumer.accept(newValue.getValue());
            }
        });
        treeSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTreeFilter());
        cameraPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingCameraPicker && newValue != null) {
                cameraSelectionConsumer.accept(newValue);
            }
        });
        installTransformScrubbers();
        clear();
    }

    public void setSceneTree(
            SceneTreeNode root,
            Consumer<SceneTreeNode> onSelect,
            Consumer<SceneTreeNode> onVisibilityChange
    ) {
        unfilteredSceneTree = root;
        treeSelectionConsumer = onSelect;
        visibilityConsumer = onVisibilityChange;
        refreshTreeFilter();
    }

    public void setCameraModes(List<String> modes, String selectedMode, Consumer<String> onSelect) {
        updatingCameraPicker = true;
        cameraSelectionConsumer = onSelect;
        cameraPicker.setItems(FXCollections.observableArrayList(modes));
        cameraPicker.setValue(selectedMode);
        updatingCameraPicker = false;
    }

    public void setChangeConsumer(Consumer<Inspectable3D> consumer) {
        changeConsumer = consumer;
    }

    public void inspect(Inspectable3D item) {
        selected = item;
        transformAdapter = new NodeTransformAdapter(item.target());
        materialAdapter = MaterialAdapter.forNode(item.target());
        lightAdapter = LightAdapter.forNode(item.target());

        nameLabel.setText(item.name());
        typeLabel.setText(item.target().getClass().getSimpleName());
        statusLabel.setText(item.persistent() ? "Auto-saved dev override" : "Runtime only");
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
        changeConsumer.accept(selected);
    }

    @FXML
    private void applyMaterialColors() {
        materialAdapter.ifPresent(adapter -> {
            adapter.setDiffuseColor(diffuseColorPicker.getValue());
            adapter.setSpecularColor(specularColorPicker.getValue());
            if (selected != null) {
                changeConsumer.accept(selected);
            }
        });
    }

    @FXML
    private void applyLightColor() {
        lightAdapter.ifPresent(adapter -> {
            adapter.setColor(lightColorPicker.getValue());
            if (selected != null) {
                changeConsumer.accept(selected);
            }
        });
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
        if (selected != null) {
            changeConsumer.accept(selected);
        }
    }

    private void refreshTreeFilter() {
        updatingTree = true;
        if (unfilteredSceneTree == null) {
            sceneTreeView.setRoot(null);
        } else {
            sceneTreeView.setRoot(unfilteredSceneTree.filter(treeSearchField.getText())
                    .map(this::toTreeItem)
                    .orElse(null));
            if (sceneTreeView.getRoot() != null) {
                sceneTreeView.getRoot().setExpanded(true);
            }
        }
        updatingTree = false;
    }

    private TreeItem<SceneTreeNode> toTreeItem(SceneTreeNode node) {
        CheckBoxTreeItem<SceneTreeNode> item = new CheckBoxTreeItem<>(node);
        item.setSelected(node.node().isVisible());
        item.selectedProperty().addListener((observable, oldValue, selectedValue) -> {
            if (node.node().isVisible() != selectedValue) {
                node.node().setVisible(selectedValue);
                visibilityConsumer.accept(node);
            }
        });
        node.node().visibleProperty().addListener((observable, oldValue, visible) -> item.setSelected(visible));
        for (SceneTreeNode child : node.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
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
