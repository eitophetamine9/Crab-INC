package crab.features.devtools.interaction;

import crab.features.devtools.domain.DevToolMode;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class TransformGizmo3D {
    private static final String GIZMO_NODE_KEY = "crab.devtools.transformGizmo";
    private static final double AXIS_LENGTH = 86;
    private static final double SHAFT_LENGTH = 58;
    private static final double SHAFT_RADIUS = 1.8;
    private static final double ARROW_LENGTH = 18;
    private static final double ARROW_RADIUS = 6;
    private static final double OUTLINE_THICKNESS = 2.2;

    private final Group root = new Group();
    private final Group outline = new Group();
    private final Group axes = new Group();
    private final Sphere originMarker = new Sphere(5);
    private final Map<GizmoAxis, Node> axisHandles = new EnumMap<>(GizmoAxis.class);
    private Point3D currentOrigin = Point3D.ZERO;

    public TransformGizmo3D() {
        markGizmoNode(root);
        markGizmoNode(outline);
        markGizmoNode(axes);
        root.setDepthTest(DepthTest.DISABLE);
        root.setMouseTransparent(false);
        root.setVisible(false);

        originMarker.setMaterial(material(Color.rgb(255, 160, 38)));
        markGizmoNode(originMarker);
        axes.getChildren().add(originMarker);
        axes.getChildren().add(axisHandle(GizmoAxis.X));
        axes.getChildren().add(axisHandle(GizmoAxis.Y));
        axes.getChildren().add(axisHandle(GizmoAxis.Z));
        root.getChildren().setAll(outline, axes);
    }

    public Group node() {
        return root;
    }

    public void attachTo(Parent sceneRoot) {
        if (!(sceneRoot instanceof Group group)) {
            return;
        }

        if (root.getParent() == group) {
            return;
        }

        detach();
        group.getChildren().add(root);
    }

    public void detach() {
        if (root.getParent() instanceof Group group) {
            group.getChildren().remove(root);
        }
    }

    public void showFor(Node selected, Parent sceneRoot) {
        if (selected == null || sceneRoot == null) {
            hide();
            return;
        }

        try {
            Point3D origin = sceneRoot.sceneToLocal(selected.localToScene(Point3D.ZERO));
            Bounds bounds = sceneRoot.sceneToLocal(selected.localToScene(selected.getBoundsInLocal()));
            if (!isValid(origin) || !isValid(bounds)) {
                hide();
                return;
            }

            currentOrigin = origin;
            axes.setTranslateX(origin.getX());
            axes.setTranslateY(origin.getY());
            axes.setTranslateZ(origin.getZ());
            rebuildOutline(bounds);
            root.setVisible(true);
        } catch (RuntimeException exception) {
            hide();
        }
    }

    public void hide() {
        root.setVisible(false);
        outline.getChildren().clear();
    }

    public void setToolMode(DevToolMode mode) {
        axes.setVisible(mode == DevToolMode.MOVE || mode == DevToolMode.ROTATE || mode == DevToolMode.SCALE);
    }

    public Point3D currentOrigin() {
        return currentOrigin;
    }

    public Node axisHandleFor(GizmoAxis axis) {
        return axisHandles.get(axis);
    }

    public Optional<GizmoAxis> axisFor(Node pickedNode) {
        Node current = pickedNode;
        while (current != null) {
            if (current.getUserData() instanceof GizmoAxis axis) {
                return Optional.of(axis);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    public boolean owns(Node pickedNode) {
        return isGizmoNode(pickedNode);
    }

    public static boolean isGizmoNode(Node node) {
        Node current = node;
        while (current != null) {
            if (Boolean.TRUE.equals(current.getProperties().get(GIZMO_NODE_KEY))) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Node axisHandle(GizmoAxis axis) {
        Group handle = new Group();
        handle.setUserData(axis);
        handle.setDepthTest(DepthTest.DISABLE);
        markGizmoNode(handle);

        PhongMaterial axisMaterial = material(axis.color());
        Cylinder shaft = new Cylinder(SHAFT_RADIUS, SHAFT_LENGTH);
        shaft.setMaterial(axisMaterial);
        shaft.setUserData(axis);
        markGizmoNode(shaft);

        MeshView arrow = cone(ARROW_RADIUS, ARROW_LENGTH, axisMaterial);
        arrow.setUserData(axis);
        markGizmoNode(arrow);

        orientAxis(axis, shaft, arrow);
        handle.getChildren().setAll(shaft, arrow);
        axisHandles.put(axis, handle);
        return handle;
    }

    private static void orientAxis(GizmoAxis axis, Cylinder shaft, MeshView arrow) {
        switch (axis) {
            case X -> {
                shaft.setTranslateX(SHAFT_LENGTH / 2.0);
                shaft.getTransforms().add(new Rotate(-90, Rotate.Z_AXIS));
                arrow.setTranslateX(AXIS_LENGTH);
                arrow.getTransforms().add(new Rotate(-90, Rotate.Z_AXIS));
            }
            case Y -> {
                shaft.setTranslateY(-SHAFT_LENGTH / 2.0);
                shaft.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
                arrow.setTranslateY(-AXIS_LENGTH);
                arrow.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
            }
            case Z -> {
                shaft.setTranslateZ(SHAFT_LENGTH / 2.0);
                shaft.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
                arrow.setTranslateZ(AXIS_LENGTH);
                arrow.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
            }
        }
    }

    private void rebuildOutline(Bounds bounds) {
        double minX = bounds.getMinX();
        double minY = bounds.getMinY();
        double minZ = bounds.getMinZ();
        double maxX = bounds.getMaxX();
        double maxY = bounds.getMaxY();
        double maxZ = bounds.getMaxZ();
        double width = Math.max(OUTLINE_THICKNESS, maxX - minX);
        double height = Math.max(OUTLINE_THICKNESS, maxY - minY);
        double depth = Math.max(OUTLINE_THICKNESS, maxZ - minZ);
        PhongMaterial outlineMaterial = material(Color.rgb(255, 160, 38));

        outline.getChildren().setAll(
                edgeX(minX, maxX, minY, minZ, width, outlineMaterial),
                edgeX(minX, maxX, maxY, minZ, width, outlineMaterial),
                edgeX(minX, maxX, minY, maxZ, width, outlineMaterial),
                edgeX(minX, maxX, maxY, maxZ, width, outlineMaterial),
                edgeY(minX, minY, maxY, minZ, height, outlineMaterial),
                edgeY(maxX, minY, maxY, minZ, height, outlineMaterial),
                edgeY(minX, minY, maxY, maxZ, height, outlineMaterial),
                edgeY(maxX, minY, maxY, maxZ, height, outlineMaterial),
                edgeZ(minX, minY, minZ, maxZ, depth, outlineMaterial),
                edgeZ(maxX, minY, minZ, maxZ, depth, outlineMaterial),
                edgeZ(minX, maxY, minZ, maxZ, depth, outlineMaterial),
                edgeZ(maxX, maxY, minZ, maxZ, depth, outlineMaterial)
        );
    }

    private static Box edgeX(double minX, double maxX, double y, double z, double width, PhongMaterial material) {
        Box edge = edgeBox(width, OUTLINE_THICKNESS, OUTLINE_THICKNESS, material);
        edge.setTranslateX((minX + maxX) / 2.0);
        edge.setTranslateY(y);
        edge.setTranslateZ(z);
        return edge;
    }

    private static Box edgeY(double x, double minY, double maxY, double z, double height, PhongMaterial material) {
        Box edge = edgeBox(OUTLINE_THICKNESS, height, OUTLINE_THICKNESS, material);
        edge.setTranslateX(x);
        edge.setTranslateY((minY + maxY) / 2.0);
        edge.setTranslateZ(z);
        return edge;
    }

    private static Box edgeZ(double x, double y, double minZ, double maxZ, double depth, PhongMaterial material) {
        Box edge = edgeBox(OUTLINE_THICKNESS, OUTLINE_THICKNESS, depth, material);
        edge.setTranslateX(x);
        edge.setTranslateY(y);
        edge.setTranslateZ((minZ + maxZ) / 2.0);
        return edge;
    }

    private static Box edgeBox(double width, double height, double depth, PhongMaterial material) {
        Box edge = new Box(width, height, depth);
        edge.setMaterial(material);
        edge.setDepthTest(DepthTest.DISABLE);
        markGizmoNode(edge);
        return edge;
    }

    private static MeshView cone(double radius, double height, PhongMaterial material) {
        int divisions = 24;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0f, 0f);
        mesh.getPoints().addAll(0f, (float) height, 0f);
        for (int i = 0; i < divisions; i++) {
            double angle = (Math.PI * 2.0 * i) / divisions;
            mesh.getPoints().addAll(
                    (float) (Math.cos(angle) * radius),
                    0f,
                    (float) (Math.sin(angle) * radius)
            );
        }
        mesh.getPoints().addAll(0f, 0f, 0f);
        int centerIndex = divisions + 1;
        for (int i = 0; i < divisions; i++) {
            int next = i == divisions - 1 ? 1 : i + 2;
            int current = i + 1;
            mesh.getFaces().addAll(0, 0, current, 0, next, 0);
            mesh.getFaces().addAll(centerIndex, 0, next, 0, current, 0);
        }

        MeshView cone = new MeshView(mesh);
        cone.setCullFace(CullFace.NONE);
        cone.setMaterial(material);
        cone.setDepthTest(DepthTest.DISABLE);
        return cone;
    }

    private static PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(color);
        return material;
    }

    private static void markGizmoNode(Node node) {
        node.getProperties().put(GIZMO_NODE_KEY, true);
    }

    private static boolean isValid(Point3D point) {
        return Double.isFinite(point.getX()) && Double.isFinite(point.getY()) && Double.isFinite(point.getZ());
    }

    private static boolean isValid(Bounds bounds) {
        return Double.isFinite(bounds.getMinX())
                && Double.isFinite(bounds.getMinY())
                && Double.isFinite(bounds.getMinZ())
                && Double.isFinite(bounds.getMaxX())
                && Double.isFinite(bounds.getMaxY())
                && Double.isFinite(bounds.getMaxZ())
                && !bounds.isEmpty();
    }
}
