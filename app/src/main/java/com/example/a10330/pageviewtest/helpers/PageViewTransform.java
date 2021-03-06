package com.example.a10330.pageviewtest.helpers;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class PageViewTransform {
    private int startDelay = 0;
    public int translationY = 0;
    public float translationZ = 0;
    public float scale = 1f;
    private float alpha = 1f;
    public boolean visible = false;
    public Rect rect = new Rect();
    public float p = 0f;// TODO: 2017/11/12 干什么用的

    public PageViewTransform() {
        // Do nothing
    }
    /**
     * Applies this transform to a view.
     */
    public void applyToPageView(View v, int duration, Interpolator interp, boolean allowLayers,
                                boolean allowShadows, ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Check to see if any properties have changed, and update the task view
        if (duration > 0) {
            ViewPropertyAnimator anim = v.animate();
            boolean requiresLayers = false;
            // Animate to the final state
            if (hasTranslationYChangedFrom(v.getTranslationY())) {
                anim.translationY(translationY);
            }
            if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
                anim.translationZ(translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                anim.scaleX(scale)//如果去掉x方向不会变化，就像新版本安卓一样
                        .scaleY(scale);
                requiresLayers = true;
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                // Use layers if we animate alpha
                anim.alpha(alpha);
                requiresLayers = true;
            }
            if (requiresLayers && allowLayers) {
                anim.withLayer();
            }
            if (updateCallback != null) {
                anim.setUpdateListener(updateCallback);
            } else {
                anim.setUpdateListener(null);
            }
            anim.setStartDelay(startDelay)
                    .setDuration(duration)
                    .setInterpolator(interp)
                    .start();
        } else {
            // Set the changed properties
            if (hasTranslationYChangedFrom(v.getTranslationY())) {
                v.setTranslationY(translationY);
            }
            if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
                v.setTranslationZ(translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                v.setScaleX(scale);//如果去掉x方向不会变化，就像新版本安卓一样
                v.setScaleY(scale);
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                v.setAlpha(alpha);
            }
        }
    }
    /**
     * Resets the current transform
     */
    public void reset() {
        startDelay = 0;
        translationY = 0;
        translationZ = 0;
        scale = 1f;
        alpha = 1f;
        visible = false;
        rect.setEmpty();
        p = 0f;
    }
    /**
     * Reset the transform on a view.
     */
    public static void reset(View v) {
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setTranslationZ(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setAlpha(1f);
    }
    /**
     * Convenience functions to compare against current property values
     */
    private boolean hasAlphaChangedFrom(float v) {
        return (Float.compare(alpha, v) != 0);
    }
    private boolean hasScaleChangedFrom(float v) {
        return (Float.compare(scale, v) != 0);
    }
    private boolean hasTranslationYChangedFrom(float v) {
        return (Float.compare(translationY, v) != 0);
    }
    private boolean hasTranslationZChangedFrom(float v) {
        return (Float.compare(translationZ, v) != 0);
    }
    @Override
    public String toString() {
        return "pageViewTransform delay: " + startDelay + " y: " + translationY + " z: " + translationZ +
                " scale: " + scale + " alpha: " + alpha + " visible: " + visible + " rect: " + rect +
                " p: " + p;
    }

}
