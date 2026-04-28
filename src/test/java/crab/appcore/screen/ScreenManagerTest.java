package crab.appcore.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ScreenManagerTest {
    @Test
    void showActivatesRequestedScreen() {
        ScreenManager manager = new ScreenManager();
        TestScreen demo = new TestScreen("demo");

        manager.register(demo);
        manager.show("demo");

        assertEquals("demo", manager.currentId().orElseThrow());
        assertEquals(1, demo.showCount);
        assertEquals(0, demo.hideCount);
    }

    @Test
    void switchingScreensHidesPreviousScreen() {
        ScreenManager manager = new ScreenManager();
        TestScreen demo = new TestScreen("demo");
        TestScreen menu = new TestScreen("menu");

        manager.register(demo);
        manager.register(menu);

        manager.show("demo");
        manager.show("menu");

        assertEquals("menu", manager.currentId().orElseThrow());
        assertEquals(1, demo.hideCount);
        assertEquals(1, menu.showCount);
    }

    @Test
    void updateDispatchesOnlyToActiveScreen() {
        ScreenManager manager = new ScreenManager();
        TestScreen demo = new TestScreen("demo");
        TestScreen menu = new TestScreen("menu");

        manager.register(demo);
        manager.register(menu);

        manager.show("demo");
        manager.update(0.5);
        manager.show("menu");
        manager.update(0.25);

        assertEquals(0.5, demo.lastUpdate, 0.0001);
        assertEquals(0.25, menu.lastUpdate, 0.0001);
    }

    @Test
    void showingUnknownScreenFailsFast() {
        ScreenManager manager = new ScreenManager();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.show("missing"));

        assertEquals("Unknown screen: missing", error.getMessage());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        ScreenManager manager = new ScreenManager();
        manager.register(new TestScreen("demo"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.register(new TestScreen("demo")));

        assertEquals("Screen already registered: demo", error.getMessage());
    }

    private static final class TestScreen implements GameScreen {
        private final String id;
        private int showCount;
        private int hideCount;
        private double lastUpdate = -1;

        private TestScreen(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void show() {
            showCount++;
        }

        @Override
        public void hide() {
            hideCount++;
        }

        @Override
        public void update(double tpf) {
            lastUpdate = tpf;
        }
    }
}
