package crab.features.devtools.domain;

public enum DevToolMode {
    SELECT("Select"),
    MOVE("Move"),
    ROTATE("Rotate"),
    SCALE("Scale"),
    INSPECT("Inspect"),
    FLY_CAMERA("Fly Camera");

    private final String displayName;

    DevToolMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
