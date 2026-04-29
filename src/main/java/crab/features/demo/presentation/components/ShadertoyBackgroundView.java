package crab.features.demo.presentation.components;

import com.almasb.fxgl.core.util.Platform;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.texture.GLImageView;
import crab.platform.metal.MetalShorelineRenderer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Renders an animated shoreline backdrop behind 3D demo content.
 *
 * Design patterns:
 * - Adapter: hides FXGL shader-view and JavaFX fallback differences behind one view.
 *
 * SOLID:
 * - Single Responsibility: owns the shader-style background presentation only.
 */
public final class ShadertoyBackgroundView extends Group {
    private static final Logger LOG = Logger.get(ShadertoyBackgroundView.class);
    private static final double PI = 3.14159265369;
    private static final int FALLBACK_SCALE = 2;
    private static final double FALLBACK_FRAME_INTERVAL = 1.0 / 30.0;

    private final int fallbackWidth;
    private final int fallbackHeight;
    private final PixelBuffer<IntBuffer> fallbackPixelBuffer;
    private final WritableImage fallbackImage;
    private final ImageView fallbackView;
    private final IntBuffer fallbackPixels;
    private double elapsedSeconds;
    private double fallbackFrameAccumulator = FALLBACK_FRAME_INTERVAL;
    private double waveSpeed = 1.0;
    private double waveHeight = 1.0;
    private double foamAmount = 1.0;
    private double shoreOffset = 0.6;
    private double chromaticAmount = 1.0;
    private MetalShorelineRenderer metalRenderer;
    private GLImageView shaderView;

    public ShadertoyBackgroundView(int width, int height, String fragmentShader) {
        this(width, height, fragmentShader, false);
    }

