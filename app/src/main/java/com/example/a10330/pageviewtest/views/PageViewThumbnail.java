package com.example.a10330.pageviewtest.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.example.a10330.pageviewtest.helpers.PageStackViewConfig;
import com.example.a10330.pageviewtest.utilities.DVUtils;

import java.util.Random;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class PageViewThumbnail extends View {
    private PageStackViewConfig mConfig;
    // Drawing
    private float mDimAlpha;
    private Matrix mScaleMatrix = new Matrix();
    private Paint mDrawPaint = new Paint();
    private RectF mBitmapRect = new RectF();
    private RectF mLayoutRect = new RectF();
    private BitmapShader mBitmapShader;
    private LightingColorFilter mLightingColorFilter = new LightingColorFilter(0xffFFFFFF, 0);
    // Thumbnail alpha
    private float mThumbnailAlpha;
    private ValueAnimator mThumbnailAlphaAnimator;
    private ValueAnimator.AnimatorUpdateListener mThumbnailAlphaUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mThumbnailAlpha = (float) animation.getAnimatedValue();
//            updateThumbnailPaintFilter();
        }
    };
    // Task bar clipping, the top of this thumbnail can be clipped against the opaque header
    // bar that overlaps this thumbnail
    private View mTaskBar;
    private Rect mClipRect = new Rect();
    // Visibility optimization, if the thumbnail height is less than the height of the header
    // bar for the task view, then just mark this thumbnail view as invisible
    private boolean mInvisible;

    public PageViewThumbnail(Context context) {
        this(context, null);
    }
    public PageViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public PageViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public PageViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
//        mConfig = PageStackViewConfig.getInstance();
//        mDrawPaint.setColorFilter(mLightingColorFilter);
//        mDrawPaint.setFilterBitmap(true);
        mDrawPaint.setAntiAlias(true);
    }
/*
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mThumbnailAlpha = mConfig.pageViewThumbnailAlpha;
//        updateThumbnailPaintFilter();
    }*/
/*
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
*//*        if (changed) {
            mLayoutRect.set(0, 0, getWidth(), getHeight());
//            updateThumbnailScale();
        }*//*
    }*/
    Random mRandom=new Random();
    int ranColor = 0xff000000 | mRandom.nextInt(0x00ffffff);
    @Override
    protected void onDraw(Canvas canvas) {
        if (mInvisible) {
            return;
        }
        mDrawPaint.setColor(ranColor);                        //设置画笔颜色
//        canvas.drawColor(Color.YELLOW);                 //设置背景颜色
        canvas.drawRect(0, 190, getWidth(), getHeight(), mDrawPaint);
//        canvas.drawBitmap(new Rect(0,200,getWidth(),getWidth()),new Rect(0,200,getWidth(),getWidth()),new Paint());
    }

    /**
     * Sets the thumbnail to a given bitmap.
     */
    void setThumbnail(Bitmap bm) {
/*        mThumbnail = bm;

        if (bm != null) {
            mBitmapShader = new BitmapShader(bm, Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP);
            mDrawPaint.setShader(mBitmapShader);
            mBitmapRect.set(0, 0, bm.getWidth(), bm.getHeight());
            updateThumbnailScale();
        } else {
            mBitmapShader = null;
            mDrawPaint.setShader(null);
        }
        updateThumbnailPaintFilter();*/
    }

    /**
     * Updates the paint to draw the thumbnail.
     */
/*    private void updateThumbnailPaintFilter() {
        if (mInvisible) {
            return;
        }
        int mul = (int) ((1.0f - mDimAlpha) * mThumbnailAlpha * 255);
        int add = (int) ((1.0f - mDimAlpha) * (1 - mThumbnailAlpha) * 255);
        if (mBitmapShader != null) {
            mLightingColorFilter =
                    new LightingColorFilter(Color.argb(255, mul, mul, mul),
                            Color.argb(0, add, add, add));
            mDrawPaint.setColorFilter(mLightingColorFilter);
            mDrawPaint.setColor(0xffffffff);
        } else {
            int grey = mul + add;
            mDrawPaint.setColorFilter(null);
            mDrawPaint.setColor(Color.argb(255, grey, grey, grey));
        }
        invalidate();
    }*/

    /**
     * Updates the thumbnail shader's scale transform.
     */
