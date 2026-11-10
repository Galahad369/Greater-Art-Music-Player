package com.local.listentomusic.ambient;

/** Find a light shaft and draw it from the same latched SDR decoder texture. */
final class FrameAmbientShaders {
    static final int BEAM_COLUMNS = 4;
    static final int BEAM_EDGES = 4;

    private FrameAmbientShaders() {}

    static final String VERTEX = """
            attribute vec2 aPosition;
            varying vec2 vUv;
            void main() {
                vUv = aPosition * .5 + .5;
                gl_Position = vec4(aPosition, 0., 1.);
            }
            """;

    private static final String FRAME_INPUT = """
            #extension GL_OES_EGL_image_external : require
            precision highp float;
            varying vec2 vUv;
            uniform samplerExternalOES uFrame;
            uniform mat4 uTransform;
            uniform vec2 uSize;
            uniform vec4 uStage;
            uniform vec4 uVideo;

            // Offsets into the visible picture, not into the untrimmed texture.
            const float NEAR_EDGE = .016;
            const float INNER = .220;

            vec3 frame(vec2 uv) {
                return texture2D(uFrame, (uTransform * vec4(clamp(uv, 0., 1.), 0., 1.)).xy).rgb;
            }

            vec4 visibleVideo() {
                vec2 lo = max(max(uStage.xy, uVideo.xy), vec2(0.));
                vec2 hi = min(min(uStage.xy + uStage.zw, uVideo.xy + uVideo.zw), uSize);
                return vec4(lo, hi - lo);
            }
            """;

    // Each row describes one edge (bottom/top/left/right). The map contains only
    // light coordinates, width, confidence and tint; it is not a downscaled frame.
    static final String ANALYZE = FRAME_INPUT + """
            struct Spot { float center; float spread; float confidence; vec3 tint; };

            Spot hotspot(vec2 origin, vec2 span, bool horizontal, bool positive, float inset) {
                float normal = positive ? 1. - inset : inset;
                float mean = 0., peak = 0., weight = 0., first = 0., second = 0.;
                vec3 tint = vec3(0.);
                for (int i = 0; i < 64; ++i) {
                    float t = (float(i) + .5) / 64.;
                    vec2 uv = horizontal ? vec2(t, normal) : vec2(normal, t);
                    vec3 color = frame(origin + uv * span);
                    float hi = max(color.r, max(color.g, color.b));
                    float energy = .72 * hi + .28 * (color.r + color.g + color.b) / 3.;
                    float w = smoothstep(.57, .92, energy);
                    w *= w;
                    mean += energy / 64.;
                    peak = max(peak, energy);
                    weight += w;
                    first += w * t;
                    second += w * t * t;
                    tint += color * w * (.16 + hi - min(color.r, min(color.g, color.b)));
                }
                float center = first / max(weight, .0001);
                float spread = sqrt(max(0., second / max(weight, .0001) - center * center));
                float confidence = smoothstep(.24, 1.45, weight) * smoothstep(.19, .49, peak - mean)
                    * (1. - smoothstep(.066, .145, spread));
                return Spot(center, spread, confidence, tint / max(max(tint.r, max(tint.g, tint.b)), .001));
            }

            // Pack positions more precisely than one byte: the small difference
            // between two positions controls the angle over the entire viewport.
            vec2 packPosition(float value) {
                float code = floor(clamp(value, 0., 1.) * 65535. + .5);
                return vec2(floor(code / 256.), mod(code, 256.)) / 255.;
            }

            void main() {
                float side = floor(vUv.y * 4.);
                bool horizontal = side < 2.;
                bool positive = mod(side, 2.) > .5;
                vec2 nearSide = uStage.xy;
                vec2 farSide = uSize - uStage.xy - uStage.zw;
                vec2 room = positive ? farSide : nearSide;
                vec4 visible = visibleVideo();
                if (vUv.x >= .75 || (horizontal ? room.y : room.x) <= 0.
                        || any(lessThanEqual(visible.zw, vec2(0.)))) {
                    gl_FragColor = vec4(0.);
                    return;
                }
                vec2 origin = (visible.xy - uVideo.xy) / uVideo.zw;
                vec2 span = visible.zw / uVideo.zw;
                Spot edge = hotspot(origin, span, horizontal, positive, NEAR_EDGE);
                Spot middle = hotspot(origin, span, horizontal, positive, (NEAR_EDGE + INNER) * .5);
                Spot inner = hotspot(origin, span, horizontal, positive, INNER);
                // Reject broad highlights and disconnected bright objects rather
                // than guessing an angle from a single line of the picture.
                float aligned = 1. - smoothstep(.027, .091, abs(middle.center - (edge.center + inner.center) * .5));
                float strength = min(edge.confidence, min(middle.confidence, inner.confidence)) * aligned;
                if (vUv.x < .25) {
                    gl_FragColor = vec4(packPosition(edge.center), packPosition(inner.center));
                } else if (vUv.x < .5) {
                    gl_FragColor = vec4(clamp(edge.spread * 1.9, .012, .16), strength, 0., 0.);
                } else {
                    gl_FragColor = vec4(edge.tint, 1.);
                }
            }
            """;

