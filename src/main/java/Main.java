/**
 * Starts the Crab Inc. JavaFX/FXGL application.
 *
 * Design patterns:
 * - Facade: gives the JVM and Maven plugin a tiny launch entry point.
 *
 * SOLID:
 * - Single Responsibility: launches the FXGL application and owns no game setup.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        crab.app.Application.main(args);
    }
}
