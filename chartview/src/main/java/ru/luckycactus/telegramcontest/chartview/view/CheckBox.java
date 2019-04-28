package ru.luckycactus.telegramcontest.chartview.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.View;

import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.dp;
import static ru.luckycactus.telegramcontest.chartview.common.Utils.dpF;

public class CheckBox extends View {

    private RectF rectF;

    private Bitmap drawBitmap;
    private Canvas drawCanvas;

    private float progress;
    private ValueAnimator checkAnimator;

    private boolean attachedToWindow;
    private boolean isChecked;

    private final Paint checkboxSquare_backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkboxSquare_checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkboxSquare_eraserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CheckBox(Context context) {
        super(context);
        rectF = new RectF();
        drawBitmap = Bitmap.createBitmap(dp(18), dp(18), Bitmap.Config.ARGB_4444);
        drawCanvas = new Canvas(drawBitmap);

        checkboxSquare_checkPaint.setStyle(Paint.Style.STROKE);
        checkboxSquare_checkPaint.setStrokeWidth(dp(2));

        checkboxSquare_eraserPaint.setColor(0);
        checkboxSquare_eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        updateTheme();
    }

    public void setProgress(float value) {
        if (progress == value) {
            return;
        }
        progress = value;
        invalidate();
    }

    public void setColor(int color) {
        checkboxSquare_backgroundPaint.setColor(color);
        invalidate();
    }

    public void updateTheme() {
        checkboxSquare_checkPaint.setColor(ChartTheme.checkColor);
        invalidate();
    }

    private void cancelCheckAnimator() {
        if (checkAnimator != null) {
            checkAnimator.cancel();
        }
    }

    private void animateToCheckedState(boolean newCheckedState) {
        checkAnimator = ValueAnimator.ofFloat(progress, newCheckedState ? 1 : 0);
        checkAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setProgress((float) animation.getAnimatedValue());
            }
        });
        checkAnimator.setDuration(300);
        checkAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attachedToWindow = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setChecked(boolean checked, boolean animated) {
        if (checked == isChecked) {
            return;
        }
        isChecked = checked;
        if (attachedToWindow && animated) {
            animateToCheckedState(checked);
        } else {
            cancelCheckAnimator();
            setProgress(checked ? 1.0f : 0.0f);
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibility() != VISIBLE) {
            return;
        }

        float checkProgress;
        float bounceProgress;
        if (progress <= 0.5f) {
            bounceProgress = checkProgress = progress / 0.5f;
        } else {
            bounceProgress = 2.0f - progress / 0.5f;
            checkProgress = 1.0f;
        }
        float bounce = dp(1) * bounceProgress;
        rectF.set(bounce, bounce, dp(18) - bounce, dp(18) - bounce);

        drawBitmap.eraseColor(0);
        drawCanvas.drawRoundRect(rectF, dp(2), dp(2), checkboxSquare_backgroundPaint);

        if (checkProgress != 1) {
            float rad = Math.min(dp(7), dp(7) * checkProgress + bounce);
            rectF.set(dp(2) + rad, dp(2) + rad, dp(16) - rad, dp(16) - rad);
            drawCanvas.drawRect(rectF, checkboxSquare_eraserPaint);
        }

        if (progress > 0.5f) {
            int endX = (int) (dp(7.5f) - dp(5) * (1.0f - bounceProgress));
            int endY = (int) (dpF(13.5f) - dp(5) * (1.0f - bounceProgress));
            drawCanvas.drawLine(dp(7.5f), (int) dpF(13.5f), endX, endY, checkboxSquare_checkPaint);
            endX = (int) (dpF(6.5f) + dp(9) * (1.0f - bounceProgress));
            endY = (int) (dpF(13.5f) - dp(9) * (1.0f - bounceProgress));
            drawCanvas.drawLine((int) dpF(6.5f), (int) dpF(13.5f), endX, endY, checkboxSquare_checkPaint);
        }
        canvas.drawBitmap(drawBitmap, 0, 0, null);
    }
}
