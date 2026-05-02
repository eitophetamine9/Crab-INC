package crab.platform.javafx3d;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the static embedded-texture GLB skybox used by the environment scene.
 */
public final class GlbSkyboxLoader {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int GLB_HEADER_BYTES = 12;
    private static final int CHUNK_HEADER_BYTES = 8;
    private static final int JSON_CHUNK_TYPE = 0x4E4F534A;
    private static final int BIN_CHUNK_TYPE = 0x004E4942;

    private GlbSkyboxLoader() {
    }

    public static Node load(URL glbUrl, float targetSize) throws IOException {
        GlbData data = readGlb(glbUrl);
        JsonNode root = JSON.readTree(data.json());
        TriangleMesh mesh = createMesh(root, data.binary(), targetSize);
        Image skyTexture = readFirstEmbeddedImage(root, data.binary());

        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setDiffuseMap(skyTexture);
        material.setSelfIlluminationMap(skyTexture);
        material.setSpecularColor(Color.TRANSPARENT);

        MeshView skybox = new MeshView(mesh);
        skybox.setCullFace(CullFace.NONE);
        skybox.setMaterial(material);
        return skybox;
    }

    private static TriangleMesh createMesh(JsonNode root, ByteBuffer binary, float targetSize) {
        JsonNode primitive = root.at("/meshes/0/primitives/0");
        int positionAccessor = primitive.at("/attributes/POSITION").asInt();
        int texCoordAccessor = primitive.at("/attributes/TEXCOORD_0").asInt();
        int indexAccessor = primitive.path("indices").asInt();

        float[] positions = readVec3Accessor(root, binary, positionAccessor);
        float[] texCoords = readVec2Accessor(root, binary, texCoordAccessor);
        int[] indices = readScalarIndices(root, binary, indexAccessor);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(centerAndScale(positions, targetSize));
        mesh.getTexCoords().setAll(flipV(texCoords));

        int[] faces = new int[indices.length * 2];
        for (int i = 0; i < indices.length; i++) {
            faces[i * 2] = indices[i];
            faces[i * 2 + 1] = indices[i];
        }
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    private static GlbData readGlb(URL glbUrl) throws IOException {
        byte[] bytes;
        try (var input = glbUrl.openStream()) {
            bytes = input.readAllBytes();
        }

        ByteBuffer source = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (source.getInt(0) != 0x46546C67) {
            throw new IOException("Invalid GLB magic: " + glbUrl);
        }

        byte[] json = null;
        byte[] binary = null;
        int offset = GLB_HEADER_BYTES;
        while (offset < source.limit()) {
            int chunkLength = source.getInt(offset);
            int chunkType = source.getInt(offset + Integer.BYTES);
            int chunkStart = offset + CHUNK_HEADER_BYTES;

            if (chunkType == JSON_CHUNK_TYPE) {
                json = new byte[chunkLength];
                System.arraycopy(bytes, chunkStart, json, 0, chunkLength);
            } else if (chunkType == BIN_CHUNK_TYPE) {
                binary = new byte[chunkLength];
                System.arraycopy(bytes, chunkStart, binary, 0, chunkLength);
            }

            offset = chunkStart + chunkLength;
        }

        if (json == null || binary == null) {
            throw new IOException("GLB must contain JSON and BIN chunks: " + glbUrl);
        }

        return new GlbData(json, ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN));
    }

    private static Image readFirstEmbeddedImage(JsonNode root, ByteBuffer binary) {
        JsonNode image = root.at("/images/0");
        JsonNode view = root.path("bufferViews").get(image.path("bufferView").asInt());
        int start = view.path("byteOffset").asInt(0);
        int length = view.path("byteLength").asInt();
        byte[] imageBytes = new byte[length];

        ByteBuffer duplicate = binary.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        duplicate.position(start);
        duplicate.get(imageBytes);
        return new Image(new ByteArrayInputStream(imageBytes));
    }

    private static float[] readVec3Accessor(JsonNode root, ByteBuffer buffer, int accessorIndex) {
        return readFloatAccessor(root, buffer, accessorIndex, 3);
    }

    private static float[] readVec2Accessor(JsonNode root, ByteBuffer buffer, int accessorIndex) {
        return readFloatAccessor(root, buffer, accessorIndex, 2);
    }

    private static float[] readFloatAccessor(JsonNode root, ByteBuffer buffer, int accessorIndex, int elementSize) {
        JsonNode accessor = root.path("accessors").get(accessorIndex);
        JsonNode view = root.path("bufferViews").get(accessor.path("bufferView").asInt());
        int count = accessor.path("count").asInt();
        int start = view.path("byteOffset").asInt(0) + accessor.path("byteOffset").asInt(0);
        int stride = view.path("byteStride").asInt(elementSize * Float.BYTES);
        List<Float> values = new ArrayList<>(count * elementSize);

        for (int i = 0; i < count; i++) {
            int itemStart = start + i * stride;
            for (int component = 0; component < elementSize; component++) {
                values.add(buffer.getFloat(itemStart + component * Float.BYTES));
            }
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

    private static float[] flipV(float[] texCoords) {
        float[] flipped = new float[texCoords.length];
        for (int i = 0; i < texCoords.length; i += 2) {
            flipped[i] = texCoords[i];
            flipped[i + 1] = 1 - texCoords[i + 1];
        }
        return flipped;
    }

    private record GlbData(byte[] json, ByteBuffer binary) {
    }
}
