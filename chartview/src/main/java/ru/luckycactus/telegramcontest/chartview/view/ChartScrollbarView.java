package ru.luckycactus.telegramcontest.chartview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;
import ru.luckycactus.telegramcontest.chartview.renderer.ChartRenderer;
import ru.luckycactus.telegramcontest.chartview.state.ChartAnimator;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.luckycactus.telegramcontest.chartview.view.ChartScrollbarView.SliderState.IDLING;
import static ru.luckycactus.telegramcontest.chartview.view.ChartScrollbarView.SliderState.LEFT_DRAGGING;
import static ru.luckycactus.telegramcontest.chartview.view.ChartScrollbarView.SliderState.MOVING;
import static ru.luckycactus.telegramcontest.chartview.view.ChartScrollbarView.SliderState.RIGHT_DRAGGING;

class ChartScrollbarView extends View {

    private static final int frameHorizontalWidth = Utils.dp(5);
    private static final int frameVerticalWidth = Utils.dp(1.5f);
    private static final float lineWidth = Utils.dpF(1);
    private static final float halfTouchZone = Utils.dpF(48) / 2f;
    private static final float minSelection = Utils.dpF(10);

    private final ChartState chartState = new ChartState();
    private final ChartTransformer chartTransformer = new ChartTransformer(chartState);
    private final ChartAnimator chartAnimator = new ChartAnimator(
        this,
        chartState,
        chartTransformer
    );
    private final ChartRenderer chartRenderer = new ChartRenderer(
        this,
        chartState,
        chartTransformer,
        lineWidth
    );
    private final Slider slider = new Slider();

    private OnSelectionChangedListener onSelectionChangedListener;

    public ChartScrollbarView(Context context) {
        this(context, null);
    }

