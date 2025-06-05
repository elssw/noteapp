package com.example.afinal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BubbleView extends View {

    private static class Bubble {
        String name;
        float amount;
        int color;

        Bubble(String name, float amount, int color) {
            this.name = name;
            this.amount = amount;
            this.color = color;
        }
    }

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<String> names, List<Float> amounts) {
        bubbles.clear();
        for (int i = 0; i < names.size(); i++) {
            float amount = amounts.get(i);
            int color = amount >= 0 ? Color.parseColor("#2196F3") : Color.parseColor("#F44336");
            bubbles.add(new Bubble(names.get(i), amount, color));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bubbles.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;
        int spacing = width / (bubbles.size() + 1);

        for (int i = 0; i < bubbles.size(); i++) {
            Bubble b = bubbles.get(i);
            float radius = Math.max(50f, Math.min(150f, Math.abs(b.amount) * 2f));
            float cx = spacing * (i + 1);
            float cy = centerY;

            paint.setColor(b.color);
            canvas.drawCircle(cx, cy, radius, paint);

            canvas.drawText(b.name, cx, cy - 10, textPaint);
            canvas.drawText(String.format("$%.0f", b.amount), cx, cy + 30, textPaint);
        }
    }
}