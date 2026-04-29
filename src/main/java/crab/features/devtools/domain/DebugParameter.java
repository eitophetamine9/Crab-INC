package crab.features.devtools.domain;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Runtime-only numeric parameter exposed by a screen for development inspection.
 */
public record DebugParameter(
        String id,
        String name,
        String group,
        double min,
        double max,
        double step,
        DoubleSupplier getter,
        DoubleConsumer setter
) {
    public DebugParameter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        if (max < min) {
            throw new IllegalArgumentException("max must be greater than or equal to min");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive");
        }
    }

    public static DebugParameter number(
            String id,
            String name,
            String group,
            double min,
            double max,
            double step,
            DoubleSupplier getter,
            DoubleConsumer setter
    ) {
        return new DebugParameter(id, name, group, min, max, step, getter, setter);
    }

    public double value() {
        return getter.getAsDouble();
    }

    public void setValue(double value) {
        setter.accept(clamp(value));
    }

    public double clamp(double value) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
