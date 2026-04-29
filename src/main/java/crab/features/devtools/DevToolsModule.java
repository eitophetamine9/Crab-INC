package crab.features.devtools;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.domain.Inspectable3DRegistry;
import crab.features.devtools.domain.Inspectable3DSelection;
import crab.features.devtools.interaction.DragPlaneProjector;
import crab.features.devtools.presentation.DevInspectorPanelController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.onKeyDown;

/**
 * Development-only 3D selection, inspection, and transform gizmo tooling.
 */
public final class DevToolsModule implements GameModule {
    private static final double DRAG_SCALE = 1.0;

    private final Inspectable3DRegistry registry = new Inspectable3DRegistry();
    private final Inspectable3DSelection selection = new Inspectable3DSelection();
    private final Set<SubScene> attachedSubScenes = new HashSet<>();
    private final Group gizmoOverlay = new Group();
    private Parent inspectorPanel;
    private DevInspectorPanelController inspectorController;
    private boolean enabled;
    private boolean toggleBound;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;

    @Override
    public void initialize(GameContext context) {
        context.register(DevToolsModule.class, this);
    }

    @Override
    public void start() {
        bindToggleOnce();
    }

    @Override
    public void initializeUi() {
        inspectorPanel = loadInspectorPanel();
        inspectorPanel.setTranslateX(690);
        inspectorPanel.setTranslateY(36);
        gizmoOverlay.setMouseTransparent(true);
    }

    @Override
    public void update(double tpf) {
        if (enabled) {
            selection.selected().ifPresentOrElse(this::updateGizmoFor, this::clearGizmo);
        }
    }

    @Override
    public void stop() {
        setEnabled(false);
        registry.items().forEach(item -> registry.clearScope(item.screenId()));
        attachedSubScenes.clear();
    }

    public Inspectable3DRegistry registry() {
        return registry;
    }

    public void registerInspectable(Inspectable3D item) {
        registry.register(item);
        refreshInspectablePicker();
    }

    public void clearScope(String screenId) {
        registry.clearScope(screenId);
        selection.selected()
                .filter(item -> item.screenId().equals(screenId))
                .ifPresent(item -> clearSelection());
        refreshInspectablePicker();
    }

    public void attachSubScene(String screenId, SubScene subScene, double dragPlaneY) {
        if (!attachedSubScenes.add(subScene)) {
            return;
        }

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleClick(event));
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handlePress(event));
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> handleDrag(event));
        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> dragging = false);
    }

    private Parent loadInspectorPanel() {
        URL resource = getClass().getResource("/fxml/devtools/dev-inspector-panel.fxml");
        if (resource == null) {
            return new Label("Missing /fxml/devtools/dev-inspector-panel.fxml");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            inspectorController = loader.getController();
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Dev inspector load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    private void bindToggleOnce() {
        if (toggleBound) {
            return;
        }

        toggleBound = true;
        onKeyDown(KeyCode.F9, () -> setEnabled(!enabled));
    }

    private void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }

        enabled = value;
        if (enabled) {
            refreshInspectablePicker();
            getGameScene().addUINode(inspectorPanel);
            getGameScene().addUINode(gizmoOverlay);
        } else {
            getGameScene().removeUINode(inspectorPanel);
            getGameScene().removeUINode(gizmoOverlay);
            dragging = false;
            clearSelection();
        }
    }

    private void handleClick(MouseEvent event) {
        if (!enabled) {
            return;
        }

        Node pickedNode = event.getPickResult().getIntersectedNode();
        if (pickedNode == null) {
            clearSelection();
            return;
        }

        Optional<Inspectable3D> item = registry.findForNode(pickedNode);
        item.ifPresentOrElse(this::select, this::clearSelection);
        event.consume();
    }

    private void handlePress(MouseEvent event) {
        if (!enabled) {
            return;
        }

        Node pickedNode = event.getPickResult().getIntersectedNode();
        if (pickedNode == null) {
            dragging = false;
            return;
        }

        registry.findForNode(pickedNode).ifPresent(item -> {
            select(item);
            dragging = true;
            lastDragX = event.getSceneX();
            lastDragY = event.getSceneY();
            event.consume();
        });
    }

    private void handleDrag(MouseEvent event) {
        if (!enabled || !dragging) {
            return;
        }

        selection.selected().ifPresent(item -> {
            double deltaX = (event.getSceneX() - lastDragX) * DRAG_SCALE;
            double deltaZ = (event.getSceneY() - lastDragY) * DRAG_SCALE;
            DragPlaneProjector.applyPlaneDelta(item.target(), new Point3D(deltaX, 0, deltaZ));
            lastDragX = event.getSceneX();
            lastDragY = event.getSceneY();
            if (inspectorController != null) {
                inspectorController.inspect(item);
            }
            event.consume();
        });
    }

    private void select(Inspectable3D item) {
        selection.select(item);
        if (inspectorController != null) {
            inspectorController.inspect(item);
        }
        updateGizmoFor(item);
    }

    private void clearSelection() {
        selection.clear();
        clearGizmo();
        if (inspectorController != null) {
            inspectorController.clear();
        }
    }

    private void refreshInspectablePicker() {
        if (inspectorController != null) {
            inspectorController.setInspectableItems(registry.items(), this::select);
        }
    }

    private void updateGizmoFor(Inspectable3D item) {
        Bounds bounds = item.target().localToScene(item.target().getBoundsInLocal());
        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
        double width = Math.max(18, bounds.getWidth());
        double height = Math.max(18, bounds.getHeight());

        Rectangle outline = new Rectangle(bounds.getMinX(), bounds.getMinY(), width, height);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.rgb(250, 204, 21));
        outline.setStrokeWidth(2);

        Line xAxis = axisLine(centerX, centerY, centerX + 64, centerY, Color.rgb(239, 68, 68));
        Line yAxis = axisLine(centerX, centerY, centerX, centerY - 64, Color.rgb(34, 197, 94));
        Line zAxis = axisLine(centerX, centerY, centerX + 44, centerY + 44, Color.rgb(59, 130, 246));

        gizmoOverlay.getChildren().setAll(outline, xAxis, yAxis, zAxis);
    }

    private void clearGizmo() {
        gizmoOverlay.getChildren().clear();
    }

    private static Line axisLine(double startX, double startY, double endX, double endY, Color color) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(4);
        return line;
    }
}