    public ChartScrollbarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartScrollbarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        chartRenderer.enableBitmapCache();
        chartAnimator.setAnimationsListener(new ChartAnimator.AnimationsListener() {
            @Override
            public void onAnimationsStart() {
                chartRenderer.disableBitmapCache();
            }

            @Override
            public void onAnimationsEnd() {
                chartRenderer.enableBitmapCache();
            }
        });
        chartState.setSelection(0f, 1f);
    }

    public void setChartData(ChartData chartData) {
        if (chartState.chartData == chartData)
            return;

        chartAnimator.jumpToTargetState();
        chartState.setChartData(chartData);
        chartTransformer.prepareMatrix();
        chartAnimator.init();
        chartRenderer.init();

    }

    public void onCheckedChange(LineData lineData) {
        chartState.updateMaxVisibleEntry();
        chartAnimator.animateChartSize();
        chartAnimator.animateLineVisibility(lineData);
    }

    public void setSelection(float from, float to) {
        if (from < 0f || to > 1f || from >= to) {
            throw new IllegalArgumentException();
        }
        slider.from = from;
        slider.to = to;
        slider.measure();
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(from, to);
        }
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListener = onSelectionChangedListener;
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(slider.from, slider.to);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h;
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (hSpecMode == MeasureSpec.EXACTLY) {
            h = hSpecSize;
        } else {
            h = (int) (w * 0.12f);
            if (hSpecMode == MeasureSpec.AT_MOST) {
                h = min(h, hSpecSize);
            }
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float verticalPadding = Utils.dpF(3);
        chartAnimator.jumpToTargetState();
        chartState.setBounds(0, verticalPadding, w, h - verticalPadding);
        chartTransformer.prepareMatrix();
        chartRenderer.notifySizeChanged();
        slider.measure();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        chartRenderer.draw(canvas);
        slider.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return slider.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public float getFrom() {
        return slider.from;
    }

    public float getTo() {
        return slider.to;
    }

    public void updateTheme() {
        chartRenderer.updateTheme();
        invalidate();
    }

    private final class Slider {
        private final Paint paint = new Paint();

        private final RectF frameRect = new RectF();
        private final RectF frameInnerRect = new RectF();

        private float from = 0.75f;
        private float to = 1f;
        private SliderState state = SliderState.IDLING;
        private float touchX;
        private float touchY;
        private boolean scrollHandled = false;
        private boolean scrollIntercepted = false;

        void measure() {
            frameRect.set(
                from * getWidth(),
                0,
                to * getWidth(),
                getHeight()
            );
            updateInnerFrameRect();
        }

        void draw(Canvas canvas) {
            paint.setColor(ChartTheme.scrollbarOverlayColor);
            canvas.drawRect(0f, 0f, frameRect.left, getHeight(), paint);
            canvas.drawRect(frameRect.right, 0f, getWidth(), getHeight(), paint);

            paint.setColor(ChartTheme.scrollbarFrameColor);
            // left
            canvas.drawRect(frameRect.left, frameRect.top, frameInnerRect.left, frameRect.bottom, paint);
            // right
            canvas.drawRect(frameInnerRect.right, frameRect.top, frameRect.right, frameRect.bottom, paint);
            // top
            canvas.drawRect(frameInnerRect.left, frameRect.top, frameInnerRect.right, frameInnerRect.top, paint);
            // bottom
            canvas.drawRect(frameInnerRect.left, frameInnerRect.bottom, frameInnerRect.right, frameRect.bottom, paint);
        }

        boolean onTouchEvent(MotionEvent event) {
            float prevTouchX = touchX;
            float prevTouchY = touchY;
            touchX = event.getX();
            touchY = event.getY();

            boolean handled = false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    float leftStart = frameRect.left - halfTouchZone;
                    float leftEnd = frameRect.left + halfTouchZone;
                    float rightStart = frameRect.right - halfTouchZone;
                    float rightEnd = frameRect.right + halfTouchZone;
                    if (rightStart - leftEnd < 4 * halfTouchZone) {
                        float tmp = frameRect.width() / 5;
                        leftEnd = frameRect.left + tmp;
                        rightStart = frameRect.right - tmp;
                    }

                    handled = true;
                    if (touchX >= leftStart && touchX <= leftEnd) {
                        state = LEFT_DRAGGING;
                    } else if (touchX >= rightStart && touchX <= rightEnd) {
                        state = RIGHT_DRAGGING;
                    } else {
                        if (touchX < leftStart || touchX > rightEnd) {
                            handled = false;
                            state = IDLING;
                        } else {
                            state = MOVING;
                        }
                    }
                    scrollHandled = false;
                    scrollIntercepted = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    handled = true;
                    float dx = touchX - prevTouchX;

                    if (!scrollHandled) {
                        if (prevTouchX != touchX || prevTouchY != touchY) {
                            if (Math.abs(prevTouchY - touchY) > Math.abs(dx)) {
                                handled = false;
                            } else {
                                getParent().requestDisallowInterceptTouchEvent(true);
                                scrollIntercepted = true;
                            }
                            scrollHandled = true;
                        }
                    }

                    if (scrollIntercepted) {
                        switch (state) {
                            case LEFT_DRAGGING:
                                frameRect.left = max(0f, min(frameRect.right - minSelection, frameRect.left + dx));
                                updateInnerFrameRect();
                                updateSelection();
                                invalidate();
                                break;
                            case RIGHT_DRAGGING:
                                frameRect.right = min(getWidth(), max(frameRect.left + minSelection, frameRect.right + dx));
                                updateInnerFrameRect();
                                updateSelection();
                                invalidate();
                                break;
                            case MOVING:
                                shiftFrame(dx);
                                updateSelection();
                                invalidate();
                                break;
                            case IDLING:
                                handled = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    state = IDLING;
                    handled = true;
            }
            return handled;
        }

        private void updateInnerFrameRect() {
            frameInnerRect.set(frameRect);
            frameInnerRect.inset(frameHorizontalWidth, frameVerticalWidth);
        }

        private void updateSelection() {
            float newFrom = frameRect.left / getWidth();
            float newTo = frameRect.right / getWidth();
            if (newFrom != from || newTo != to) {
                from = newFrom;
                to = newTo;
                if (onSelectionChangedListener != null) {
                    onSelectionChangedListener.onSelectionChanged(from, to);
                }
            }
        }

        private void shiftFrame(float dx) {
            frameRect.offset(dx, 0f);
            if (frameRect.left < 0f) {
                frameRect.offset(-frameRect.left, 0f);
            } else if (frameRect.right > getWidth()) {
                frameRect.offset(getWidth() - frameRect.right, 0f);
            }
            updateInnerFrameRect();
        }
    }

    enum SliderState {
        IDLING,
        MOVING,
        LEFT_DRAGGING,
        RIGHT_DRAGGING
    }

    interface OnSelectionChangedListener {
        void onSelectionChanged(float from, float to);
    }
}