/*    void updateThumbnailScale() {
        if (mBitmapShader != null) {
            mScaleMatrix.setRectToRect(mBitmapRect, mLayoutRect, Matrix.ScaleToFit.FILL);
            mBitmapShader.setLocalMatrix(mScaleMatrix);
        }
    }*/

    /**
     * Updates the clip rect based on the given task bar.
     */
    void updateClipToTaskBar(PageViewHeader taskBar) {// TODO: 2017/11/15 这个方法很重要
/*        mTaskBar = taskBar;
        int top = (int) Math.max(0, taskBar.getTranslationY() +
                taskBar.getMeasuredHeight() - 1);
        mClipRect.set(0, top, getMeasuredWidth(), getMeasuredHeight());
        setClipBounds(mClipRect);*/
    }

    /**
     * Updates the visibility of the the thumbnail.
     */
    void updateThumbnailVisibility(int clipBottom) {
/*        boolean invisible = mTaskBar != null && (getHeight() - clipBottom) <= mTaskBar.getHeight();
        if (invisible != mInvisible) {
            mInvisible = invisible;
            if (!mInvisible) {
                updateThumbnailPaintFilter();
            }
            invalidate();
        }*/
    }

    /**
     * Sets the dim alpha, only used when we are not using hardware layers.
     * (see RecentsConfiguration.useHardwareLayers)
     */
    void setDimAlpha(float dimAlpha) {
/*        mDimAlpha = dimAlpha;
        updateThumbnailPaintFilter();*/
    }

    /**
     * Binds the thumbnail view to the task
     */
    //void rebindToTask(Task t) {
    void rebindToTask(Bitmap thumbnail) {
/*        if (thumbnail != null) {
            setThumbnail(thumbnail);
        } else {
            setThumbnail(null);
        }*/
    }

    /**
     * Unbinds the thumbnail view from the task
     */
    void unbindFromTask() {
        setThumbnail(null);
    }

/*    Bitmap mThumbnail;

    public Bitmap getThumbnail() {
        return mThumbnail;
    }*/

    /**
     * Handles focus changes.
     */
    void onFocusChanged(boolean focused) {
/*        if (focused) {
            if (Float.compare(getAlpha(), 1f) != 0) {
                startFadeAnimation(1f, 0, 150, null);
            }
        } else {
            if (Float.compare(getAlpha(), mConfig.pageViewThumbnailAlpha) != 0) {
                startFadeAnimation(mConfig.pageViewThumbnailAlpha, 0, 150, null);
            }
        }*/
    }

    /**
     * Prepares for the enter recents animation, this gets called before the the view
     * is first visible and will be followed by a startEnterRecentsAnimation() call.
     */
/*
    void prepareEnterRecentsAnimation(boolean ispageViewLaunchTargetTask) {
        if (ispageViewLaunchTargetTask) {
            mThumbnailAlpha = 1f;
        } else {
            mThumbnailAlpha = mConfig.pageViewThumbnailAlpha;
        }
        updateThumbnailPaintFilter();
    }
*/

    /**
     * Starts a new thumbnail alpha animation.
     */
/*    void startFadeAnimation(float finalAlpha, int delay, int duration, final Runnable postAnimRunnable) {
        DVUtils.cancelAnimationWithoutCallbacks(mThumbnailAlphaAnimator);
        mThumbnailAlphaAnimator = ValueAnimator.ofFloat(mThumbnailAlpha, finalAlpha);
        mThumbnailAlphaAnimator.setStartDelay(delay);
        mThumbnailAlphaAnimator.setDuration(duration);
        mThumbnailAlphaAnimator.setInterpolator(mConfig.fastOutSlowInInterpolator);
        mThumbnailAlphaAnimator.addUpdateListener(mThumbnailAlphaUpdateListener);
        if (postAnimRunnable != null) {
            mThumbnailAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    postAnimRunnable.run();
                }
            });
        }
        mThumbnailAlphaAnimator.start();
    }*/
}
