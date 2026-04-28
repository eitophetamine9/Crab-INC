#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <CoreFoundation/CoreFoundation.h>
#include <stdint.h>
#include <string.h>

typedef struct {
    float time;
    float waveSpeed;
    float waveHeight;
    float foamAmount;
    float shoreOffset;
    float chromaticAmount;
    uint32_t width;
    uint32_t height;
} CrabShorelineParams;

@interface CrabMetalShorelineRenderer : NSObject
@property(nonatomic, strong) id<MTLDevice> device;
@property(nonatomic, strong) id<MTLCommandQueue> commandQueue;
@property(nonatomic, strong) id<MTLComputePipelineState> pipeline;
@property(nonatomic, strong) id<MTLBuffer> paramsBuffer;
@property(nonatomic, strong) id<MTLBuffer> outputBuffer;
@property(nonatomic) uint32_t width;
@property(nonatomic) uint32_t height;
@end

@implementation CrabMetalShorelineRenderer
@end

static NSString *CrabMetalShorelineShaderSource(void) {
    return
        @"#include <metal_stdlib>\n"
        @"using namespace metal;\n"
        @"constant float PI = 3.14159265369;\n"
        @"struct Params { float time; float waveSpeed; float waveHeight; float foamAmount; float shoreOffset; float chromaticAmount; uint width; uint height; };\n"
        @"float mix1(float a, float b, float t) { return a * (1.0 - t) + b * t; }\n"
        @"float clamp01(float v) { return clamp(v, 0.0, 1.0); }\n"
        @"float fract1(float v) { return fract(v); }\n"
        @"float mod1(float v, float d) { return v - d * floor(v / d); }\n"
        @"float step1(float edge, float v) { return v < edge ? 0.0 : 1.0; }\n"
        @"float smooth1(float edge0, float edge1, float value) { float t = clamp01((value - edge0) / (edge1 - edge0)); return t * t * (3.0 - 2.0 * t); }\n"
        @"float fade1(float t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }\n"
        @"float permute1(float value) { return mod1(((value * 34.0) + 1.0) * value, 289.0); }\n"
        @"float cnoise(float2 p) {\n"
        @"  float piX0 = mod1(floor(p.x), 289.0);\n"
        @"  float piY0 = mod1(floor(p.y), 289.0);\n"
        @"  float piX1 = mod1(piX0 + 1.0, 289.0);\n"
        @"  float piY1 = mod1(piY0 + 1.0, 289.0);\n"
        @"  float pfX0 = fract1(p.x);\n"
        @"  float pfY0 = fract1(p.y);\n"
        @"  float pfX1 = pfX0 - 1.0;\n"
        @"  float pfY1 = pfY0 - 1.0;\n"
        @"  float i0 = permute1(permute1(piX0) + piY0);\n"
        @"  float i1 = permute1(permute1(piX1) + piY0);\n"
        @"  float i2 = permute1(permute1(piX0) + piY1);\n"
        @"  float i3 = permute1(permute1(piX1) + piY1);\n"
        @"  float gx0 = 2.0 * fract1(i0 * 0.0243902439) - 1.0;\n"
        @"  float gx1 = 2.0 * fract1(i1 * 0.0243902439) - 1.0;\n"
        @"  float gx2 = 2.0 * fract1(i2 * 0.0243902439) - 1.0;\n"
        @"  float gx3 = 2.0 * fract1(i3 * 0.0243902439) - 1.0;\n"
        @"  float gy0 = abs(gx0) - 0.5;\n"
        @"  float gy1 = abs(gx1) - 0.5;\n"
        @"  float gy2 = abs(gx2) - 0.5;\n"
        @"  float gy3 = abs(gx3) - 0.5;\n"
        @"  gx0 -= floor(gx0 + 0.5);\n"
        @"  gx1 -= floor(gx1 + 0.5);\n"
        @"  gx2 -= floor(gx2 + 0.5);\n"
        @"  gx3 -= floor(gx3 + 0.5);\n"
        @"  float norm0 = 1.79284291400159 - 0.85373472095314 * (gx0 * gx0 + gy0 * gy0);\n"
        @"  float norm1 = 1.79284291400159 - 0.85373472095314 * (gx2 * gx2 + gy2 * gy2);\n"
        @"  float norm2 = 1.79284291400159 - 0.85373472095314 * (gx1 * gx1 + gy1 * gy1);\n"
        @"  float norm3 = 1.79284291400159 - 0.85373472095314 * (gx3 * gx3 + gy3 * gy3);\n"
        @"  gx0 *= norm0; gy0 *= norm0; gx2 *= norm1; gy2 *= norm1; gx1 *= norm2; gy1 *= norm2; gx3 *= norm3; gy3 *= norm3;\n"
        @"  float n00 = gx0 * pfX0 + gy0 * pfY0;\n"
        @"  float n10 = gx1 * pfX1 + gy1 * pfY0;\n"
        @"  float n01 = gx2 * pfX0 + gy2 * pfY1;\n"
        @"  float n11 = gx3 * pfX1 + gy3 * pfY1;\n"
        @"  float fadeX = fade1(pfX0);\n"
        @"  float fadeY = fade1(pfY0);\n"
        @"  return 2.3 * mix1(mix1(n00, n10, fadeX), mix1(n01, n11, fadeX), fadeY);\n"
        @"}\n"
        @"float colorChannel(float r, float g, float b, uint channel) { return channel == 0 ? r / 255.0 : (channel == 1 ? g / 255.0 : b / 255.0); }\n"
        @"float vignette(float2 uv, float radius, float softness) { return smooth1(radius, radius - softness, length(uv - float2(0.5, 0.5))); }\n"
        @"float acesFilm(float x) { const float a = 2.51; const float b = 0.03; const float c = 2.43; const float d = 0.59; const float e = 0.14; return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0); }\n"
        @"float fragChannel(float2 uv, constant Params& p, uint channel) {\n"
        @"  float sandChannel = colorChannel(255.0, 150.0, 80.0, channel);\n"
        @"  float waterChannel = colorChannel(90.0, 180.0, 110.0, channel);\n"
        @"  float deepWaterChannel = colorChannel(20.0, 70.0, 130.0, channel);\n"
        @"  float shaderTime = p.time * p.waveSpeed;\n"
        @"  float slowTime = shaderTime * 0.2;\n"
        @"  float uvy = uv.y - (sin(shaderTime) * 0.5 + 0.5) * 0.1 - p.shoreOffset;\n"
        @"  float wuvy = uv.y - (sin(0.75) * 0.5 + 0.5) * 0.1 - 0.59;\n"
        @"  float shore = sin(uv.x * PI * 4.0 + slowTime);\n"
        @"  shore += sin(uv.x * PI * 3.0);\n"
        @"  shore = shore * 0.5 + 0.5;\n"
        @"  shore *= 0.05 * p.waveHeight;\n"
        @"  float smshore = smooth1(uvy * 5.0, uvy * 5.0 + 2.5, shore);\n"
        @"  float wshore = step1(wuvy * 5.0, shore);\n"
        @"  shore = smooth1(uvy * 5.0, uvy * 5.0 + 2.0, shore);\n"
        @"  float shoreMask = step1(0.01, shore);\n"
        @"  float suvx = uv.x + uv.y * 5.0;\n"
        @"  float sand = step1(fract1(uv.y * 10.0) * 2.0 - 0.5, (sin(suvx * PI * 1.5) + sin(suvx * PI * 2.0)) * 0.5 + 0.5);\n"
        @"  sand -= step1(fract1(uv.y * 10.0) * 2.0, sin(suvx * PI * 2.0) * 0.5 + 0.5);\n"
        @"  waterChannel = mix1(deepWaterChannel, waterChannel, smooth1(0.0, 0.5, uv.y));\n"
        @"  sandChannel *= clamp(sand, 0.95, 1.0);\n"
        @"  float result = mix1(sandChannel, waterChannel, smshore);\n"
        @"  float foam = cnoise(float2(uv.x * 30.0, (uvy * 4.0 + slowTime * 0.5) * 10.0)) * 0.5 + 0.5;\n"
        @"  foam = clamp(step1(shore, foam) * shoreMask * p.foamAmount, 0.0, 1.0);\n"
        @"  float soff = mix1(0.01, 0.2, smooth1(0.7, 0.0, uv.y));\n"
        @"  float foams = cnoise(float2(uv.x * 30.0, (uvy * 4.0 + soff + slowTime * 0.5) * 10.0)) * 0.5 + 0.5;\n"
        @"  foams = 1.0 - step1(shore, foams) * shoreMask * 0.2 * p.foamAmount;\n"
        @"  result *= foams;\n"
        @"  result *= 1.0 - wshore * (1.0 - shoreMask) * (sin(shaderTime - PI / 2.0) * 0.5 + 0.5) * 0.2;\n"
        @"  result = mix1(result, 1.0, foam);\n"
        @"  result = mix1(result, result * vignette(uv, 0.99, 0.8), 0.5);\n"
        @"  return result;\n"
        @"}\n"
        @"uint argb(float r, float g, float b) { uint ri = uint(round(clamp(r, 0.0, 1.0) * 255.0)); uint gi = uint(round(clamp(g, 0.0, 1.0) * 255.0)); uint bi = uint(round(clamp(b, 0.0, 1.0) * 255.0)); return 0xFF000000u | (ri << 16) | (gi << 8) | bi; }\n"
        @"kernel void renderShoreline(device uint *outPixels [[buffer(0)]], constant Params& p [[buffer(1)]], uint2 gid [[thread_position_in_grid]]) {\n"
        @"  if (gid.x >= p.width || gid.y >= p.height) { return; }\n"
        @"  float2 uv = float2((float(gid.x) + 0.5) / float(p.width), 1.0 - (float(gid.y) + 0.5) / float(p.height));\n"
        @"  float chmt = 1.3 - vignette(uv, 0.9, 0.8);\n"
        @"  float amount = 0.004 * chmt * p.chromaticAmount;\n"
        @"  float r = acesFilm(fragChannel(uv + float2(amount, amount), p, 0));\n"
        @"  float g = acesFilm(fragChannel(uv, p, 1));\n"
        @"  float b = acesFilm(fragChannel(uv - float2(amount, amount), p, 2));\n"
        @"  outPixels[gid.y * p.width + gid.x] = argb(r, g, b);\n"
        @"}\n";
}