    static final String COMPOSITE = FRAME_INPUT + """
            uniform sampler2D uBeams;
            uniform vec3 uHeldGlow;
            uniform float uHold;
            uniform float uLight;

            bool inside(vec2 p, vec4 rect) {
                return all(greaterThanEqual(p, rect.xy)) && all(lessThan(p, rect.xy + rect.zw));
            }

            vec3 edgeAnchor(vec2 uv, vec2 origin, vec2 span) {
                vec2 center = origin + uv * span;
                vec2 d = vec2(.052, .056) * span;
                return .25 * (frame(center - d) + frame(center + d)
                    + frame(center + vec2(-d.x, d.y)) + frame(center + vec2(d.x, -d.y)));
            }

            float unpackPosition(vec2 encoded) {
                return dot(encoded, vec2(65280., 255.)) / 65535.;
            }

            vec3 shaft(vec2 xy, bool horizontal, bool positive, float edgeLength, float screenDistance) {
                float row = ((horizontal ? 0. : 2.) + (positive ? 1. : 0.) + .5) / 4.;
                vec2 shape = texture2D(uBeams, vec2(.375, row)).rg;
                if (shape.y <= .001) return vec3(0.);
                vec4 positions = texture2D(uBeams, vec2(.125, row));
                vec3 tint = texture2D(uBeams, vec2(.625, row)).rgb;
                float axis = horizontal ? xy.y : xy.x;
                float beyond = max(0., positive ? axis - 1. : -axis);
                float tangent = horizontal ? xy.x : xy.y;
                float exit = unpackPosition(positions.rg);
                float angle = (exit - unpackPosition(positions.ba)) / (INNER - NEAR_EDGE);
                float center = exit + angle * (beyond + NEAR_EDGE);
                float x = (tangent - center) / (shape.x + .055 * screenDistance * uSize.x / edgeLength);
                float core = exp2(-8. * x * x);
                float rim = exp2(-.87 * x * x);
                float haze = exp2(-.14 * x * x);
                float reach = exp2(-.40 * screenDistance);
                tint = max(vec3(0.), tint - vec3(.62 * min(tint.r, min(tint.g, tint.b))));
                tint /= max(max(tint.r, max(tint.g, tint.b)), .01);
                vec3 plasma = tint * min(1., rim * .98 + haze * .32);
                vec3 hot = mix(tint, vec3(1.), .91);
                return shape.y * reach * mix(plasma, hot, core * .89);
            }

            void main() {
                vec2 p = vUv * uSize;
                // The foreground draw is the existing unmodified video path.
                if (inside(p, uStage)) {
                    gl_FragColor = inside(p, uVideo) ? vec4(frame((p - uVideo.xy) / uVideo.zw), 1.)
                        : vec4(0., 0., 0., 1.);
                    return;
                }
                vec4 visible = visibleVideo();
                if (uLight <= 0. || any(lessThanEqual(visible.zw, vec2(0.)))) {
                    gl_FragColor = vec4(vec3(.012), 1.);
                    return;
                }
                vec2 delta = p - clamp(p, uStage.xy, uStage.xy + uStage.zw);
                bool horizontal = abs(delta.y) >= abs(delta.x);
                bool positive = (horizontal ? delta.y : delta.x) > 0.;
                vec2 nearest = clamp(p, visible.xy, visible.xy + visible.zw);
                vec2 edgePosition = (nearest - visible.xy) / visible.zw;
                float t = (horizontal ? edgePosition.x : edgePosition.y) * 4.;
                float slot = min(floor(t), 3.);
                float a = .12 + .19 * slot, side = positive ? .88 : .12;
                vec2 uvA = horizontal ? vec2(a, side) : vec2(side, a);
                vec2 uvB = horizontal ? vec2(a + .19, side) : vec2(side, a + .19);
                vec2 origin = (visible.xy - uVideo.xy) / uVideo.zw;
                vec2 span = visible.zw / uVideo.zw;
                vec3 edge = mix(edgeAnchor(uvA, origin, span), edgeAnchor(uvB, origin, span),
                    smoothstep(0., 1., t - slot));
                float distance = length(p - nearest) / uSize.x;
                float spill = .86 * exp2(-6.6 * distance) + .38 * exp2(-1.85 * distance);
                vec3 color = mix(edge, uHeldGlow, uHold);
                float peak = max(color.r, max(color.g, color.b));
                vec3 glow = color * (1.02 + .28 * (1. - peak)) * (spill * uLight);
                glow *= min(1., .33 / max(dot(glow, vec3(.2126, .7152, .0722)), .001));
                if (uHold < .5) {
                    float edgeLength = horizontal ? visible.z : visible.w;
                    glow += shaft((p - visible.xy) / visible.zw, horizontal, positive, edgeLength, distance) * uLight * 1.28;
                }
                float lightLimit = .84 + .14 * exp2(-4. * distance);
                glow *= min(1., lightLimit / max(max(glow.r, max(glow.g, glow.b)), .001));
                gl_FragColor = vec4(vec3(.012) + glow, 1.);
            }
            """;
}
