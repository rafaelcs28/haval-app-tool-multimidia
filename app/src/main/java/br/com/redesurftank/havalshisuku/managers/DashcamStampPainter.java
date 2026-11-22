package br.com.redesurftank.havalshisuku.managers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Draws the stamp over a transparent frame-sized canvas. Laid out at 1920×1080 and scaled to the
 * clip's size. Provisional layout; the final one follows the design spec.
 */
public final class DashcamStampPainter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final ZoneId zone = ZoneId.systemDefault();

    public void paint(Canvas c, int width, int height, DashcamStampFrame f, String address,
            DashcamStampJob.StampMap map, List<DashcamGpsTrack.Fix> fixes) {
        float s = width / 1920f;
        c.save();
        c.scale(s, s);
        float H = height / s;
        drawPanel(c, H, f, address);
        if (map != null && f.lat != null) drawMap(c, H, f, map, fixes);
        c.restore();
    }

    private void drawPanel(Canvas c, float H, DashcamStampFrame f, String address) {
        float left = 32, bottom = H - 32, top = bottom - 132, right = 1460;
        fill.setColor(0xB80B0E13);
        c.drawRoundRect(new RectF(left, top, right, bottom), 18, 18, fill);
        float x = left + 28;
        // Speed
        String speed = f.speedKmh == null ? "--" : String.valueOf(Math.round(f.speedKmh));
        text(c, speed, x, top + 86, 78, condensed, 0xFFFFFFFF);
        float sw = width(speed, 78, condensed);
        text(c, "km/h", x + sw + 8, top + 86, 22, medium, 0xCCFFFFFF);
        x += sw + 80;
        if (f.accelG != null) {
            int color = f.accelG <= -0.35f ? 0xFFEF4444 : (f.accelG <= -0.2f ? 0xFFFBBF24 : 0xFFFFFFFF);
            text(c, (f.accelG >= 0 ? "+" : "−") + DashcamStampFrame.decimal(Math.abs(f.accelG), 2) + " g", x, top + 62, 30, medium, color);
            text(c, f.accelG < -0.05f ? "freando" : (f.accelG > 0.05f ? "acelerando" : "constante"), x, top + 96, 20, regular, 0xB3FFFFFF);
            x += 170;
        }
        divider(c, x - 22, top + 24, bottom - 24);
        // Date / time / address
        Instant at = Instant.ofEpochMilli(f.utcMs);
        text(c, at.atZone(zone).format(DATE) + "  " + at.atZone(zone).format(TIME), x, top + 50, 26, medium, 0xFFFFFFFF);
        String place = address != null ? address
                : (f.lat != null ? String.format(java.util.Locale.ROOT, "%.5f, %.5f", f.lat, f.lon) : "");
        text(c, ellipsize(place), x, top + 84, 21, regular, 0xE6FFFFFF);
        String meta = (f.compass != null ? f.compass + " · " : "")
                + (f.lat != null ? String.format(java.util.Locale.ROOT, "%.5f, %.5f", f.lat, f.lon) : "");
        text(c, meta, x, top + 112, 17, regular, 0x99FFFFFF);
        x += 470;
        divider(c, x - 22, top + 24, bottom - 24);
        // Safety
        if (f.beltWarning != null) {
            text(c, f.beltWarning ? "Sem cinto" : "Cinto ok", x, top + 50, 24, medium, f.beltWarning ? 0xFFEF4444 : 0xFF35B98F);
        }
        String signal = f.hazard ? "Pisca-alerta" : (f.left ? "◀ Seta esq." : (f.right ? "Seta dir. ▶" : ""));
        if (!signal.isEmpty()) text(c, signal, x, top + 84, 22, medium, 0xFFFBBF24);
        String lights = f.highBeam ? "Farol alto" : (f.lowBeam ? "Farol baixo" : "");
        String gear = f.gear != null ? f.gear + (f.driveMode != null ? " · " + f.driveMode : "") : (f.driveMode != null ? f.driveMode : "");
        text(c, join(gear, lights, f.steerDeg != null ? "volante " + Math.round(f.steerDeg) + "°" : null), x, top + 112, 17, regular, 0xCCFFFFFF);
        x += 300;
        divider(c, x - 22, top + 24, bottom - 24);
        // Context
        if (f.soc != null) text(c, Math.round(f.soc) + "%", x, top + 56, 28, medium, 0xFFFFFFFF);
        if (f.powerKw != null) text(c, (f.powerKw < 0 ? "regen " : "") + Math.round(Math.abs(f.powerKw)) + " kW", x + 86, top + 56, 20, regular, 0xCCFFFFFF);
        String temps = (f.insideC != null ? Math.round(f.insideC) + "° int" : "") + (f.outsideC != null ? "  " + Math.round(f.outsideC) + "° ext" : "");
        text(c, temps.trim(), x, top + 90, 19, regular, 0xCCFFFFFF);
        text(c, "Impulse", x, top + 114, 14, medium, 0x80FFFFFF);
    }

    private void drawMap(Canvas c, float H, DashcamStampFrame f, DashcamStampJob.StampMap map,
            List<DashcamGpsTrack.Fix> fixes) {
        float w = 400, h = 270, right = 1920 - 32, bottom = H - 32;
        RectF box = new RectF(right - w, bottom - h, right, bottom);
        c.save();
        Path clip = new Path();
        clip.addRoundRect(box, 18, 18, Path.Direction.CW);
        c.clipPath(clip);
        float cx = map.x(f.lon), cy = map.y(f.lat);
        float ox = box.centerX() - cx, oy = box.centerY() - cy;
        c.drawBitmap(map.bitmap, ox, oy, bitmapPaint);
        fill.setColor(0x33000000);
        c.drawRect(box, fill);
        Path trail = new Path();
        boolean first = true;
        for (DashcamGpsTrack.Fix fx : fixes) {
            float px = map.x(fx.lon) + ox, py = map.y(fx.lat) + oy;
            if (first) trail.moveTo(px, py); else trail.lineTo(px, py);
            first = false;
        }
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(8);
        stroke.setColor(0xFF4A9EFF);
        c.drawPath(trail, stroke);
        // Car arrow
        c.save();
        c.translate(box.centerX(), box.centerY());
        if (f.bearing != null) c.rotate(f.bearing);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(0, 0, 18, fill);
        Path arrow = new Path();
        arrow.moveTo(0, -12);
        arrow.lineTo(9, 10);
        arrow.lineTo(0, 5);
        arrow.lineTo(-9, 10);
        arrow.close();
        fill.setColor(0xFF1F6FEB);
        c.drawPath(arrow, fill);
        c.restore();
        c.restore();
        stroke.setStrokeWidth(2);
        stroke.setColor(0x99FFFFFF);
        c.drawRoundRect(box, 18, 18, stroke);
        text(c, "© OpenStreetMap", box.right - 128, box.bottom - 10, 13, regular, 0xCCFFFFFF);
    }

    private void divider(Canvas c, float x, float y0, float y1) {
        fill.setColor(0x2EFFFFFF);
        c.drawRect(x, y0, x + 2, y1, fill);
    }

    private void text(Canvas c, String value, float x, float y, float size, Typeface face, int color) {
        if (value == null || value.isEmpty()) return;
        text.setTypeface(face);
        text.setTextSize(size);
        text.setColor(color);
        text.setShadowLayer(3, 0, 1, 0x99000000);
        c.drawText(value, x, y, text);
    }

    private float width(String value, float size, Typeface face) {
        text.setTypeface(face);
        text.setTextSize(size);
        return text.measureText(value);
    }

    private static String ellipsize(String value) {
        if (value == null) return "";
        return value.length() <= 52 ? value : value.substring(0, 51) + "…";
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(p);
        }
        return sb.toString();
    }
}
