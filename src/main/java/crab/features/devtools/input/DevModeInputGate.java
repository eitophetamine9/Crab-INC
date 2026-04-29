package crab.features.devtools.input;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Captures game-level shortcuts that should not leak while devtools owns input.
 */
public final class DevModeInputGate implements EventHandler<KeyEvent> {
    private final BooleanSupplier devModeActive;
    private final Supplier<KeyCode> menuKey;

    public DevModeInputGate(BooleanSupplier devModeActive, Supplier<KeyCode> menuKey) {
        this.devModeActive = Objects.requireNonNull(devModeActive);
        this.menuKey = Objects.requireNonNull(menuKey);
    }

    @Override
    public void handle(KeyEvent event) {
        if (devModeActive.getAsBoolean() && event.getCode() == menuKey.get()) {
            event.consume();
        }
    }
}
