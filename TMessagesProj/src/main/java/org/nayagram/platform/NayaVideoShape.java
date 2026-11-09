package org.nayagram.platform;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * NayaVideoShape - Custom shapes generator for Telegram Round Video Messages (Video Notes).
 * Provides geometric Vector Paths for rendering video notes in shapes beyond the circle:
 * Circle, Heart, Star, Cat Silhouette, Flower, Cloud, Living Blob, and Seasonal Ghost.
 */
public class NayaVideoShape {

    /**
     * Generates a closed Android Path scaled to the given bounding box (w, h).
     */
    public static Path getShapePath(int shape, float w, float h) {
        Path path = new Path();
        if (w <= 0 || h <= 0) {
            return path;
        }

        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f;

        switch (shape) {
            case NayaConfig.VIDEO_SHAPE_HEART:
                // Smooth Heart Curve
                path.moveTo(cx, cy + r * 0.75f);
                path.cubicTo(cx - r * 1.2f, cy + r * 0.1f, cx - r * 1.1f, cy - r * 0.8f, cx, cy - r * 0.35f);
                path.cubicTo(cx + r * 1.1f, cy - r * 0.8f, cx + r * 1.2f, cy + r * 0.1f, cx, cy + r * 0.75f);
                path.close();
                break;

            case NayaConfig.VIDEO_SHAPE_STAR:
                // 5-Point Star
                int points = 5;
                float outerR = r * 0.95f;
                float innerR = r * 0.42f;
                double angleStep = Math.PI / points;
                double startAngle = -Math.PI / 2.0;

                for (int i = 0; i < points * 2; i++) {
                    float radius = (i % 2 == 0) ? outerR : innerR;
                    float x = (float) (cx + radius * Math.cos(startAngle + i * angleStep));
                    float y = (float) (cy + radius * Math.sin(startAngle + i * angleStep));
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.close();
                break;

            case NayaConfig.VIDEO_SHAPE_CAT:
                // Cute Cat Silhouette (Circle with pointed ears)
                path.addCircle(cx, cy + r * 0.08f, r * 0.88f, Path.Direction.CW);
                // Left Ear
                Path leftEar = new Path();
                leftEar.moveTo(cx - r * 0.72f, cy - r * 0.35f);
                leftEar.lineTo(cx - r * 0.85f, cy - r * 0.95f);
                leftEar.lineTo(cx - r * 0.22f, cy - r * 0.75f);
                leftEar.close();
                // Right Ear
                Path rightEar = new Path();
                rightEar.moveTo(cx + r * 0.72f, cy - r * 0.35f);
                rightEar.lineTo(cx + r * 0.85f, cy - r * 0.95f);
                rightEar.lineTo(cx + r * 0.22f, cy - r * 0.75f);
                rightEar.close();

                path.op(leftEar, Path.Op.UNION);
                path.op(rightEar, Path.Op.UNION);
                break;

            case NayaConfig.VIDEO_SHAPE_FLOWER:
                // 6-Petal Flower
                int petals = 6;
                path.moveTo(cx, cy);
                for (int i = 0; i < petals; i++) {
                    double pAngle = i * (2.0 * Math.PI / petals);
                    float px = (float) (cx + r * 0.5f * Math.cos(pAngle));
                    float py = (float) (cy + r * 0.5f * Math.sin(pAngle));
                    path.addCircle(px, py, r * 0.45f, Path.Direction.CW);
                }
                path.addCircle(cx, cy, r * 0.4f, Path.Direction.CW);
                break;

            case NayaConfig.VIDEO_SHAPE_CLOUD:
                // Fluffy Cloud Shape
                path.addCircle(cx - r * 0.35f, cy + r * 0.15f, r * 0.48f, Path.Direction.CW);
                path.addCircle(cx + r * 0.35f, cy + r * 0.15f, r * 0.48f, Path.Direction.CW);
                path.addCircle(cx, cy - r * 0.25f, r * 0.62f, Path.Direction.CW);
                path.addCircle(cx - r * 0.55f, cy - r * 0.05f, r * 0.38f, Path.Direction.CW);
                path.addCircle(cx + r * 0.55f, cy - r * 0.05f, r * 0.38f, Path.Direction.CW);
                break;

            case NayaConfig.VIDEO_SHAPE_BLOB:
                // Organic Living Blob
                path.moveTo(cx, cy - r * 0.9f);
                path.cubicTo(cx + r * 0.95f, cy - r * 0.85f, cx + r * 1.05f, cy + r * 0.2f, cx + r * 0.6f, cy + r * 0.88f);
                path.cubicTo(cx + r * 0.1f, cy + r * 1.05f, cx - r * 0.7f, cy + r * 0.9f, cx - r * 0.92f, cy + r * 0.3f);
                path.cubicTo(cx - r * 1.05f, cy - r * 0.3f, cx - r * 0.6f, cy - r * 0.95f, cx, cy - r * 0.9f);
                path.close();
                break;

            case NayaConfig.VIDEO_SHAPE_GHOST:
                // Ghost Silhouette
                path.moveTo(cx, cy - r * 0.92f);
                path.cubicTo(cx + r * 0.85f, cy - r * 0.92f, cx + r * 0.85f, cy + r * 0.3f, cx + r * 0.8f, cy + r * 0.7f);
                path.lineTo(cx + r * 0.5f, cy + r * 0.92f);
                path.lineTo(cx + r * 0.2f, cy + r * 0.72f);
                path.lineTo(cx, cy + r * 0.92f);
                path.lineTo(cx - r * 0.2f, cy + r * 0.72f);
                path.lineTo(cx - r * 0.5f, cy + r * 0.92f);
                path.lineTo(cx - r * 0.8f, cy + r * 0.7f);
                path.cubicTo(cx - r * 0.85f, cy + r * 0.3f, cx - r * 0.85f, cy - r * 0.92f, cx, cy - r * 0.92f);
                path.close();
                break;

            case NayaConfig.VIDEO_SHAPE_CIRCLE:
            default:
                path.addCircle(cx, cy, r, Path.Direction.CW);
                break;
        }

        return path;
    }
}
