package ru.luckycactus.telegramcontest.chartview.common;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

public class Utils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static void endAnimator(Animator animator) {
        if (animator != null && animator.isRunning()) {
            animator.end();
        }
    }

    public static void cancelAnimator(Animator animator) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    public static float sp(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }

    public static float dpF(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static int dp(float dp) {
        return (int) Math.floor(dpF(dp));
    }

    public static int getNumberOfDigits(int number) {
        if (number < 100000) {
            if (number < 100) {
                if (number < 10) {
                    return 1;
                } else {
                    return 2;
                }
            } else {
                if (number < 1000) {
                    return 3;
                } else {
                    if (number < 10000) {
                        return 4;
                    } else {
                        return 5;
                    }
                }
            }
        } else {
            if (number < 10000000) {
                if (number < 1000000) {
                    return 6;
                } else {
                    return 7;
                }
            } else {
                if (number < 100000000) {
                    return 8;
                } else {
                    if (number < 1000000000) {
                        return 9;
                    } else {
                        return 10;
                    }
                }
            }
        }
    }

    public static int getFloorPowerOfTen(int number) {
        if (number < 100000) {
            if (number < 100) {
                if (number < 10) {
                    return 1;
                } else {
                    return 10;
                }
            } else {
                if (number < 1000) {
                    return 100;
                } else {
                    if (number < 10000) {
                        return 1000;
                    } else {
                        return 10000;
                    }
                }
            }
        } else {
            if (number < 10000000) {
                if (number < 1000000) {
                    return 100000;
                } else {
                    return 1000000;
                }
            } else {
                if (number < 100000000) {
                    return 10000000;
                } else {
                    if (number < 1000000000) {
                        return 100000000;
                    } else {
                        return 1000000000;
                    }
                }
            }
        }
    }

    public static int getThemeColorOrThrow(Context context, int resId) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(resId, typedValue, true)) {
            return typedValue.data;
        } else {
            throw new IllegalArgumentException("Attribute not set on theme");
        }
    }

    public static int generateViewId() {
        if (Build.VERSION.SDK_INT >= 17) {
            return View.generateViewId();
        } else {
            int result;
            int newValue;
            do {
                result = sNextGeneratedId.get();
                newValue = result + 1;
                if (newValue > 16777215) {
                    newValue = 1;
                }
            } while (!sNextGeneratedId.compareAndSet(result, newValue));

            return result;
        }
    }

    public static void setElevation(View view, float elevation) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setElevation(elevation);
        }
    }
}
