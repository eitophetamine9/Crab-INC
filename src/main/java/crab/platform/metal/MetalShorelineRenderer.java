package crab.platform.metal;

import com.almasb.fxgl.logging.Logger;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Calls the macOS Metal shoreline compute renderer through Java's native linker.
 *
 * Design patterns:
 * - Adapter: hides the native Metal C ABI behind a Java renderer object.
 *
 * SOLID:
 * - Single Responsibility: owns lifecycle and calls for the Metal shoreline renderer only.
 */
public final class MetalShorelineRenderer implements AutoCloseable {
    private static final Logger LOG = Logger.get(MetalShorelineRenderer.class);
    private static final String LIBRARY_PROPERTY = "crab.metal.library";
    private static final String DEFAULT_LIBRARY_PATH = "target/native/libcrab_metal_shoreline.dylib";

    private static final Linker LINKER = Linker.nativeLinker();
    private static final NativeBindings BINDINGS = NativeBindings.load();

    private final MemorySegment renderer;
    private final MemorySegment outputPixels;
    private boolean closed;

    private MetalShorelineRenderer(MemorySegment renderer, IntBuffer outputPixels) {
        this.renderer = renderer;
        this.outputPixels = MemorySegment.ofBuffer(outputPixels);
    }

    public static Optional<MetalShorelineRenderer> create(int width, int height, IntBuffer outputPixels) {
        if (!outputPixels.isDirect()) {
            LOG.warning("Metal shoreline renderer requires a direct IntBuffer");
            return Optional.empty();
        }

        if (!BINDINGS.available()) {
            return Optional.empty();
        }

        try {
            int supported = (int) BINDINGS.supported.invokeExact();
            if (supported == 0) {
                LOG.warning("Metal shoreline renderer is unavailable on this machine");
                return Optional.empty();
            }

            MemorySegment renderer = (MemorySegment) BINDINGS.create.invokeExact(width, height);
            if (renderer.address() == 0) {
                LOG.warning("Metal shoreline renderer failed to create a native renderer");
                return Optional.empty();
            }

            return Optional.of(new MetalShorelineRenderer(renderer, outputPixels));
        } catch (Throwable exception) {
            LOG.warning("Metal shoreline renderer initialization failed", exception);
            return Optional.empty();
        }
    }

    public boolean render(
            double time,
            double waveSpeed,
            double waveHeight,
            double foamAmount,
            double shoreOffset,
            double chromaticAmount
    ) {
        if (closed) {
            return false;
        }

        try {
            int result = (int) BINDINGS.render.invokeExact(
                    renderer,
                    (float) time,
                    (float) waveSpeed,
                    (float) waveHeight,
                    (float) foamAmount,
                    (float) shoreOffset,
                    (float) chromaticAmount,
                    outputPixels
            );

            return result == 1;
        } catch (Throwable exception) {
            LOG.warning("Metal shoreline renderer frame failed", exception);
            return false;
        }
    }

    @Override
    public void close() {
        if (closed || !BINDINGS.available()) {
            return;
        }

        try {
            BINDINGS.destroy.invokeExact(renderer);
        } catch (Throwable exception) {
            LOG.warning("Metal shoreline renderer disposal failed", exception);
        } finally {
            closed = true;
        }
    }

    private record NativeBindings(
            MethodHandle supported,
            MethodHandle create,
            MethodHandle render,
            MethodHandle destroy
    ) {
        private static NativeBindings load() {
            Optional<Path> libraryPath = findLibraryPath();
            if (libraryPath.isEmpty()) {
                LOG.warning("Metal shoreline renderer library was not found; CPU fallback will be used");
                return unavailable();
            }

            try {
                System.load(libraryPath.get().toString());
                SymbolLookup symbols = SymbolLookup.loaderLookup();

                return new NativeBindings(
                        downcall(symbols, "crab_metal_shoreline_supported", FunctionDescriptor.of(ValueLayout.JAVA_INT)),
                        downcall(symbols, "crab_metal_shoreline_create", FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT
                        )),
                        downcall(symbols, "crab_metal_shoreline_render", FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.JAVA_FLOAT,
                                ValueLayout.ADDRESS
                        )),
                        downcall(symbols, "crab_metal_shoreline_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
                );
            } catch (RuntimeException exception) {
                LOG.warning("Metal shoreline renderer library failed to load; CPU fallback will be used", exception);
                return unavailable();
            }
        }

        private static NativeBindings unavailable() {
            return new NativeBindings(null, null, null, null);
        }

        private static Optional<Path> findLibraryPath() {
            String configuredPath = System.getProperty(LIBRARY_PROPERTY);
            if (configuredPath != null && !configuredPath.isBlank()) {
                Path path = Path.of(configuredPath).toAbsolutePath();
                return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
            }

            Path defaultPath = Path.of(DEFAULT_LIBRARY_PATH).toAbsolutePath();
            return Files.isRegularFile(defaultPath) ? Optional.of(defaultPath) : Optional.empty();
        }

        private static MethodHandle downcall(SymbolLookup symbols, String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = symbols.find(name)
                    .orElseThrow(() -> new IllegalStateException("Missing native symbol: " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }

        private boolean available() {
            return supported != null;
        }
    }
}
