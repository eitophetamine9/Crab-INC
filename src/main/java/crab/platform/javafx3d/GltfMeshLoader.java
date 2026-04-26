package crab.platform.javafx3d;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.shape.TriangleMesh;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a small static glTF mesh into a JavaFX TriangleMesh.
 *
 * code is taken somewhere online, i forgot where tho.
 *
 * Design patterns:
 * - Adapter: converts glTF buffer/accessor data into JavaFX mesh data.
 *
 * SOLID:
 * - Single Responsibility: knows only how to read static mesh geometry.
 */
public final class GltfMeshLoader {
    private static final ObjectMapper JSON = new ObjectMapper();

    private GltfMeshLoader() {
    }

    public static TriangleMesh load(URL gltfUrl) throws IOException {
        JsonNode root = JSON.readTree(gltfUrl);
        ByteBuffer buffer = loadBinaryBuffer(gltfUrl, root).order(ByteOrder.LITTLE_ENDIAN);

        int positionAccessor = root.at("/meshes/0/primitives/0/attributes/POSITION").asInt();
        int indexAccessor = root.at("/meshes/0/primitives/0/indices").asInt();

        float[] positions = readVec3Accessor(root, buffer, positionAccessor);
        int[] indices = readScalarIndices(root, buffer, indexAccessor);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(centerAndScale(positions, 230));
        mesh.getTexCoords().setAll(0, 0);

        int[] faces = new int[indices.length * 2];
        for (int i = 0; i < indices.length; i += 3) {
            // Flipping one axis for JavaFX space also flips winding order,
            // so swap the last two vertices to preserve outward-facing triangles.
            int a = indices[i];
            int b = indices[i + 2];
            int c = indices[i + 1];

            faces[i * 2] = a;
            faces[i * 2 + 1] = 0;
            faces[(i + 1) * 2] = b;
            faces[(i + 1) * 2 + 1] = 0;
            faces[(i + 2) * 2] = c;
            faces[(i + 2) * 2 + 1] = 0;
        }
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    private static ByteBuffer loadBinaryBuffer(URL gltfUrl, JsonNode root) throws IOException {
        String uri = root.at("/buffers/0/uri").asText();
        URL binaryUrl = resolveSibling(gltfUrl, uri);

        try (InputStream input = binaryUrl.openStream()) {
            return ByteBuffer.wrap(input.readAllBytes());
        }
    }

    private static URL resolveSibling(URL source, String sibling) throws IOException {
        try {
            URI base = source.toURI();
            URI resolved = base.resolve(sibling);
            return resolved.toURL();
        } catch (URISyntaxException exception) {
            throw new IOException("Invalid glTF URL: " + source, exception);
        }
    }

    private static float[] readVec3Accessor(JsonNode root, ByteBuffer buffer, int accessorIndex) {
        JsonNode accessor = root.path("accessors").get(accessorIndex);
        JsonNode view = root.path("bufferViews").get(accessor.path("bufferView").asInt());
        int count = accessor.path("count").asInt();
        int start = view.path("byteOffset").asInt(0) + accessor.path("byteOffset").asInt(0);
        int stride = view.path("byteStride").asInt(12);
        List<Float> values = new ArrayList<>(count * 3);

        for (int i = 0; i < count; i++) {
            int itemStart = start + i * stride;
            values.add(buffer.getFloat(itemStart));
            values.add(buffer.getFloat(itemStart + 4));
            values.add(buffer.getFloat(itemStart + 8));
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static int[] readScalarIndices(JsonNode root, ByteBuffer buffer, int accessorIndex) {
        JsonNode accessor = root.path("accessors").get(accessorIndex);
        JsonNode view = root.path("bufferViews").get(accessor.path("bufferView").asInt());
        int count = accessor.path("count").asInt();
        int start = view.path("byteOffset").asInt(0) + accessor.path("byteOffset").asInt(0);
        int componentType = accessor.path("componentType").asInt();
        int[] indices = new int[count];

        for (int i = 0; i < count; i++) {
            indices[i] = switch (componentType) {
                case 5123 -> Short.toUnsignedInt(buffer.getShort(start + i * Short.BYTES));
                case 5125 -> buffer.getInt(start + i * Integer.BYTES);
                default -> throw new IllegalArgumentException("Unsupported glTF index component type: " + componentType);
            };
        }
        return indices;
    }

    private static float[] centerAndScale(float[] positions, float targetSize) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < positions.length; i += 3) {
            minX = Math.min(minX, positions[i]);
            maxX = Math.max(maxX, positions[i]);
            minY = Math.min(minY, positions[i + 1]);
            maxY = Math.max(maxY, positions[i + 1]);
            minZ = Math.min(minZ, positions[i + 2]);
            maxZ = Math.max(maxZ, positions[i + 2]);
        }

        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        float maxDimension = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        float scale = targetSize / maxDimension;

        float[] scaled = new float[positions.length];
        for (int i = 0; i < positions.length; i += 3) {
            scaled[i] = (positions[i] - centerX) * scale;
            scaled[i + 1] = -(positions[i + 1] - centerY) * scale;
            scaled[i + 2] = (positions[i + 2] - centerZ) * scale;
        }
        return scaled;
    }
}
