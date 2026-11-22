package br.com.redesurftank.havalshisuku.managers;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.core.graphics.PathParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The stamp, option B "Cápsulas" (design spec in docs/architecture/dashcam-stamp-spec.md): two
 * rounded capsules in the bottom corners. Left: speed with the road limit, acceleration/braking in
 * g, power flow in kW and %, safety row (belt, indicators, hazard, lights, hand brake, gear,
 * steering), engine/locks/door. Right: north-up night-styled mini-map, time, address, context
 * (mode, battery, temperatures, odometer) and the evidence line (original's SHA-256, km in the
 * minute). Alerts (over the limit, speed camera) above the left capsule; EVENTO badge top right.
 * Laid out on a 1920×1080 grid scaled by min(W/1920, H/1080), capsules anchored to the corners.
 */
public final class DashcamStampPainter {
    /** Per-clip values the frame does not carry. */
    public static final class Extras {
        final String hash;
        final float distanceM;

        public Extras(String hash, float distanceM) {
            this.hash = hash;
            this.distanceM = distanceM;
        }
    }

    /** Overall stamp size relative to the design grid (spec at 100 %). */
    static final float SIZE = 0.6f;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int W2 = 0x9EFFFFFF;
    private static final int LABEL = 0x8CFFFFFF;
    private static final int INACTIVE = 0x3DFFFFFF;
    private static final int GREEN = 0xFF3EE08F;
    private static final int AMBER = 0xFFFFB020;
    private static final int RED = 0xFFFF4A4A;
    private static final int BLUE = 0xFF5AA9FF;
    private static final int CYAN = 0xFF4FD1FF;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint mapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Typeface condBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Typeface cond = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private final Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
    private final Typeface monoMedium = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    private final ZoneId zone = ZoneId.systemDefault();
    private final Map<String, Path> icons = new HashMap<>();
    private final Map<String, Path> iconFills = new HashMap<>();

