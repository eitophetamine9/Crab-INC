package crab.features.demo;

import crab.appcore.screen.ScreenManager;

import java.util.LinkedHashMap;
import java.util.Map;

final class DemoInputRouter {
    private final ScreenManager screens;
    private final Map<String, DemoDirectionalControls> directionalControls = new LinkedHashMap<>();

    DemoInputRouter(ScreenManager screens) {
        this.screens = screens;
    }

    void register(String screenId, DemoDirectionalControls controls) {
        directionalControls.put(screenId, controls);
    }

    void moveUp() {
        route(DemoDirectionalControls::moveUp);
    }

    void moveDown() {
        route(DemoDirectionalControls::moveDown);
    }

    void moveLeft() {
        route(DemoDirectionalControls::moveLeft);
    }

    void moveRight() {
        route(DemoDirectionalControls::moveRight);
    }

    private void route(ControlAction action) {
        screens.currentId()
                .map(directionalControls::get)
                .ifPresent(action::apply);
    }

    private interface ControlAction {
        void apply(DemoDirectionalControls controls);
    }
}