    public ShadertoyBackgroundView(int width, int height, String fragmentShader, boolean imageBackedOnly) {
        this.fallbackWidth = Math.max(1, width / FALLBACK_SCALE);
        this.fallbackHeight = Math.max(1, height / FALLBACK_SCALE);
        this.fallbackPixels = ByteBuffer
                .allocateDirect(fallbackWidth * fallbackHeight * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        this.fallbackPixelBuffer = new PixelBuffer<>(
                fallbackWidth,
                fallbackHeight,
                fallbackPixels,
                PixelFormat.getIntArgbPreInstance()
        );
        this.fallbackImage = new WritableImage(fallbackPixelBuffer);
        this.fallbackView = new ImageView(fallbackImage);

        fallbackView.setFitWidth(width);
        fallbackView.setFitHeight(height);
        fallbackView.setSmooth(true);

        if (imageBackedOnly) {
            if (Platform.get() == Platform.MAC) {
                metalRenderer = MetalShorelineRenderer.create(fallbackWidth, fallbackHeight, fallbackPixels).orElse(null);
                if (metalRenderer != null) {
                    LOG.info("Shoreline renderer selected: native Metal GPU compute renderer for image-backed texture");
                } else {
                    LOG.warning("Shoreline renderer selected: Java PixelBuffer CPU fallback for image-backed texture because Metal renderer is unavailable");
                }
            } else {
                LOG.infof(
                        "Shoreline renderer selected: Java PixelBuffer CPU fallback for image-backed texture on %s",
                        Platform.get()
                );
            }

            installFallback();
        } else if (Platform.get() == Platform.WINDOWS) {
            try {
                shaderView = new GLImageView(width, height, fragmentShader);
                applyShaderUniforms();
                getChildren().add(shaderView);
                LOG.info("Shoreline renderer selected: FXGL GLImageView GPU shader");
            } catch (RuntimeException | Error ignored) {
                LOG.warning("Shoreline renderer selected: Java PixelBuffer CPU fallback after GLImageView initialization failed", ignored);
                installFallback();
            }
        } else if (Platform.get() == Platform.MAC) {
            metalRenderer = MetalShorelineRenderer.create(fallbackWidth, fallbackHeight, fallbackPixels).orElse(null);
            if (metalRenderer != null) {
                LOG.info("Shoreline renderer selected: native Metal GPU compute renderer");
            } else {
                LOG.warning("Shoreline renderer selected: Java PixelBuffer CPU fallback because Metal renderer is unavailable");
            }

            installFallback();
        } else {
            LOG.infof(
                    "Shoreline renderer selected: Java PixelBuffer CPU fallback because FXGL GLImageView is not supported on %s",
                    Platform.get()
            );
            installFallback();
        }
    }

    public WritableImage textureImage() {
        return fallbackImage;
    }

    public void setWaveSpeed(double value) {
        waveSpeed = value;
        applyParameterChange();
    }

    public void setWaveHeight(double value) {
        waveHeight = value;
        applyParameterChange();
    }

    public void setFoamAmount(double value) {
        foamAmount = value;
        applyParameterChange();
    }

    public void setShoreOffset(double value) {
        shoreOffset = value;
        applyParameterChange();
    }

    public void setChromaticAmount(double value) {
        chromaticAmount = value;
        applyParameterChange();
    }

    public void update(double tpf) {
        elapsedSeconds += tpf;

        if (shaderView != null) {
            shaderView.onUpdate(tpf);
            return;
        }

        fallbackFrameAccumulator += tpf;
        if (fallbackFrameAccumulator >= FALLBACK_FRAME_INTERVAL) {
            fallbackFrameAccumulator = 0.0;
            renderFallback(elapsedSeconds);
        }
    }

    public void dispose() {
        if (shaderView != null) {
            shaderView.dispose();
            shaderView = null;
        }

        if (metalRenderer != null) {
            metalRenderer.close();
            metalRenderer = null;
        }
    }

    private void installFallback() {
        shaderView = null;
        getChildren().setAll(fallbackView);
        renderFallback(0.0);
    }

    private void applyParameterChange() {
        if (shaderView != null) {
            applyShaderUniforms();
        } else {
            renderFallback(elapsedSeconds);
        }
    }

    private void applyShaderUniforms() {
        shaderView.getProperties().setValue("waveSpeed", waveSpeed);
        shaderView.getProperties().setValue("waveHeight", waveHeight);
        shaderView.getProperties().setValue("foamAmount", foamAmount);
        shaderView.getProperties().setValue("shoreOffset", shoreOffset);
        shaderView.getProperties().setValue("chromaticAmount", chromaticAmount);
    }

    private void renderFallback(double time) {
        if (metalRenderer != null && renderMetalFallback(time)) {
            updateFallbackImage();
            return;
        }

        renderCpuFallback(time);
        updateFallbackImage();
    }

    private boolean renderMetalFallback(double time) {
        boolean rendered = metalRenderer.render(
                time,
                waveSpeed,
                waveHeight,
                foamAmount,
                shoreOffset,
                chromaticAmount
        );

        if (!rendered) {
            LOG.warning("Metal shoreline renderer failed; switching to Java PixelBuffer CPU fallback");
            metalRenderer.close();
            metalRenderer = null;
        }

        return rendered;
    }

    private void renderCpuFallback(double time) {
        int index = 0;

        for (int y = 0; y < fallbackHeight; y++) {
            double uvY = 1.0 - (y + 0.5) / fallbackHeight;

            for (int x = 0; x < fallbackWidth; x++) {
                double uvX = (x + 0.5) / fallbackWidth;
                fallbackPixels.put(index++, shaderColor(
                        uvX,
                        uvY,
                        time,
                        waveSpeed,
                        waveHeight,
                        foamAmount,
                        shoreOffset,
                        chromaticAmount
                ));
            }
        }
    }

    private void updateFallbackImage() {
        fallbackPixelBuffer.updateBuffer(pixelBuffer -> new Rectangle2D(0, 0, fallbackWidth, fallbackHeight));
    }

    private static int shaderColor(
            double uvX,
            double uvY,
            double time,
            double waveSpeed,
            double waveHeight,
            double foamAmount,
            double shoreOffset,
            double chromaticAmount
    ) {
        double chmt = 1.3 - vignette(uvX, uvY, 0.9, 0.8);
        double amount = 0.004 * chmt * chromaticAmount;

        double red = acesFilm(frag(uvX + amount, uvY + amount, time, waveSpeed, waveHeight, foamAmount, shoreOffset, 0));
        double green = acesFilm(frag(uvX, uvY, time, waveSpeed, waveHeight, foamAmount, shoreOffset, 1));
        double blue = acesFilm(frag(uvX - amount, uvY - amount, time, waveSpeed, waveHeight, foamAmount, shoreOffset, 2));

        return argb(red, green, blue);
    }

    private static double frag(
            double uvX,
            double uvY,
            double time,
            double waveSpeed,
            double waveHeight,
            double foamAmount,
            double shoreOffset,
            int channel
    ) {
        double sandChannel = colorChannel(255.0, 150.0, 80.0, channel);
        double waterChannel = colorChannel(90.0, 180.0, 110.0, channel);
        double deepWaterChannel = colorChannel(20.0, 70.0, 130.0, channel);

        double shaderTime = time * waveSpeed;
        double slowTime = shaderTime * 0.2;
        double uvy = uvY - (Math.sin(shaderTime) * 0.5 + 0.5) * 0.1 - shoreOffset;
        double wuvy = uvY - (Math.sin(0.75) * 0.5 + 0.5) * 0.1 - 0.59;

        double shore = Math.sin(uvX * PI * 4.0 + slowTime);
        shore += Math.sin(uvX * PI * 3.0);
        shore = shore * 0.5 + 0.5;
        shore *= 0.05 * waveHeight;

        double smshore = smoothstep(uvy * 5.0, uvy * 5.0 + 2.5, shore);
        double wshore = step(wuvy * 5.0, shore);
        shore = smoothstep(uvy * 5.0, uvy * 5.0 + 2.0, shore);

        double shoreMask = step(0.01, shore);

        double suvx = uvX + uvY * 5.0;
        double sand = step(
                fract(uvY * 10.0) * 2.0 - 0.5,
                (Math.sin(suvx * PI * 1.5) + Math.sin(suvx * PI * 2.0)) * 0.5 + 0.5
        );
        sand -= step(fract(uvY * 10.0) * 2.0, Math.sin(suvx * PI * 2.0) * 0.5 + 0.5);

        waterChannel = mix(deepWaterChannel, waterChannel, smoothstep(0.0, 0.5, uvY));
        sandChannel *= clamp(sand, 0.95, 1.0);

        double result = mix(sandChannel, waterChannel, smshore);

        double foam = cnoise(uvX * 30.0, (uvy * 4.0 + slowTime * 0.5) * 10.0) * 0.5 + 0.5;
        foam = clamp(step(shore, foam) * shoreMask * foamAmount, 0.0, 1.0);

        double soff = mix(0.01, 0.2, smoothstep(0.7, 0.0, uvY));
        double foams = cnoise(uvX * 30.0, (uvy * 4.0 + soff + slowTime * 0.5) * 10.0) * 0.5 + 0.5;
        foams = 1.0 - step(shore, foams) * shoreMask * 0.2 * foamAmount;

        result *= foams;
        result *= 1.0 - wshore * (1.0 - shoreMask) * (Math.sin(shaderTime - PI / 2.0) * 0.5 + 0.5) * 0.2;
        result = mix(result, 1.0, foam);
        result = mix(result, result * vignette(uvX, uvY, 0.99, 0.8), 0.5);

        return result;
    }

    private static double cnoise(double x, double y) {
        double piX0 = mod(Math.floor(x), 289.0);
        double piY0 = mod(Math.floor(y), 289.0);
        double piX1 = mod(piX0 + 1.0, 289.0);
        double piY1 = mod(piY0 + 1.0, 289.0);
        double pfX0 = fract(x);
        double pfY0 = fract(y);
        double pfX1 = pfX0 - 1.0;
        double pfY1 = pfY0 - 1.0;

        double i0 = permute(permute(piX0) + piY0);
        double i1 = permute(permute(piX1) + piY0);
        double i2 = permute(permute(piX0) + piY1);
        double i3 = permute(permute(piX1) + piY1);

        double gx0 = 2.0 * fract(i0 * 0.0243902439) - 1.0;
        double gx1 = 2.0 * fract(i1 * 0.0243902439) - 1.0;
        double gx2 = 2.0 * fract(i2 * 0.0243902439) - 1.0;
        double gx3 = 2.0 * fract(i3 * 0.0243902439) - 1.0;

        double gy0 = Math.abs(gx0) - 0.5;
        double gy1 = Math.abs(gx1) - 0.5;
        double gy2 = Math.abs(gx2) - 0.5;
        double gy3 = Math.abs(gx3) - 0.5;

        gx0 -= Math.floor(gx0 + 0.5);
        gx1 -= Math.floor(gx1 + 0.5);
        gx2 -= Math.floor(gx2 + 0.5);
        gx3 -= Math.floor(gx3 + 0.5);

        double norm0 = 1.79284291400159 - 0.85373472095314 * (gx0 * gx0 + gy0 * gy0);
        double norm1 = 1.79284291400159 - 0.85373472095314 * (gx2 * gx2 + gy2 * gy2);
        double norm2 = 1.79284291400159 - 0.85373472095314 * (gx1 * gx1 + gy1 * gy1);
        double norm3 = 1.79284291400159 - 0.85373472095314 * (gx3 * gx3 + gy3 * gy3);

        gx0 *= norm0;
        gy0 *= norm0;
        gx2 *= norm1;
        gy2 *= norm1;
        gx1 *= norm2;
        gy1 *= norm2;
        gx3 *= norm3;
        gy3 *= norm3;

        double n00 = gx0 * pfX0 + gy0 * pfY0;
        double n10 = gx1 * pfX1 + gy1 * pfY0;
        double n01 = gx2 * pfX0 + gy2 * pfY1;
        double n11 = gx3 * pfX1 + gy3 * pfY1;

        double fadeX = fade(pfX0);
        double fadeY = fade(pfY0);
        double nX0 = mix(n00, n10, fadeX);
        double nX1 = mix(n01, n11, fadeX);

        return 2.3 * mix(nX0, nX1, fadeY);
    }

    private static double colorChannel(double r, double g, double b, int channel) {
        return switch (channel) {
            case 0 -> r / 255.0;
            case 1 -> g / 255.0;
            case 2 -> b / 255.0;
            default -> throw new IllegalArgumentException("Unknown color channel: " + channel);
        };
    }

    private static double acesFilm(double x) {
        double a = 2.51;
        double b = 0.03;
        double c = 2.43;
        double d = 0.59;
        double e = 0.14;

        return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
    }

    private static double vignette(double x, double y, double radius, double softness) {
        return smoothstep(radius, radius - softness, Math.hypot(x - 0.5, y - 0.5));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double permute(double value) {
        return mod(((value * 34.0) + 1.0) * value, 289.0);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double step(double edge, double value) {
        return value < edge ? 0.0 : 1.0;
    }

    private static double mix(double start, double end, double amount) {
        return start * (1.0 - amount) + end * amount;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double mod(double value, double divisor) {
        return value - divisor * Math.floor(value / divisor);
    }

    private static int argb(double red, double green, double blue) {
        int r = (int) Math.round(clamp(red, 0.0, 1.0) * 255.0);
        int g = (int) Math.round(clamp(green, 0.0, 1.0) * 255.0);
        int b = (int) Math.round(clamp(blue, 0.0, 1.0) * 255.0);

        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
