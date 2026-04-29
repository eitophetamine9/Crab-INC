package crab.features.devtools;

import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.features.devtools.camera.DebugCameraController;
import crab.features.devtools.domain.DebugParameterGroup;
import crab.features.devtools.domain.DebugParameterSource;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.domain.Inspectable3DRegistry;
import crab.features.devtools.domain.Inspectable3DSelection;
import crab.features.devtools.domain.SceneTreeBuilder;
import crab.features.devtools.domain.SceneTreeNode;
import crab.features.devtools.input.DevModeInputGate;
import crab.features.devtools.input.DevMouseInteractionPolicy;
import crab.features.devtools.input.DevMousePressAction;
import crab.features.devtools.interaction.DragPlaneProjector;
import crab.features.devtools.persistence.DebugSceneOverrideStore;
import crab.features.devtools.presentation.DevInspectorPanelController;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.getGameScene;
import static com.almasb.fxgl.dsl.FXGL.getSettings;
import static com.almasb.fxgl.dsl.FXGL.onKeyDown;

/**
 * Development-only 3D selection, inspection, and transform gizmo tooling.
 */
public final class DevToolsModule implements GameModule {
    private static final double DRAG_SCALE = 1.0;
    private static final String SCENE_CAMERA = "Scene Camera";
    private static final String DEV_CAMERA = "Dev Camera";

    private final Inspectable3DRegistry registry = new Inspectable3DRegistry();
    private final Inspectable3DSelection selection = new Inspectable3DSelection();
    private final Set<SubScene> attachedSubScenes = new HashSet<>();
    private final SceneTreeBuilder sceneTreeBuilder = new SceneTreeBuilder(registry);
    private final DebugSceneOverrideStore overrideStore = new DebugSceneOverrideStore(Path.of(".crabinc", "devtools"));
    private final Map<String, Inspectable3D> pendingSaves = new LinkedHashMap<>();
    private final Set<KeyCode> activeDevCameraKeys = EnumSet.noneOf(KeyCode.class);
    private final DevMouseInteractionPolicy mousePolicy = new DevMouseInteractionPolicy();
    private final Group gizmoOverlay = new Group();
    private Parent inspectorPanel;
    private Stage inspectorWindow;
    private DevInspectorPanelController inspectorController;
    private PauseTransition saveDebounce;
    private DevModeInputGate inputGate;
    private String activeScreenId;
    private SubScene activeSubScene;
    private DebugCameraController activeDebugCamera;
    private boolean enabled;
    private boolean toggleBound;
    private boolean inputGateBound;
    private boolean dragging;
    private boolean looking;
    private double lastDragX;
    private double lastDragY;
    private double lastLookX;
    private double lastLookY;

    @Override
    public void initialize(GameContext context) {
        context.register(DevToolsModule.class, this);
    }

    @Override
    public void start() {
        bindToggleOnce();
        bindInputGateOnce();
    }

    @Override
    public void initializeUi() {
        inspectorPanel = loadInspectorPanel();
        gizmoOverlay.setMouseTransparent(true);
        saveDebounce = new PauseTransition(Duration.millis(250));
        saveDebounce.setOnFinished(event -> flushPendingSaves());
    }

    @Override
    public void update(double tpf) {
        if (enabled) {
            updateDevCamera(tpf);
            selection.selected().ifPresentOrElse(this::updateGizmoFor, this::clearGizmo);
        }
    }

    @Override
    public void stop() {
        setEnabled(false);
        closeInspectorWindow();
        flushPendingSaves();
        registry.items().forEach(item -> registry.clearScope(item.screenId()));
        attachedSubScenes.clear();
    }

