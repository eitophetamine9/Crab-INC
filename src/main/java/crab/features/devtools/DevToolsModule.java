package crab.features.devtools;

import crab.app.AppTypography;
import crab.appcore.context.GameContext;
import crab.appcore.context.GameModule;
import crab.features.devtools.camera.CameraDebugParameters;
import crab.features.devtools.camera.DebugCameraController;
import crab.features.devtools.domain.DebugParameterGroup;
import crab.features.devtools.domain.DebugParameterSource;
import crab.features.devtools.domain.DevToolMode;
import crab.features.devtools.domain.Inspectable3D;
import crab.features.devtools.domain.Inspectable3DRegistry;
import crab.features.devtools.domain.Inspectable3DSelection;
import crab.features.devtools.domain.SceneTreeBuilder;
import crab.features.devtools.domain.SceneTreeNode;
import crab.features.devtools.input.DevModeInputGate;
import crab.features.devtools.input.DevMouseInteractionPolicy;
import crab.features.devtools.input.DevMousePressAction;
import crab.features.devtools.interaction.GizmoAxis;
import crab.features.devtools.interaction.TransformGizmo3D;
import crab.features.devtools.interaction.ViewRelativeTransform;
import crab.features.devtools.persistence.DebugSceneOverrideStore;
import crab.features.devtools.presentation.DevInspectorPanelController;
import crab.features.devtools.properties.NodeTransformAdapter;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
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
    private final TransformGizmo3D transformGizmo = new TransformGizmo3D();
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
    private DevToolMode toolMode = DevToolMode.MOVE;
    private boolean dragging;
    private boolean axisDragging;
    private boolean looking;
    private boolean lookMoved;
    private boolean suppressNavigationClick;
    private boolean suppressNextClick;
    private GizmoAxis activeAxis;
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
        saveDebounce = new PauseTransition(Duration.millis(250));
        saveDebounce.setOnFinished(event -> flushPendingSaves());
    }

    @Override
    public void update(double tpf) {
        if (enabled) {
            updateDevCamera(tpf);
            selection.selected().ifPresentOrElse(this::updateGizmoFor, transformGizmo::hide);
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
        if (selection.selectedItems().stream().anyMatch(item -> item.screenId().equals(screenId))) {
            clearSelection();
        }
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
        if (enabled) {
            activateDevCameraNavigation();
        }
        if (!attachedSubScenes.add(subScene)) {
            return;
        }

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleClick(event));
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handlePress(event));
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> handleDrag(event));
        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (looking && (lookMoved || suppressNavigationClick)) {
                suppressNextClick = true;
            }
            dragging = false;
            axisDragging = false;
            looking = false;
            lookMoved = false;
            suppressNavigationClick = false;
            activeAxis = null;
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
            inspectorController.setToolModeConsumer(this::setToolMode);
            inspectorController.setToolMode(toolMode);
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
        Scene scene = new Scene(root);
        AppTypography.applyTo(scene);
        stage.setScene(scene);
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
            attachGizmoToActiveSubScene();
            activateDevCameraNavigation();
            if (activeSubScene != null) {
                activeSubScene.requestFocus();
            }
        } else {
            hideInspectorWindow();
            transformGizmo.hide();
            restoreSceneCamera();
            dragging = false;
            axisDragging = false;
            looking = false;
            lookMoved = false;
            suppressNavigationClick = false;
            suppressNextClick = false;
            activeDevCameraKeys.clear();
            clearSelection();
        }
    }

    private void handleClick(MouseEvent event) {
        if (!enabled) {
            return;
        }

        if (suppressNextClick) {
            suppressNextClick = false;
            event.consume();
            return;
        }

        activeSubScene = (SubScene) event.getSource();
        Node pickedNode = event.getPickResult().getIntersectedNode();
        if (transformGizmo.owns(pickedNode)) {
            event.consume();
            return;
        }
        if (pickedNode == null) {
            if (!event.isShiftDown()) {
                clearSelection();
            }
            return;
        }

        Optional<Inspectable3D> item = registry.findForNode(pickedNode);
        item.ifPresentOrElse(
                selectedItem -> select(selectedItem, event.isShiftDown()),
                () -> {
                    if (!event.isShiftDown()) {
                        clearSelection();
                    }
                }
        );
        event.consume();
    }

    private void handlePress(MouseEvent event) {
        if (!enabled) {
            return;
        }

        activeSubScene = (SubScene) event.getSource();
        Node pickedNode = event.getPickResult().getIntersectedNode();
        Optional<GizmoAxis> pickedAxis = transformGizmo.axisFor(pickedNode);
        if (pickedAxis.isPresent() && selection.selected().isPresent() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            if (!isTransformToolMode()) {
                event.consume();
                return;
            }
            activeAxis = pickedAxis.get();
            axisDragging = true;
            dragging = false;
            looking = false;
            lastDragX = event.getSceneX();
            lastDragY = event.getSceneY();
            event.consume();
            return;
        }

        Optional<Inspectable3D> pickedItem = pickedNode == null ? Optional.empty() : registry.findForNode(pickedNode);
        DevMousePressAction action = mousePolicy.pressAction(toolMode, pickedItem.isPresent(), event.getButton());

        if (action == DevMousePressAction.CAMERA_LOOK) {
            activateDevCameraNavigation();
            looking = true;
            lookMoved = false;
            suppressNavigationClick = pickedItem.isPresent()
                    || event.getButton() != javafx.scene.input.MouseButton.PRIMARY;
            dragging = false;
            axisDragging = false;
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
            if (event.isShiftDown()) {
                return;
            }
            if (!selection.contains(item)) {
                select(item);
            }
            dragging = true;
            axisDragging = false;
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
            double deltaX = event.getSceneX() - lastLookX;
            double deltaY = event.getSceneY() - lastLookY;
            if (deltaX != 0 || deltaY != 0) {
                lookMoved = true;
            }
            activeDebugCamera.look(deltaX, deltaY);
            lastLookX = event.getSceneX();
            lastLookY = event.getSceneY();
            saveDevCameraState();
            event.consume();
            return;
        }

        if (axisDragging && activeAxis != null) {
            double deltaX = event.getSceneX() - lastDragX;
            double deltaY = event.getSceneY() - lastDragY;
            applyTransformToSelection(item -> applyAxisTransform(item, activeAxis, activeAxis.dragAmount(deltaX, deltaY)));
            lastDragX = event.getSceneX();
            lastDragY = event.getSceneY();
            event.consume();
            return;
        }

        if (!dragging) {
            return;
        }

        double deltaX = event.getSceneX() - lastDragX;
        double deltaY = event.getSceneY() - lastDragY;
        applyTransformToSelection(item -> applyBodyTransform(item, deltaX, deltaY));
        lastDragX = event.getSceneX();
        lastDragY = event.getSceneY();
        event.consume();
    }

    private boolean isDevCameraActive() {
        return activeDebugCamera != null && activeDebugCamera.isDevCameraActive();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (enabled && event.getCode() == KeyCode.ESCAPE) {
            returnToDevCameraNavigation();
            event.consume();
            return;
        }

        if (enabled && setToolModeForShortcut(event.getCode())) {
            event.consume();
            return;
        }

        if (!enabled || !isDevCameraActive() || toolMode != DevToolMode.FLY_CAMERA) {
            return;
        }

        if (isDevCameraKey(event.getCode())) {
            activeDevCameraKeys.add(event.getCode());
            event.consume();
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (!enabled || !isDevCameraActive() || toolMode != DevToolMode.FLY_CAMERA) {
            return;
        }

        if (isDevCameraKey(event.getCode())) {
            activeDevCameraKeys.remove(event.getCode());
            event.consume();
        }
    }

    private void updateDevCamera(double tpf) {
        if (!isDevCameraActive() || toolMode != DevToolMode.FLY_CAMERA) {
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

    private boolean setToolModeForShortcut(KeyCode keyCode) {
        DevToolMode mode = switch (keyCode) {
            case S -> DevToolMode.SELECT;
            case M -> DevToolMode.MOVE;
            case R -> DevToolMode.ROTATE;
            case T -> DevToolMode.SCALE;
            case I -> DevToolMode.INSPECT;
            case F -> DevToolMode.FLY_CAMERA;
            default -> null;
        };
        if (mode == null) {
            return false;
        }
        setToolMode(mode);
        return true;
    }

    private void select(Inspectable3D item) {
        select(item, false);
    }

    private void select(Inspectable3D item, boolean additive) {
        if (additive) {
            selection.toggle(item);
        } else {
            selection.select(item);
        }
        selection.selected().ifPresentOrElse(this::inspectPrimarySelection, this::clearSelection);
    }

    private void inspectPrimarySelection(Inspectable3D item) {
        if (inspectorController != null) {
            inspectorController.inspect(item);
            inspectorController.setSelectionCount(selection.selectedItems().size());
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
        transformGizmo.hide();
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

    private void setToolMode(DevToolMode mode) {
        toolMode = mode;
        transformGizmo.setToolMode(mode);
        if (inspectorController != null) {
            inspectorController.setToolMode(mode);
        }
        dragging = false;
        axisDragging = false;
        looking = false;
        lookMoved = false;
        suppressNavigationClick = false;
        activeAxis = null;
        activeDevCameraKeys.clear();
        if (mode == DevToolMode.FLY_CAMERA) {
            activateDevCameraNavigation();
        }
    }

    private void selectCameraMode(String mode) {
        if (activeDebugCamera == null) {
            return;
        }

        if (DEV_CAMERA.equals(mode)) {
            activateDevCameraNavigation();
        } else {
            restoreSceneCamera();
        }
        refreshCameraPicker();
    }

    private void returnToDevCameraNavigation() {
        dragging = false;
        axisDragging = false;
        looking = false;
        lookMoved = false;
        suppressNavigationClick = false;
        activeAxis = null;
        activeDevCameraKeys.clear();
        clearSelection();
        activateDevCameraNavigation();
    }

    private void activateDevCameraNavigation() {
        if (activeDebugCamera == null) {
            return;
        }

        activeDebugCamera.activateDevCamera();
        if (activeSubScene != null) {
            activeSubScene.requestFocus();
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
                    CameraDebugParameters.groupsFor(activeScreenId + ".camera.scene", sceneCamera)
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

    private static List<DebugParameterGroup> devCameraDebugGroups(String idPrefix, DebugCameraController controller) {
        List<crab.features.devtools.domain.DebugParameter> parameters = new ArrayList<>(
                CameraDebugParameters.groupsFor(idPrefix, controller.devCamera()).getFirst().parameters()
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

    private void attachGizmoToActiveSubScene() {
        if (activeSubScene != null && activeSubScene.getRoot() != null) {
            transformGizmo.attachTo(activeSubScene.getRoot());
        }
    }

    private void updateSelectedAfterTransform(Inspectable3D item) {
        if (inspectorController != null) {
            inspectorController.inspect(item);
            inspectorController.setSelectionCount(selection.selectedItems().size());
        }
        updateGizmoFor(item);
        saveInspectable(item);
    }

    private void applyTransformToSelection(java.util.function.Consumer<Inspectable3D> transform) {
        List<Inspectable3D> items = selection.selectedItems();
        for (Inspectable3D item : items) {
            transform.accept(item);
            saveInspectable(item);
        }
        selection.selected().ifPresent(item -> {
            if (inspectorController != null) {
                inspectorController.inspect(item);
                inspectorController.setSelectionCount(selection.selectedItems().size());
            }
            updateGizmoFor(item);
        });
    }

    private boolean isTransformToolMode() {
        return toolMode == DevToolMode.MOVE || toolMode == DevToolMode.ROTATE || toolMode == DevToolMode.SCALE;
    }

    private void applyAxisTransform(Inspectable3D item, GizmoAxis axis, double amount) {
        if (toolMode == DevToolMode.MOVE) {
            axis.applyDelta(item.target(), amount);
            return;
        }

        NodeTransformAdapter adapter = new NodeTransformAdapter(item.target());
        NodeTransformAdapter.TransformSnapshot snapshot = adapter.snapshot();
        if (toolMode == DevToolMode.ROTATE) {
            double degrees = amount * 0.5;
            adapter.setRotation(
                    snapshot.rotateX() + (axis == GizmoAxis.X ? degrees : 0),
                    snapshot.rotateY() + (axis == GizmoAxis.Y ? degrees : 0),
                    snapshot.rotateZ() + (axis == GizmoAxis.Z ? degrees : 0)
            );
            return;
        }

        if (toolMode == DevToolMode.SCALE) {
            double scaleDelta = amount * 0.01;
            adapter.setScale(
                    axis == GizmoAxis.X ? Math.max(0.01, snapshot.scaleX() + scaleDelta) : snapshot.scaleX(),
                    axis == GizmoAxis.Y ? Math.max(0.01, snapshot.scaleY() + scaleDelta) : snapshot.scaleY(),
                    axis == GizmoAxis.Z ? Math.max(0.01, snapshot.scaleZ() + scaleDelta) : snapshot.scaleZ()
            );
        }
    }

    private void applyBodyTransform(Inspectable3D item, double deltaX, double deltaY) {
        Camera camera = activeSubScene == null ? null : activeSubScene.getCamera();
        if (toolMode == DevToolMode.MOVE) {
            ViewRelativeTransform.move(item.target(), camera, deltaX, deltaY, DRAG_SCALE);
            return;
        }

        if (toolMode == DevToolMode.SCALE) {
            ViewRelativeTransform.scaleUniform(item.target(), deltaX, deltaY, 0.01);
            return;
        }

        if (toolMode == DevToolMode.ROTATE) {
            NodeTransformAdapter adapter = new NodeTransformAdapter(item.target());
            NodeTransformAdapter.TransformSnapshot snapshot = adapter.snapshot();
            adapter.setRotation(snapshot.rotateX() + (deltaY * 0.5), snapshot.rotateY() + (deltaX * 0.5), snapshot.rotateZ());
        }
    }

    private void updateGizmoFor(Inspectable3D item) {
        if (activeSubScene == null || activeSubScene.getRoot() == null) {
            transformGizmo.hide();
            return;
        }

        attachGizmoToActiveSubScene();
        transformGizmo.showFor(item.target(), activeSubScene.getRoot());
    }
}
