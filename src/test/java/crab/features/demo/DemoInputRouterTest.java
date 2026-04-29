package crab.features.demo;

import crab.appcore.screen.GameScreen;
import crab.appcore.screen.ScreenManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DemoInputRouterTest {
    @Test
    void routesMovementOnlyToActiveDemoScreen() {
        ScreenManager screens = new ScreenManager();
        FakeScreen box = new FakeScreen("box");
        FakeScreen crab = new FakeScreen("crab");
        screens.register(box);
        screens.register(crab);

        DemoInputRouter router = new DemoInputRouter(screens);
        router.register(box.id(), box);
        router.register(crab.id(), crab);

        screens.show(box.id());
        router.moveRight();
        screens.show(crab.id());
        router.moveUp();

        assertEquals(1, box.rightCount);
        assertEquals(0, box.upCount);
        assertEquals(0, crab.rightCount);
        assertEquals(1, crab.upCount);
    }

    private static final class FakeScreen implements GameScreen, DemoDirectionalControls {
        private final String id;
        private int upCount;
        private int rightCount;

        private FakeScreen(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void moveUp() {
            upCount++;
        }

        @Override
        public void moveDown() {
        }

        @Override
        public void moveLeft() {
        }

        @Override
        public void moveRight() {
            rightCount++;
        }
    }
}
