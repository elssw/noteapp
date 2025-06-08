package com.example.afinal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BubbleView extends View {

    private static class Bubble {
        String name;
        float amount;
        int color;
        float cx, cy, radius;

        Bubble(String name, float amount, int color) {
            this.name = name;
            this.amount = amount;
            this.color = color;
        }
    }

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private static final float minRadius = 60f;
    private static final float maxRadius = 150f;

    public BubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<String> names, List<Float> amounts) {
        bubbles.clear();
        float maxAmount = 0f;

        // 找出最大金額（取絕對值）
        for (Float amt : amounts) {
            maxAmount = Math.max(maxAmount, Math.abs(amt));
        }

        // 建立泡泡資料
        for (int i = 0; i < names.size(); i++) {
            float amount = amounts.get(i);
            int color = amount >= 0 ? Color.parseColor("#2196F3") : Color.parseColor("#F44336");

            Bubble bubble = new Bubble(names.get(i), amount, color);

            // 根據比例設定半徑
            if (maxAmount > 0) {
                float scale = Math.abs(amount) / maxAmount;
                bubble.radius = minRadius + scale * (maxRadius - minRadius);
            } else {
                bubble.radius = minRadius; // 若全部都是 0
            }

            bubbles.add(bubble);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bubbles.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int maxTries = 300;

        List<Bubble> placed = new ArrayList<>();

        for (Bubble b : bubbles) {
            boolean placedSuccessfully = false;

            for (int attempt = 0; attempt < maxTries; attempt++) {
                float padding = 12f;
                float cx = b.radius + padding + random.nextFloat() * (width - 2 * b.radius - 2 * padding);
                float cy = b.radius + padding + random.nextFloat() * (height - 2 * b.radius - 2 * padding);

                boolean overlap = false;
                for (Bubble other : placed) {
                    float dx = cx - other.cx;
                    float dy = cy - other.cy;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < b.radius + other.radius + 12f) {
                        overlap = true;
                        break;
                    }
                }

                if (!overlap) {
                    b.cx = cx;
                    b.cy = cy;
                    placedSuccessfully = true;
                    break;
                }
            }

            if (placedSuccessfully) {
                placed.add(b);

                paint.setColor(b.color);
                canvas.drawCircle(b.cx, b.cy, b.radius, paint);

                canvas.drawText(b.name, b.cx, b.cy - 10, textPaint);
                canvas.drawText(String.format("$%.0f", b.amount), b.cx, b.cy + 30, textPaint);
            }
        }
    }
}