    public DashcamStampPainter() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        text.setFontFeatureSettings("tnum");
        mapPaint.setColorFilter(new ColorMatrixColorFilter(nightMatrix()));
        defineIcons();
    }

    public void paint(Canvas c, int width, int height, DashcamStampFrame f, String address,
            DashcamStampJob.StampMap map, List<DashcamGpsTrack.Fix> fixes, Extras extras) {
        float s = Math.min(width / 1920f, height / 1080f);
        float dy = height / s - 1080f;
        float dxR = width / s - 1920f;
        c.save();
        c.scale(s, s);
        c.translate(0, dy);
        // Owner: smaller stamp (60 %), docked flush in the bottom corners and nearly opaque, so the
        // left capsule hides the camera's own "4K / date" burn-in in the bottom-left corner.
        c.save();
        c.translate(0, 1080);
        c.scale(SIZE, SIZE);
        c.translate(-36, -1044);
        capsule(c, 36, 776, 640, 268, LEFT_RADII);
        if (f.event) {
            stroke.setStrokeWidth(3);
            stroke.setColor(0xE6FF4A4A);
            Path outline = new Path();
            outline.addRoundRect(new RectF(37.5f, 777.5f, 674.5f, 1042.5f), LEFT_RADII, Path.Direction.CW);
            c.drawPath(outline, stroke);
        }
        drawLeft(c, f);
        drawAlerts(c, f);
        c.restore();
        c.save();
        c.translate(dxR + 1920, 1080);
        c.scale(SIZE, SIZE);
        c.translate(-1884, -1044);
        capsule(c, 1084, 776, 800, 268, RIGHT_RADII);
        drawMap(c, f, map, fixes);
        drawRight(c, f, address, extras);
        c.restore();
        c.restore();
        if (f.event) {
            c.save();
            c.scale(s, s);
            c.translate(dxR, 0);
            c.scale(SIZE, SIZE, 1920, 0);
            drawEvent(c);
            c.restore();
        }
    }

    // ---- left capsule ----

    private void drawLeft(Canvas c, DashcamStampFrame f) {
        boolean over = f.overLimit;
        String speed = f.speedKmh == null ? "—" : String.valueOf(Math.round(f.speedKmh));
        boolean three = speed.length() >= 3;
        textAt(c, speed, 62, 910, three ? 112 : 140, condBold, f.speedKmh == null ? 0x66FFFFFF : (over ? RED : WHITE), -3f / (three ? 112 : 140), false);
        textAt(c, "KM/H", 66, 944, 24, cond, W2, 1f / 24, false);
        if (f.limitKmh != null) {
            fill.setColor(WHITE);
            c.drawCircle(274, 916, 28, fill);
            stroke.setStrokeWidth(6);
            stroke.setColor(0xFFE02424);
            c.drawCircle(274, 916, 25, stroke);
            textCenter(c, String.valueOf(f.limitKmh), 274, 924, 24, condBold, 0xFF111111);
        }
        // Acceleration / braking
        Float g = f.accelG;
        int gColor = g == null ? 0x66FFFFFF : (g <= -0.5f ? RED : (g <= -0.3f ? AMBER : WHITE));
        textAt(c, g != null && g < 0 ? "FRENAGEM" : "ACELERAÇÃO", 322, 820, 17, condBold, LABEL, 2.5f / 17, false);
        textAt(c, g == null ? "—" : (g >= 0 ? "+" : "−") + DashcamStampFrame.decimal(Math.abs(g), 2) + " g", 652, 826, 38, condBold, gColor, 0, true);
        bar(c, 842, g == null ? 0 : Math.min(Math.abs(g) / 0.8f, 1f), g != null && g < 0, g != null && g < 0 ? gColor : WHITE);
        // Power flow
        Float kw = f.powerKw;
        Float pct = f.outputPct;
        boolean regen = (kw != null && kw < 0) || (kw == null && pct != null && pct < 0);
        boolean zero = (kw == null || Math.abs(kw) < 0.5f) && (pct == null || Math.abs(pct) < 0.5f);
        String label = kw == null && pct == null ? "POTÊNCIA" : (zero ? "POTÊNCIA" : (regen ? "REGENERANDO" : "ACELERANDO"));
        textAt(c, label, 322, 896, 17, condBold, LABEL, 2.5f / 17, false);
        int pColor = kw == null && pct == null ? 0x66FFFFFF : (zero ? W2 : (regen ? GREEN : WHITE));
        String kwText = kw == null ? "—" : Math.round(Math.abs(kw)) + " kW";
        String pctText = pct == null ? null : Math.round(Math.abs(pct)) + "%";
        drawPower(c, kwText, pctText, pColor);
        bar(c, 918, pct == null ? 0 : Math.min(Math.abs(pct) / 100f, 1f), regen, regen ? GREEN : WHITE);
        fill.setColor(0x1AFFFFFF);
        c.drawRect(60, 951.25f, 652, 952.75f, fill);
        drawSafetyRow(c, f);
        drawEngineRow(c, f);
    }

    private void drawPower(Canvas c, String kw, String pct, int color) {
        float x = 652;
        if (pct != null) {
            float w = measure(pct, 38, condBold, 0);
            textAt(c, pct, x - w, 902, 38, condBold, color, 0, false);
            x -= w;
            float sw = measure(" · ", 38, cond, 0);
            textAt(c, " · ", x - sw, 902, 38, cond, 0x99FFFFFF, 0, false);
            x -= sw;
        }
        textAt(c, kw, x, 902, 38, condBold, color, 0, true);
    }

    private void bar(Canvas c, float y, float fraction, boolean toLeft, int color) {
        fill.setColor(0x29FFFFFF);
        c.drawRoundRect(new RectF(322, y, 652, y + 8), 4, 4, fill);
        if (fraction > 0) {
            float w = fraction * 164;
            fill.setColor(color);
            RectF r = toLeft ? new RectF(487 - w, y, 487, y + 8) : new RectF(487, y, 487 + w, y + 8);
            c.drawRoundRect(r, 4, 4, fill);
        }
        fill.setColor(0xD9FFFFFF);
        c.drawRoundRect(new RectF(486, y - 5, 488, y + 13), 1, 1, fill);
    }

    private void drawSafetyRow(Canvas c, DashcamStampFrame f) {
        float x = 62, cy = 982;
        if (Boolean.TRUE.equals(f.beltWarning)) {
            x = chip(c, x, cy, 36, "belt", "SEM CINTO", 26, RED) + 20;
        } else {
            icon(c, "belt", x, cy, 34, f.beltWarning == null ? INACTIVE : WHITE);
            x += 34 + 20;
        }
        boolean blink = (f.utcMs / 500) % 2 == 0;
        icon(c, "left", x, cy, 34, (f.left || f.hazard) && blink ? GREEN : INACTIVE);
        x += 54;
        icon(c, "right", x, cy, 34, (f.right || f.hazard) && blink ? GREEN : INACTIVE);
        x += 54;
        icon(c, "hazard", x, cy, 32, f.hazard ? RED : INACTIVE);
        x += 52;
        if (f.highBeam) icon(c, "highbeam", x, cy, 34, BLUE);
        else icon(c, "lowbeam", x, cy, 34, f.lowBeam ? GREEN : INACTIVE);
        x += 54;
        icon(c, "handbrake", x, cy, 34, Boolean.TRUE.equals(f.handBrake) ? RED : INACTIVE);
        textCenter(c, "P", x + 17, cy + 5, 13, condBold, Boolean.TRUE.equals(f.handBrake) ? RED : INACTIVE);
        x += 54;
        String gear = f.gear == null ? "—" : f.gear;
        float gw = measure(gear, 26, condBold, 0) + 20;
        fill.setColor(f.gear == null ? INACTIVE : WHITE);
        c.drawRoundRect(new RectF(x, cy - 18, x + gw, cy + 18), 8, 8, fill);
        textCenter(c, gear, x + gw / 2, cy + 9, 26, condBold, 0xFF0C0E12);
        // Steering, right-aligned at 652
        String steer = f.steerDeg == null ? "—" : (f.steerDeg > 0 ? "+" : (f.steerDeg < 0 ? "−" : "")) + Math.abs(Math.round(f.steerDeg)) + "°";
        float sw = measure(steer, 26, cond, 0);
        textAt(c, steer, 652 - sw, cy + 9, 26, cond, 0xE0FFFFFF, 0, false);
        c.save();
        c.rotate(f.steerDeg == null ? 0 : f.steerDeg, 652 - sw - 8 - 16, cy);
        icon(c, "steer", 652 - sw - 8 - 32, cy, 32, 0xE0FFFFFF);
        c.restore();
    }

    private void drawEngineRow(Canvas c, DashcamStampFrame f) {
        float cy = 1020;
        boolean on = Boolean.TRUE.equals(f.engineOn);
        int color = f.engineOn == null ? 0x52FFFFFF : (on ? 0xE0FFFFFF : 0x52FFFFFF);
        icon(c, "engine", 62, cy, 24, color);
        String engine = f.engineOn == null ? "Motor —" : (on ? "Motor " + thousands(Math.round(f.rpm)) + " rpm" : "Motor desligado");
        textAt(c, engine, 94, 1028, 22, cond, color, 0, false);
        float x = 94 + measure(engine, 22, cond, 0) + 18;
        if (f.locked != null) {
            icon(c, f.locked ? "lock" : "unlock", x, cy, 22, LABEL);
            textAt(c, f.locked ? "Travado" : "Destravado", x + 28, 1028, 22, cond, LABEL, 0, false);
        }
        if (f.doorOpen) {
            String t = "PORTA ABERTA";
            float w = 10 + 20 + 6 + measure(t, 19, condBold, 1f / 19) + 10;
            chip(c, 652 - w, cy, 28, "door", t, 20, RED);
        }
    }

    /** Red (or given colour) chip with a white icon and label; returns its right edge. */
    private float chip(Canvas c, float x, float cy, float h, String iconName, String label, float iconSize, int bg) {
        float tw = measure(label, 19, condBold, 1f / 19);
        float w = 10 + iconSize + 6 + tw + 10;
        fill.setColor(bg);
        c.drawRoundRect(new RectF(x, cy - h / 2, x + w, cy + h / 2), 9, 9, fill);
        icon(c, iconName, x + 10, cy, iconSize, WHITE);
        textAt(c, label, x + 10 + iconSize + 6, cy + 7, 19, condBold, WHITE, 1f / 19, false);
        return x + w;
    }

    private void drawAlerts(Canvas c, DashcamStampFrame f) {
        float x = 36, top = 716, h = 44;
        if (f.overLimit && f.limitKmh != null) {
            String t = "ACIMA DO LIMITE · " + f.limitKmh;
            float w = 16 + 24 + 8 + measure(t, 22, condBold, 1f / 22) + 16;
            fill.setColor(0xF2FF4A4A);
            c.drawRoundRect(new RectF(x, top, x + w, top + h), 22, 22, fill);
            icon(c, "alert", x + 16, top + h / 2, 24, WHITE);
            textAt(c, t, x + 48, top + 30, 22, condBold, WHITE, 1f / 22, false);
            x += w + 10;
        }
        if (f.radarDistanceM != null) {
            int m = Math.round(f.radarDistanceM / 10f) * 10;
            String t = "Radar a " + m + " m" + (f.radarLimitKmh != null ? " · " + f.radarLimitKmh : "");
            float w = 16 + 24 + 8 + measure(t, 22, condBold, 1f / 22) + 16;
            fill.setColor(0xD10C0E12);
            RectF r = new RectF(x, top, x + w, top + h);
            c.drawRoundRect(r, 22, 22, fill);
            stroke.setStrokeWidth(2);
            stroke.setColor(AMBER);
            c.drawRoundRect(new RectF(x + 1, top + 1, x + w - 1, top + h - 1), 21, 21, stroke);
            icon(c, "radar", x + 16, top + h / 2, 24, AMBER);
            textAt(c, t, x + 48, top + 30, 22, condBold, AMBER, 1f / 22, false);
        }
    }

    // ---- right capsule ----

    private void drawMap(Canvas c, DashcamStampFrame f, DashcamStampJob.StampMap map, List<DashcamGpsTrack.Fix> fixes) {
        RectF box = new RectF(1100, 792, 1336, 1028);
        c.save();
        Path clip = new Path();
        clip.addRoundRect(box, 20, 20, Path.Direction.CW);
        c.clipPath(clip);
        fill.setColor(0xFF1A1E24);
        c.drawRect(box, fill);
        float scale = 0.85f;
        if (map != null && f.lat != null) {
            float cx = map.x(f.lon), cy = map.y(f.lat);
            Matrix m = new Matrix();
            m.postTranslate(-cx, -cy);
            m.postScale(scale, scale);
            m.postTranslate(box.centerX(), box.centerY());
            c.drawBitmap(map.bitmap, m, mapPaint);
            // Trail: past solid with glow, rest dashed.
            Path past = new Path(), next = new Path();
            boolean p0 = true, n0 = true;
            for (DashcamGpsTrack.Fix fx : fixes) {
                float px = (map.x(fx.lon) - cx) * scale + box.centerX();
                float py = (map.y(fx.lat) - cy) * scale + box.centerY();
                if (fx.utcMs <= f.utcMs) {
                    if (p0) past.moveTo(px, py); else past.lineTo(px, py);
                    p0 = false;
                } else {
                    if (n0) {
                        next.moveTo(box.centerX(), box.centerY());
                        n0 = false;
                    }
                    next.lineTo(px, py);
                }
            }
            if (!p0) past.lineTo(box.centerX(), box.centerY());
            stroke.setColor(0x734FD1FF);
            stroke.setStrokeWidth(4.4f);
            stroke.setPathEffect(new DashPathEffect(new float[]{2, 7.6f}, 0));
            c.drawPath(next, stroke);
            stroke.setPathEffect(null);
            stroke.setColor(0x474FD1FF);
            stroke.setStrokeWidth(12);
            c.drawPath(past, stroke);
            stroke.setColor(CYAN);
            stroke.setStrokeWidth(4.7f);
            c.drawPath(past, stroke);
            // Car arrow
            c.save();
            c.translate(box.centerX(), box.centerY());
            fill.setColor(0x2E4FD1FF);
            c.drawCircle(0, 0, 25, fill);
            if (f.bearing != null) c.rotate(f.bearing);
            Path arrow = new Path();
            arrow.moveTo(0, -16);
            arrow.lineTo(12, 13);
            arrow.lineTo(0, 7);
            arrow.lineTo(-12, 13);
            arrow.close();
            fill.setColor(WHITE);
            c.drawPath(arrow, fill);
            stroke.setColor(0xFF0B1016);
            stroke.setStrokeWidth(2.5f);
            c.drawPath(arrow, stroke);
            c.restore();
        }
        c.restore();
        stroke.setColor(0x29FFFFFF);
        stroke.setStrokeWidth(1.5f);
        c.drawRoundRect(new RectF(box.left + 0.75f, box.top + 0.75f, box.right - 0.75f, box.bottom - 0.75f), 19, 19, stroke);
        // N badge
        fill.setColor(0xD9080A0E);
        c.drawCircle(1125, 817, 15, fill);
        stroke.setColor(0x33FFFFFF);
        c.drawCircle(1125, 817, 15, stroke);
        textCenter(c, "N", 1125, 823, 17, condBold, WHITE);
        if (f.bearing != null) {
            String h = compass16(f.bearing) + " " + Math.round(((f.bearing % 360) + 360) % 360) + "°";
            fill.setColor(0xD9080A0E);
            c.drawRoundRect(new RectF(1168, 988, 1268, 1018), 15, 15, fill);
            textCenter(c, h, 1218, 1010, 20, condBold, WHITE);
        }
    }

    private void drawRight(Canvas c, DashcamStampFrame f, String address, Extras extras) {
        Instant at = Instant.ofEpochMilli(f.utcMs);
        textAt(c, at.atZone(zone).format(TIME), 1360, 834, 38, monoMedium, WHITE, 0, false);
        textAt(c, at.atZone(zone).format(DATE), 1564, 834, 25, mono, W2, 0, false);
        textAt(c, "IMPULSE", 1858, 825, 15, condBold, 0x66FFFFFF, 5f / 15, true);
        String street;
        String district = null;
        if (address != null && !address.isEmpty()) {
            int sep = address.indexOf(" · ");
            street = sep > 0 ? address.substring(0, sep) : address;
            if (sep > 0) district = address.substring(sep + 3);
        } else {
            street = f.lat == null ? "Sem sinal de GPS" : "Endereço indisponível";
        }
        text.setTypeface(condBold);
        text.setTextSize(44);
        text.setLetterSpacing(0);
        textAt(c, TextUtils.ellipsize(street, text, 499, TextUtils.TruncateAt.END).toString(), 1359, 891, 44, condBold, WHITE, 0, false);
        if (district != null) {
            text.setTypeface(cond);
            text.setTextSize(27);
            textAt(c, TextUtils.ellipsize(district, text, 250, TextUtils.TruncateAt.END).toString(), 1360, 929, 27, cond, 0xBFFFFFFF, 0, false);
        }
        if (f.lat != null) {
            String coords = String.format(Locale.ROOT, "%.5f, %.5f", f.lat, f.lon).replace('-', '−');
            textAt(c, coords, 1858, 928, 19, mono, 0x80FFFFFF, 0, true);
        }
        // Context bar
        fill.setColor(0x12FFFFFF);
        c.drawRoundRect(new RectF(1354, 948, 1868, 992), 14, 14, fill);
        String[] items = {
                f.driveMode == null ? "—" : f.driveMode,
                f.soc == null ? "—" : Math.round(f.soc) + "%",
                (f.insideC == null ? "—" : Math.round(f.insideC) + "° int") + " · " + (f.outsideC == null ? "—" : Math.round(f.outsideC) + "° ext"),
                f.odometerKm == null ? "—" : thousands(Math.round(f.odometerKm)) + " km",
        };
        String[] itemIcons = {null, "battery", "temp", "odo"};
        float[] widths = new float[4];
        float total = 0;
        for (int i = 0; i < 4; i++) {
            widths[i] = measure(items[i], 24, cond, 0) + (itemIcons[i] != null ? 28 : 0);
            total += widths[i];
        }
        float gap = (514 - 32 - total) / 3f;
        float x = 1370;
        for (int i = 0; i < 4; i++) {
            if (itemIcons[i] != null) icon(c, itemIcons[i], x, 970, 22, 0xE0FFFFFF);
            textAt(c, items[i], x + (itemIcons[i] != null ? 28 : 0), 978, 24, cond, 0xE0FFFFFF, 0, false);
            x += widths[i];
            if (i < 3) {
                fill.setColor(0x24FFFFFF);
                c.drawRect(x + gap / 2 - 0.75f, 959, x + gap / 2 + 0.75f, 981, fill);
                x += gap;
            }
        }
        // Evidence line
        StringBuilder ev = new StringBuilder();
        if (extras != null && extras.hash != null) ev.append("Original SHA-256 ").append(extras.hash);
        if (extras != null && extras.distanceM > 0) {
            if (ev.length() > 0) ev.append(" · ");
            ev.append(DashcamStampFrame.decimal(extras.distanceM / 1000f, 1)).append(" km no minuto");
        }
        textAt(c, ev.toString(), 1362, 1020, 17, mono, 0x6BFFFFFF, 0, false);
    }

    private void drawEvent(Canvas c) {
        String t = "EVENTO";
        float tw = measure(t, 26, condBold, 3f / 26);
        float w = 20 + 12 + 10 + tw + 20;
        float right = 1884, top = 32;
        fill.setColor(0xEBDC2626);
        c.drawRoundRect(new RectF(right - w, top, right, top + 46), 23, 23, fill);
        fill.setColor(WHITE);
        c.drawCircle(right - w + 26, top + 23, 6, fill);
        textAt(c, t, right - w + 42, top + 32, 26, condBold, WHITE, 3f / 26, false);
    }

    // ---- primitives ----

    /** Rounded only on the corner that faces the picture; square where it meets the frame edges. */
    private static final float[] LEFT_RADII = {0, 0, 30, 30, 0, 0, 0, 0};
    private static final float[] RIGHT_RADII = {30, 30, 0, 0, 0, 0, 0, 0};

    private void capsule(Canvas c, float x, float y, float w, float h, float[] radii) {
        Path body = new Path();
        body.addRoundRect(new RectF(x, y, x + w, y + h), radii, Path.Direction.CW);
        // 94 % opaque: dense enough to hide the camera's burn-in underneath.
        fill.setShader(new LinearGradient(0, y, 0, y + h, 0xF01A1D24, 0xF0090B0E, Shader.TileMode.CLAMP));
        c.drawPath(body, fill);
        fill.setShader(null);
        Path edge = new Path();
        edge.addRoundRect(new RectF(x + 0.75f, y + 0.75f, x + w - 0.75f, y + h - 0.75f), radii, Path.Direction.CW);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(0x1FFFFFFF);
        c.drawPath(edge, stroke);
    }

    private void textAt(Canvas c, String value, float x, float baseline, float size, Typeface face, int color,
            float letterSpacingEm, boolean alignRight) {
        if (value == null || value.isEmpty()) return;
        text.setTypeface(face);
        text.setTextSize(size);
        text.setLetterSpacing(letterSpacingEm);
        text.setColor(color);
        text.setTextAlign(alignRight ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(value, x, baseline, text);
        text.setTextAlign(Paint.Align.LEFT);
        text.setLetterSpacing(0);
    }

    private void textCenter(Canvas c, String value, float cx, float baseline, float size, Typeface face, int color) {
        text.setTypeface(face);
        text.setTextSize(size);
        text.setLetterSpacing(0);
        text.setColor(color);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(value, cx, baseline, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private float measure(String value, float size, Typeface face, float letterSpacingEm) {
        text.setTypeface(face);
        text.setTextSize(size);
        text.setLetterSpacing(letterSpacingEm);
        float w = text.measureText(value);
        text.setLetterSpacing(0);
        return w;
    }

    /** Draws a 24-grid icon with its left edge at x, vertically centred on cy. */
    private void icon(Canvas c, String name, float x, float cy, float size, int color) {
        Path strokes = icons.get(name);
        Path fills = iconFills.get(name);
        c.save();
        c.translate(x, cy - size / 2);
        c.scale(size / 24f, size / 24f);
        if (fills != null) {
            fill.setColor(color);
            c.drawPath(fills, fill);
        }
        if (strokes != null) {
            stroke.setColor(color);
            stroke.setStrokeWidth(1.9f);
            c.drawPath(strokes, stroke);
        }
        c.restore();
    }

    private void defineIcons() {
        put("belt", "M7 21v-7.5a5 5 0 0 1 10 0V21 M8 8.5L16.5 19", circle(12, 4.2f, 2.3f));
        put("left", null, "M10.5 4L3 12l7.5 8v-5H21V9H10.5z");
        put("right", null, "M13.5 4L21 12l-7.5 8v-5H3V9h10.5z");
        put("hazard", "M12 3L22 20H2z M12 8.5L17 17.5H7z", null);
        put("lowbeam", "M11 5.5C6 5.5 4 8.6 4 12s2 6.5 7 6.5z M14 7.5l7 2.5M14 12l7 2.5M14 16.5l7 2.5", null);
        put("highbeam", "M14 7h7M14 10.3h7M14 13.7h7M14 17h7", "M11 5.5C6 5.5 4 8.6 4 12s2 6.5 7 6.5z");
        put("handbrake", circle(12, 12, 6.5f) + " M4.2 6.5a10 10 0 0 0 0 11M19.8 6.5a10 10 0 0 1 0 11", null);
        put("steer", circle(12, 12, 9) + " M3.3 10.5h6.3M14.4 10.5h6.3M12 14.4V21", circle(12, 12, 2.4f));
        put("battery", rect(3, 7, 16, 10, 2) + " M21 10.5v3", null);
        put("temp", "M10 4a2 2 0 0 1 4 0v9.5a4 4 0 1 1-4 0z", circle(12, 16.5f, 1.6f));
        put("odo", "M4 17a8 8 0 1 1 16 0 M12 17l4-5", null);
        put("engine", "M4 10v6M4 13h2M6 9h3V7h5v2h3l2 2h2v6h-2l-2 2H9l-3-3V9z", null);
        put("door", "M6 21V8l6-5h7v18z M7.5 10h10M15 14h2", null);
        put("lock", rect(5, 11, 14, 10, 2) + " M8 11V8a4 4 0 0 1 8 0v3", null);
        put("unlock", rect(5, 11, 14, 10, 2) + " M8 11V8a4 4 0 0 1 7.6-1.7", null);
        put("radar", rect(7, 7, 10, 9, 1.5f) + " " + circle(12, 11.5f, 2.4f) + " M12 16v5M9 21h6", null);
        put("alert", null, "M12 3L22 20H2z");
    }

    private void put(String name, String strokeData, String fillData) {
        if (strokeData != null) icons.put(name, PathParser.createPathFromPathData(strokeData));
        if (fillData != null) iconFills.put(name, PathParser.createPathFromPathData(fillData));
    }

    static String circle(float cx, float cy, float r) {
        return String.format(Locale.ROOT, "M%.2f %.2fa%.2f %.2f 0 1 0 %.2f 0a%.2f %.2f 0 1 0 %.2f 0z",
                cx - r, cy, r, r, 2 * r, r, r, -2 * r);
    }

    static String rect(float x, float y, float w, float h, float r) {
        return String.format(Locale.ROOT,
                "M%.2f %.2fh%.2fa%.2f %.2f 0 0 1 %.2f %.2fv%.2fa%.2f %.2f 0 0 1 %.2f %.2fh%.2fa%.2f %.2f 0 0 1 %.2f %.2fv%.2fa%.2f %.2f 0 0 1 %.2f %.2fz",
                x + r, y, w - 2 * r, r, r, r, r, h - 2 * r, r, r, -r, r, -(w - 2 * r), r, r, -r, -r, -(h - 2 * r), r, r, r, -r);
    }

    /** 16-point compass in Portuguese (L = leste, O = oeste). */
    static String compass16(float bearing) {
        String[] n = {"N", "NNE", "NE", "ENE", "L", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"};
        int i = (int) Math.floor(((bearing % 360 + 360) % 360 + 11.25) / 22.5) % 16;
        return n[i];
    }

    static String thousands(long v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', '.');
    }

    /** Night map: invert → saturation 0.45 → scale 0.72 (spec 3.1). */
    static ColorMatrix nightMatrix() {
        ColorMatrix inv = new ColorMatrix(new float[]{-1, 0, 0, 0, 255, 0, -1, 0, 0, 255, 0, 0, -1, 0, 255, 0, 0, 0, 1, 0});
        ColorMatrix sat = new ColorMatrix();
        sat.setSaturation(0.45f);
        ColorMatrix scale = new ColorMatrix();
        scale.setScale(0.72f, 0.72f, 0.72f, 1f);
        ColorMatrix m = new ColorMatrix(inv);
        m.postConcat(sat);
        m.postConcat(scale);
        return m;
    }
}
