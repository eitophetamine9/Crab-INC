package crab.features.devtools.presentation;

final class ScrubbableNumber {
    private ScrubbableNumber() {
    }

    static double valueForDrag(double startValue, double horizontalDelta, double step) {
        return startValue + horizontalDelta * step;
    }
}
