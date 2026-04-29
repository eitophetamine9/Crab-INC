package crab.features.devtools;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class DevToolsModuleThreadingTest {
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
    void initializesUiFromFxglBackgroundThread() {
        DevToolsModule module = new DevToolsModule();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FXGL Background Thread test");
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<?> initialization = executor.submit(module::initializeUi);

            assertDoesNotThrow(() -> initialization.get(3, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