int crab_metal_shoreline_supported(void) {
    return MTLCreateSystemDefaultDevice() != nil ? 1 : 0;
}

void *crab_metal_shoreline_create(int width, int height) {
    id<MTLDevice> device = MTLCreateSystemDefaultDevice();
    if (device == nil || width <= 0 || height <= 0) {
        return NULL;
    }

    NSError *error = nil;
    id<MTLLibrary> library = [device newLibraryWithSource:CrabMetalShorelineShaderSource() options:nil error:&error];
    if (library == nil) {
        NSLog(@"Crab Metal shoreline library compile failed: %@", error);
        return NULL;
    }

    id<MTLFunction> function = [library newFunctionWithName:@"renderShoreline"];
    if (function == nil) {
        return NULL;
    }

    id<MTLComputePipelineState> pipeline = [device newComputePipelineStateWithFunction:function error:&error];
    if (pipeline == nil) {
        NSLog(@"Crab Metal shoreline pipeline creation failed: %@", error);
        return NULL;
    }

    CrabMetalShorelineRenderer *renderer = [CrabMetalShorelineRenderer new];
    renderer.device = device;
    renderer.commandQueue = [device newCommandQueue];
    renderer.pipeline = pipeline;
    renderer.paramsBuffer = [device newBufferWithLength:sizeof(CrabShorelineParams) options:MTLResourceStorageModeShared];
    renderer.outputBuffer = [device newBufferWithLength:(NSUInteger)width * (NSUInteger)height * sizeof(uint32_t) options:MTLResourceStorageModeShared];
    renderer.width = (uint32_t)width;
    renderer.height = (uint32_t)height;

    if (renderer.commandQueue == nil || renderer.paramsBuffer == nil || renderer.outputBuffer == nil) {
        return NULL;
    }

    return (__bridge_retained void *)renderer;
}

