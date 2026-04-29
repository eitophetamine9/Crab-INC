package crab.features.devtools.presentation;

import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class DevInspectorPanelFxmlTest {
    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean();

    @BeforeAll
    static void startToolkit() throws Exception {
        if (!TOOLKIT_STARTED.compareAndSet(false, true)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
    }

    @Test
    void loadsFloatingInspectorPanelFxml() throws Exception {
        URL resource = getClass().getResource("/fxml/devtools/dev-inspector-panel.fxml");

        assertNotNull(resource);

        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();

        assertNotNull(root);
        assertNotNull(loader.<DevInspectorPanelController>getController());
    }
}
