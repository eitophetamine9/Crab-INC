package crab.features.devtools.input;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DevModeInputGateTest {
    @Test
    void consumesMenuKeyOnlyWhileDevModeIsActive() {
        AtomicBoolean devModeActive = new AtomicBoolean(true);
        DevModeInputGate gate = new DevModeInputGate(devModeActive::get, () -> KeyCode.ESCAPE);
        KeyEvent escape = keyPressed(KeyCode.ESCAPE);
        KeyEvent f9 = keyPressed(KeyCode.F9);

        gate.handle(escape);
        gate.handle(f9);

        assertTrue(escape.isConsumed());
        assertFalse(f9.isConsumed());
    }

    @Test
    void leavesMenuKeyAloneInGameMode() {
        DevModeInputGate gate = new DevModeInputGate(() -> false, () -> KeyCode.ESCAPE);
        KeyEvent escape = keyPressed(KeyCode.ESCAPE);

        gate.handle(escape);

        assertFalse(escape.isConsumed());
    }

    private static KeyEvent keyPressed(KeyCode code) {
        return new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                code,
                false,
                false,
                false,
                false
        );
    }
}