int crab_metal_shoreline_render(
        void *rendererPointer,
        float time,
        float waveSpeed,
        float waveHeight,
        float foamAmount,
        float shoreOffset,
        float chromaticAmount,
        uint32_t *outPixels
) {
    if (rendererPointer == NULL || outPixels == NULL) {
        return 0;
    }

    CrabMetalShorelineRenderer *renderer = (__bridge CrabMetalShorelineRenderer *)rendererPointer;
    CrabShorelineParams params = {
            time,
            waveSpeed,
            waveHeight,
            foamAmount,
            shoreOffset,
            chromaticAmount,
            renderer.width,
            renderer.height
    };

    memcpy(renderer.paramsBuffer.contents, &params, sizeof(params));

    id<MTLCommandBuffer> commandBuffer = [renderer.commandQueue commandBuffer];
    id<MTLComputeCommandEncoder> encoder = [commandBuffer computeCommandEncoder];
    if (commandBuffer == nil || encoder == nil) {
        return 0;
    }

    [encoder setComputePipelineState:renderer.pipeline];
    [encoder setBuffer:renderer.outputBuffer offset:0 atIndex:0];
    [encoder setBuffer:renderer.paramsBuffer offset:0 atIndex:1];

    MTLSize grid = MTLSizeMake(renderer.width, renderer.height, 1);
    MTLSize threadgroup = MTLSizeMake(16, 16, 1);
    [encoder dispatchThreads:grid threadsPerThreadgroup:threadgroup];
    [encoder endEncoding];
    [commandBuffer commit];
    [commandBuffer waitUntilCompleted];

    if (commandBuffer.status != MTLCommandBufferStatusCompleted) {
        return 0;
    }

    memcpy(outPixels, renderer.outputBuffer.contents, (NSUInteger)renderer.width * (NSUInteger)renderer.height * sizeof(uint32_t));
    return 1;
}

void crab_metal_shoreline_destroy(void *rendererPointer) {
    if (rendererPointer != NULL) {
        CFRelease(rendererPointer);
    }
}
