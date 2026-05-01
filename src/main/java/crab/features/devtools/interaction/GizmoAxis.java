package crab.features.devtools.interaction;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public enum GizmoAxis {
    X(Color.rgb(248, 72, 72)) {
        @Override
        public void applyDelta(Node node, double amount) {
            node.setTranslateX(node.getTranslateX() + amount);
        }

        @Override
        public double dragAmount(double deltaX, double deltaY) {
            return deltaX;
        }
    },
    Y(Color.rgb(64, 132, 255)) {
        @Override
        public void applyDelta(Node node, double amount) {
            node.setTranslateY(node.getTranslateY() + amount);
        }

        @Override
        public double dragAmount(double deltaX, double deltaY) {
            return -deltaY;
        }
    },
    Z(Color.rgb(120, 205, 64)) {
        @Override
        public void applyDelta(Node node, double amount) {
            node.setTranslateZ(node.getTranslateZ() + amount);
        }

        @Override
        public double dragAmount(double deltaX, double deltaY) {
            return deltaY;
        }
    };

    private final Color color;

    GizmoAxis(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }

    public abstract void applyDelta(Node node, double amount);

    public abstract double dragAmount(double deltaX, double deltaY);
}