    public Inspectable3DRegistry registry() {
        return registry;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void registerInspectable(Inspectable3D item) {
        overrideStore.applyOverrides(item);
        registry.register(item);
        refreshSceneTree();
    }

    public void registerParameterSource(
            String screenId,
            String stableId,
            String displayName,
            DebugParameterSource source
    ) {
        registry.items().stream()
                .filter(item -> item.screenId().equals(screenId) && item.id().equals(stableId))
                .findFirst()
                .ifPresent(item -> {
                    List<DebugParameterGroup> groups = new ArrayList<>(item.debugParameterGroups());
                    groups.addAll(source.parameterGroups());
                    registerInspectable(Inspectable3D.forNodeWithGroups(item.id(), displayName, screenId, item.target(), groups));
                });
    }

    public void clearScope(String screenId) {
        registry.clearScope(screenId);
        selection.selected()
                .filter(item -> item.screenId().equals(screenId))
                .ifPresent(item -> clearSelection());
        if (screenId.equals(activeScreenId)) {
            activeScreenId = null;
            activeSubScene = null;
            activeDebugCamera = null;
        }
        refreshSceneTree();
    }

    public void attachSubScene(String screenId, SubScene subScene, double dragPlaneY) {
        activeScreenId = screenId;
        activeSubScene = subScene;
        activeDebugCamera = new DebugCameraController(subScene);
        registerCameraInspectables();
        subScene.setFocusTraversable(true);
        refreshSceneTree();
        refreshCameraPicker();
        if (!attachedSubScenes.add(subScene)) {
            return;
        }

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleClick(event));
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handlePress(event));
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> handleDrag(event));
        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            dragging = false;
            looking = false;
        });
        subScene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        subScene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
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
            inspectorController.setChangeConsumer(this::saveInspectable);
            return root;
        } catch (IOException exception) {
            Label fallback = new Label("Dev inspector load failed: " + exception.getMessage());
            fallback.setTextFill(Color.WHITE);
            return fallback;
        }
    }

    private Stage createInspectorWindow(Parent root) {
        Stage stage = new Stage();
        stage.setTitle("Crab Inc. 3D Debugger");
        stage.setScene(new Scene(root));
        stage.setMinWidth(680);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(event -> {
            event.consume();
            setEnabled(false);
        });
        return stage;
    }

    private void showInspectorWindow() {
        if (Platform.isFxApplicationThread()) {
            showInspectorWindowOnFxThread();
        } else {
            Platform.runLater(this::showInspectorWindowOnFxThread);
        }
    }

    private void showInspectorWindowOnFxThread() {
        if (!enabled || inspectorPanel == null) {
            return;
        }

        if (inspectorWindow == null) {
            inspectorWindow = createInspectorWindow(inspectorPanel);
        }

        inspectorWindow.show();
        inspectorWindow.toFront();
    }

    private void hideInspectorWindow() {
        if (Platform.isFxApplicationThread()) {
            hideInspectorWindowOnFxThread();
        } else {
            Platform.runLater(this::hideInspectorWindowOnFxThread);
        }
    }

    private void hideInspectorWindowOnFxThread() {
        if (inspectorWindow != null) {
            inspectorWindow.hide();
        }
    }

    private void closeInspectorWindow() {
        if (Platform.isFxApplicationThread()) {
            closeInspectorWindowOnFxThread();
        } else {
            Platform.runLater(this::closeInspectorWindowOnFxThread);
        }
    }

    private void closeInspectorWindowOnFxThread() {
        if (inspectorWindow != null) {
            inspectorWindow.close();
            inspectorWindow = null;
        }
    }

    private void bindToggleOnce() {
        if (toggleBound) {
            return;
        }

        toggleBound = true;
        onKeyDown(KeyCode.F9, () -> setEnabled(!enabled));
    }

    private void bindInputGateOnce() {
        if (inputGateBound) {
            return;
        }

        inputGateBound = true;
        inputGate = new DevModeInputGate(this::isEnabled, () -> getSettings().getMenuKey());
        getGameScene().getInput().addEventFilter(KeyEvent.ANY, inputGate);
    }

    private void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }

        enabled = value;
        if (enabled) {
            refreshSceneTree();
            refreshCameraPicker();
            showInspectorWindow();
            getGameScene().addUINode(gizmoOverlay);
            if (activeSubScene != null) {
                activeSubScene.requestFocus();
            }
        } else {
            hideInspectorWindow();
            getGameScene().removeUINode(gizmoOverlay);
            restoreSceneCamera();
            dragging = false;
            looking = false;
            activeDevCameraKeys.clear();
            clearSelection();
        }
    }

    private void handleClick(MouseEvent event) {
        if (!enabled) {
            return;
        }

        activeSubScene = (SubScene) event.getSource();
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

        activeSubScene = (SubScene) event.getSource();
        Node pickedNode = event.getPickResult().getIntersectedNode();
        Optional<Inspectable3D> pickedItem = pickedNode == null ? Optional.empty() : registry.findForNode(pickedNode);
        DevMousePressAction action = mousePolicy.pressAction(isDevCameraActive(), pickedItem.isPresent(), event.getButton());

        if (action == DevMousePressAction.CAMERA_LOOK) {
            looking = true;
            dragging = false;
            lastLookX = event.getSceneX();
            lastLookY = event.getSceneY();
            event.consume();
            return;
        }

        if (action != DevMousePressAction.OBJECT_DRAG) {
            dragging = false;
            return;
        }

        pickedItem.ifPresent(item -> {
            select(item);
            dragging = true;
            looking = false;
            lastDragX = event.getSceneX();
            lastDragY = event.getSceneY();
            event.consume();
        });
    }

    private void handleDrag(MouseEvent event) {
        if (!enabled) {
            return;
        }

        if (looking && isDevCameraActive()) {
            activeDebugCamera.look(event.getSceneX() - lastLookX, event.getSceneY() - lastLookY);
            lastLookX = event.getSceneX();
            lastLookY = event.getSceneY();
            saveDevCameraState();
            event.consume();
            return;
        }

        if (!dragging) {
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
            saveInspectable(item);
            event.consume();
        });
    }

    private boolean isDevCameraActive() {
        return activeDebugCamera != null && activeDebugCamera.isDevCameraActive();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (!enabled || !isDevCameraActive()) {
            return;
        }

        if (isDevCameraKey(event.getCode())) {
            activeDevCameraKeys.add(event.getCode());
            event.consume();
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (!enabled || !isDevCameraActive()) {
            return;
        }

        if (isDevCameraKey(event.getCode())) {
            activeDevCameraKeys.remove(event.getCode());
            event.consume();
        }
    }

    private void updateDevCamera(double tpf) {
        if (!isDevCameraActive()) {
            return;
        }

        double forward = axis(KeyCode.W, KeyCode.UP) - axis(KeyCode.S, KeyCode.DOWN);
        double right = axis(KeyCode.D, KeyCode.RIGHT) - axis(KeyCode.A, KeyCode.LEFT);
        double up = keyDown(KeyCode.Q) - keyDown(KeyCode.E);
        boolean boosted = activeDevCameraKeys.contains(KeyCode.SHIFT);
        if (forward != 0 || right != 0 || up != 0) {
            activeDebugCamera.move(forward, right, up, boosted, tpf);
            saveDevCameraState();
        }
    }

    private double axis(KeyCode positivePrimary, KeyCode positiveSecondary) {
        return Math.min(1, keyDown(positivePrimary) + keyDown(positiveSecondary));
    }

    private int keyDown(KeyCode key) {
        return activeDevCameraKeys.contains(key) ? 1 : 0;
    }

    private static boolean isDevCameraKey(KeyCode keyCode) {
        return keyCode == KeyCode.W
                || keyCode == KeyCode.A
                || keyCode == KeyCode.S
                || keyCode == KeyCode.D
                || keyCode == KeyCode.UP
                || keyCode == KeyCode.DOWN
                || keyCode == KeyCode.LEFT
                || keyCode == KeyCode.RIGHT
                || keyCode == KeyCode.Q
                || keyCode == KeyCode.E
                || keyCode == KeyCode.SHIFT;
    }

    private void select(Inspectable3D item) {
        selection.select(item);
        if (inspectorController != null) {
            inspectorController.inspect(item);
        }
        updateGizmoFor(item);
    }

    private void select(SceneTreeNode treeNode) {
        Inspectable3D item = treeNode.inspectable()
                .orElseGet(() -> Inspectable3D.temporaryForNode(treeNode.screenId(), treeNode.node()));
        select(item);
    }

    private void clearSelection() {
        selection.clear();
        clearGizmo();
        if (inspectorController != null) {
            inspectorController.clear();
        }
    }

    private void refreshSceneTree() {
        if (inspectorController != null) {
            if (activeScreenId == null || activeSubScene == null || activeSubScene.getRoot() == null) {
                return;
            }

            inspectorController.setSceneTree(
                    sceneTreeBuilder.build(activeScreenId, activeSubScene.getRoot(), detachedSceneNodes()),
                    this::select,
                    this::saveNodeVisibility
            );
        }
    }

    private void refreshCameraPicker() {
        if (inspectorController == null) {
            return;
        }

        String selected = activeDebugCamera != null && activeDebugCamera.isDevCameraActive() ? DEV_CAMERA : SCENE_CAMERA;
        inspectorController.setCameraModes(List.of(SCENE_CAMERA, DEV_CAMERA), selected, this::selectCameraMode);
    }

    private void selectCameraMode(String mode) {
        if (activeDebugCamera == null) {
            return;
        }

        if (DEV_CAMERA.equals(mode)) {
            activeDebugCamera.activateDevCamera();
            if (activeSubScene != null) {
                activeSubScene.requestFocus();
            }
        } else {
            restoreSceneCamera();
        }
        refreshCameraPicker();
    }

    private void restoreSceneCamera() {
        if (activeDebugCamera != null && activeDebugCamera.isDevCameraActive()) {
            activeDebugCamera.restoreOriginalCamera();
        }
    }

    private void saveInspectable(Inspectable3D item) {
        if (!item.persistent()) {
            return;
        }

        pendingSaves.put(item.screenId() + ":" + item.id(), item);
        if (saveDebounce == null) {
            flushPendingSaves();
        } else {
            saveDebounce.playFromStart();
        }
    }

    private void saveDevCameraState() {
        if (!isDevCameraActive()) {
            return;
        }

        registry.findExactForNode(activeDebugCamera.devCamera()).ifPresent(item -> {
            saveInspectable(item);
            selection.selected()
                    .filter(selectedItem -> selectedItem.target() == activeDebugCamera.devCamera())
                    .ifPresent(selectedItem -> {
                        if (inspectorController != null) {
                            inspectorController.inspect(selectedItem);
                        }
                    });
        });
    }

    private void saveNodeVisibility(SceneTreeNode treeNode) {
        treeNode.inspectable().ifPresent(this::saveInspectable);
    }

    private void registerCameraInspectables() {
        if (activeScreenId == null || activeDebugCamera == null) {
            return;
        }

        Camera sceneCamera = activeDebugCamera.originalCamera();
        if (sceneCamera != null) {
            registerInspectable(Inspectable3D.forNodeWithGroups(
                    activeScreenId + ".camera.scene",
                    "Scene Camera",
                    activeScreenId,
                    sceneCamera,
                    cameraDebugGroups(activeScreenId + ".camera.scene", sceneCamera)
            ));
        }

        registerInspectable(Inspectable3D.forNodeWithGroups(
                activeScreenId + ".camera.dev",
                "Dev Camera",
                activeScreenId,
                activeDebugCamera.devCamera(),
                devCameraDebugGroups(activeScreenId + ".camera.dev", activeDebugCamera)
        ));
    }

    private List<Node> detachedSceneNodes() {
        if (activeDebugCamera == null) {
            return List.of();
        }

        List<Node> nodes = new ArrayList<>();
        if (activeDebugCamera.originalCamera() != null) {
            nodes.add(activeDebugCamera.originalCamera());
        }
        nodes.add(activeDebugCamera.devCamera());
        return nodes;
    }

    private static List<DebugParameterGroup> cameraDebugGroups(String idPrefix, Camera camera) {
        List<crab.features.devtools.domain.DebugParameter> parameters = new ArrayList<>();
        parameters.add(crab.features.devtools.domain.DebugParameter.number(
                idPrefix + ".x", "Camera X", "Camera", -5000, 5000, 1, camera::getTranslateX, camera::setTranslateX));
        parameters.add(crab.features.devtools.domain.DebugParameter.number(
                idPrefix + ".y", "Camera Y", "Camera", -5000, 5000, 1, camera::getTranslateY, camera::setTranslateY));
        parameters.add(crab.features.devtools.domain.DebugParameter.number(
                idPrefix + ".z", "Camera Z", "Camera", -5000, 5000, 1, camera::getTranslateZ, camera::setTranslateZ));
        if (camera instanceof PerspectiveCamera perspectiveCamera) {
            parameters.add(crab.features.devtools.domain.DebugParameter.number(
                    idPrefix + ".fov", "Field of View", "Camera", 1, 120, 0.25,
                    perspectiveCamera::getFieldOfView,
                    perspectiveCamera::setFieldOfView
            ));
        }
        return List.of(new DebugParameterGroup("Camera", parameters));
    }

    private static List<DebugParameterGroup> devCameraDebugGroups(String idPrefix, DebugCameraController controller) {
        List<crab.features.devtools.domain.DebugParameter> parameters = new ArrayList<>(
                cameraDebugGroups(idPrefix, controller.devCamera()).getFirst().parameters()
        );
        parameters.add(crab.features.devtools.domain.DebugParameter.number(
                idPrefix + ".yaw", "Yaw", "Camera", -360, 360, 0.25,
                controller::yawAngle,
                controller::setYawAngle
        ));
        parameters.add(crab.features.devtools.domain.DebugParameter.number(
                idPrefix + ".pitch", "Pitch", "Camera", -89, 89, 0.25,
                controller::pitchAngle,
                controller::setPitchAngle
        ));
        return List.of(new DebugParameterGroup("Camera", parameters));
    }

    private void flushPendingSaves() {
        List<Inspectable3D> items = List.copyOf(pendingSaves.values());
        pendingSaves.clear();
        items.forEach(overrideStore::saveInspectable);
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
