package com.example.a10330.pageviewtest.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import com.example.a10330.pageviewtest.R;
import com.example.a10330.pageviewtest.helpers.PageStackViewConfig;
import com.example.a10330.pageviewtest.helpers.PageViewTransform;
import com.example.a10330.pageviewtest.utilities.DVConstants;
import com.example.a10330.pageviewtest.utilities.DVUtils;
/**
 * Created by 10330 on 2017/11/5.
 */

public class PageView<T> extends FrameLayout implements View.OnClickListener,View.OnLongClickListener {
    interface PageViewCallbacks<T> {
        void onPageViewAppIconClicked(PageView dcv);
        void onPageViewAppInfoClicked(PageView dcv);
        void onPageViewClicked(PageView<T> dcv, T key);
        void onPageViewDismissed(PageView<T> dcv);
        void onPageViewClipStateChanged(PageView dcv);
        void onPageViewFocusChanged(PageView<T> dcv, boolean focused);
    }
    private PageStackViewConfig mConfig;
    private float mTaskProgress;
    private ObjectAnimator mTaskProgressAnimator;
    private float mMaxDimScale;
    private int mDimAlpha;
    private AccelerateInterpolator mDimInterpolator = new AccelerateInterpolator(1f);
    private Paint mDimLayerPaint = new Paint();
    private T mKey;
    private boolean mIsFocused;
    private boolean mFocusAnimationsEnabled;
    private boolean mClipViewInStack;
    private AnimateablePageViewBounds mViewBounds;
    private View mPageViewContent;
    PageViewThumbnail mThumbnailView;
    private PageViewHeader mHeaderView;
    private PageViewCallbacks<T> mCb;
    // Optimizations
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setTaskProgress((Float) animation.getAnimatedValue());
                }
            };

    public PageView(Context context) {
        this(context, null);
    }
    public PageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public PageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = PageStackViewConfig.getInstance();
        mMaxDimScale = mConfig.pageStackMaxDim / 255f;
        mClipViewInStack = true;
        mViewBounds = new AnimateablePageViewBounds(this, mConfig.pageViewRoundedCornerRadiusPx);
        setTaskProgress(mTaskProgress);
        setDim(mDimAlpha);
        /*if (mConfig.fakeShadows) {
            setBackground(new FakeShadowDrawable(context.getResources(), mConfig));
        }*/
        setOutlineProvider(mViewBounds);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPageViewContent =findViewById(R.id.page_view_content);
        mHeaderView =findViewById(R.id.task_view_bar);
        mThumbnailView =findViewById(R.id.task_view_thumbnail);
//        mThumbnailView.updateClipToTaskBar(mHeaderView);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();

        // Measure the content
        mPageViewContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY));

        // Measure the bar view, and action button
        mHeaderView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mConfig.taskBarHeight, MeasureSpec.EXACTLY));

        // Measure the thumbnail to be square
        mThumbnailView.measure(
                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
        invalidateOutline();
    }
  //不用重写也行
    /**
     * Synchronizes this view's properties with the task's transform
     */
    void updateViewPropertiesToPageTransform(PageViewTransform toTransform, int duration) {
        updateViewPropertiesToPageTransform(toTransform, duration, null);
    }
    void updateViewPropertiesToPageTransform(PageViewTransform toTransform, int duration,
                                             ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Apply the transform
        toTransform.applyToPageView(this, duration, mConfig.fastOutSlowInInterpolator, false,
                !mConfig.fakeShadows, updateCallback);
        // Update the task progress
        DVUtils.cancelAnimationWithoutCallbacks(mTaskProgressAnimator);
        if (duration <= 0) {
            setTaskProgress(toTransform.p);
        } else {
            mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", toTransform.p);
            mTaskProgressAnimator.setDuration(duration);
            mTaskProgressAnimator.addUpdateListener(mUpdateDimListener);
            mTaskProgressAnimator.start();
        }//改变阴影的
    }
    void resetViewProperties() {
        setDim(0);
        setLayerType(View.LAYER_TYPE_NONE, null);
        PageViewTransform.reset(this);
    }
    /**
     * Prepares this task view for the enter-recents animations.  This is called earlier in the
     * first layout because the actual animation into recents may take a long time.
     */
/*    void prepareEnterRecentsAnimation(boolean isPageViewLaunchTargetTask,
                                      boolean occludesLaunchTarget, int offscreenY) {
        int initialDim = mDimAlpha;
        if (mConfig.launchedHasConfigurationChanged) {
            // Just load the views as-is
        } else if (mConfig.launchedFromAppWithThumbnail) {
            if (isPageViewLaunchTargetTask) {
                // Set the dim to 0 so we can animate it in
                initialDim = 0;
            } else if (occludesLaunchTarget) {
                // Move the task view off screen (below) so we can animate it in
                setTranslationY(offscreenY);
            }

        } else if (mConfig.launchedFromHome) {
            // Move the task view off screen (below) so we can animate it in
            setTranslationY(offscreenY);
            setTranslationZ(0);
            setScaleX(1f);
            setScaleY(1f);
        }
        // Apply the current dim
        setDim(initialDim);
        // Prepare the thumbnail view alpha
        mThumbnailView.prepareEnterRecentsAnimation(isPageViewLaunchTargetTask);
    }*/
    /**
     * Animates this task view as it enters recents
     */
/*    void startEnterRecentsAnimation(final ViewAnimation.PageViewEnterContext ctx) {
        Log.i(getClass().getSimpleName(), "startEnterRecentsAnimation");
        final PageViewTransform transform = ctx.currentTaskTransform;
        int startDelay = 0;

        if (mConfig.launchedFromHome) {
            Log.i(getClass().getSimpleName(), "mConfig.launchedFromHome false");
            // Animate the tasks up
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex - 1);
            int delay = mConfig.transitionEnterFromHomeDelay +
                    frontIndex * mConfig.pageViewEnterFromHomeStaggerDelay;

            setScaleX(transform.scale);
            setScaleY(transform.scale);
            if (!mConfig.fakeShadows) {
                animate().translationZ(transform.translationZ);
            }
            animate()
                    .translationY(transform.translationY)
                    .setStartDelay(delay)
                    .setUpdateListener(ctx.updateListener)
                    .setInterpolator(mConfig.quintOutInterpolator)
                    .setDuration(mConfig.pageViewEnterFromHomeDuration +
                            frontIndex * mConfig.pageViewEnterFromHomeStaggerDelay)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            // Decrement the post animation trigger
                            ctx.postAnimationTrigger.decrement();
                        }
                    })
                    .start();
            ctx.postAnimationTrigger.increment();
            startDelay = delay;
        }

        // Enable the focus animations from this point onwards so that they aren't affected by the
        // window transitions
        postDelayed(new Runnable() {
            @Override
            public void run() {
                enableFocusAnimations();
            }
        }, startDelay);
    }*/
    /**
     * Animates this task view if the user does not interact with the stack after a certain time.
     */
    void startNoUserInteractionAnimation() {
        mHeaderView.startNoUserInteractionAnimation();
    }
    /**
     * Animates the deletion of this task view
     */
    private void startDeleteTaskAnimation(final Runnable r) {
        // Disabling clipping with the stack while the view is animating away
        setClipViewInStack(false);
        animate().translationX(mConfig.pageViewRemoveAnimTranslationXPx)
                .alpha(0f)
                .setStartDelay(0)
                .setUpdateListener(null)
                .setInterpolator(mConfig.fastOutSlowInInterpolator)
                .setDuration(mConfig.pageViewRemoveAnimDuration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        // We just throw this into a runnable because starting a view property
                        // animation using layers can cause inconsisten不一致的 results if we try and
                        // update the layers while the animation is running.  In some cases,
                        // the runnabled passed in may start an animation which also uses layers
                        // so we defer all this by posting this.
                        r.run();
                        // Re-enable clipping with the stack (we will reuse this view)
//                        setClipViewInStack(true);
                    }
                })
                .start();
    }
    /**
     * Mark this task view that the user does has not interacted with the stack after a certain time.
     */
    void setNoUserInteractionState() {
        mHeaderView.setNoUserInteractionState();
    }
    /**
     * Resets the state tracking that the user has not interacted with the stack after a certain time.
     */
/*
    void resetNoUserInteractionState() {
        mHeaderView.resetNoUserInteractionState();
    }
*/
    /**
     * Dismisses this task.
     */
    private void dismissTask() {
        // Animate out the view and call the callback
        final PageView<T> tv = this;
        startDeleteTaskAnimation(new Runnable() {
            @Override
            public void run() {
                if (mCb != null) {
                    mCb.onPageViewDismissed(tv);
                }
            }
        });
    }
    /**
     * Returns whether this view should be clipped, or any views below should clip against this
     * view.
     */
    boolean shouldClipViewInStack() {
        return mClipViewInStack && (getVisibility() == View.VISIBLE);
    }
    /**
     * Sets whether this view should be clipped, or clipped against.
     */
    void setClipViewInStack(boolean clip) {
        if (clip != mClipViewInStack) {
            mClipViewInStack = clip;
            if (mCb != null) {
                mCb.onPageViewClipStateChanged(this);
            }
        }
    }
    void setCallbacks(PageViewCallbacks cb) {
        mCb = cb;
    }
    /**
     * Resets this pageView for reuse.
     */
 /*   void reset() {
        resetViewProperties();
        resetNoUserInteractionState();
        setClipViewInStack(false);
        setCallbacks(null);
    }*/
    T getAttachedKey() {
        return mKey;
    }
    /**
     * Returns the view bounds.
     */
    AnimateablePageViewBounds getViewBounds() {
        return mViewBounds;
    }
    private void setTaskProgress(float p) {
        mTaskProgress = p;
        mViewBounds.setAlpha(p);
        updateDimFromTaskProgress();
    }
    /**
     * Returns the current dim.
     */
    private void setDim(int dim) {
        mDimAlpha = dim;
        if (mConfig.useHardwareLayers) {
            // Defer setting hardware layers if we have not yet measured, or there is no dim to draw
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                PorterDuffColorFilter mDimColorFilter =
                        new PorterDuffColorFilter(Color.argb(mDimAlpha, 0, 0, 0),
                                PorterDuff.Mode.SRC_ATOP);
                mDimLayerPaint.setColorFilter(mDimColorFilter);
                mPageViewContent.setLayerType(LAYER_TYPE_HARDWARE, mDimLayerPaint);
            }
        } else {
            float dimAlpha = mDimAlpha / 255.0f;
            if (mThumbnailView != null) {
                mThumbnailView.setDimAlpha(dimAlpha);
            }
            if (mHeaderView != null) {
                mHeaderView.setDimAlpha(dim);
            }
        }
    }
    /**
     * Compute the dim as a function of the scale of this view.
     */
    private int getDimFromTaskProgress() {
        float dim = mMaxDimScale * mDimInterpolator.getInterpolation(1f - mTaskProgress);
        return (int) (dim * 255);
    }
    /**
     * Update the dim as a function of the scale of this view.
     */
    private void updateDimFromTaskProgress() {
        setDim(getDimFromTaskProgress());
    }
    /**** View focus state ****/
    /**
     * Sets the focused task explicitly. We need a separate flag because requestFocus() won't happen
     * if the view is not currently visible, or we are in touch state (where we still want to keep
     * track of focus).
     */
    void setFocusedTask(boolean animateFocusedState) {
        mIsFocused = true;
        if (mFocusAnimationsEnabled) {
            // Focus the header bar
            mHeaderView.onPageViewFocusChanged(true, animateFocusedState);
        }
        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(true);
        // Call the callback
        if (mCb != null) {
            mCb.onPageViewFocusChanged(this, true);
        }
        // Workaround, we don't always want it focusable in touch mode, but we want the first task
        // to be focused after the enter-recents animation, which can be triggered from either touch
        // or keyboard
        setFocusableInTouchMode(true);
        requestFocus();
        setFocusableInTouchMode(false);
        invalidate();
    }
    /**
     * Unsets the focused task explicitly.
     */
    private void unsetFocusedTask() {
        mIsFocused = false;
        if (mFocusAnimationsEnabled) {
            // Un-focus the header bar
            mHeaderView.onPageViewFocusChanged(false, true);
        }
        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(false);
        // Call the callback
        if (mCb != null) {
            mCb.onPageViewFocusChanged(this, false);
        }
        invalidate();
    }
    /**
     * Updates the explicitly focused state when the view focus changes.
     */
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            unsetFocusedTask();
        }
    }
    /**
     * Returns whether we have explicitly been focused.
     */
    boolean isFocusedTask() {
        return mIsFocused || isFocused();
    }
    /**
     * Enables all focus animations.
     */
    void enableFocusAnimations() {
        boolean wasFocusAnimationsEnabled = mFocusAnimationsEnabled;
        mFocusAnimationsEnabled = true;
        if (mIsFocused && !wasFocusAnimationsEnabled) {
            // Re-notify the header if we were focused and animations were not previously enabled
            mHeaderView.onPageViewFocusChanged(true, true);
        }
    }
    /**
     * Binds this task view to the task
     */
    void onTaskBound(T key) {
        mKey = key;
    }
    private boolean isBound() {
        return mKey != null;
    }
    /**
     * Binds this task view to the task
     */
    void onTaskUnbound() {
        mKey = null;
    }
    public void onDataLoaded(T key, Bitmap thumbnail, Drawable headerIcon,
                             String headerTitle, int headerBgColor) {
        if (!isBound() || !mKey.equals(key))
            return;
        if (mThumbnailView != null && mHeaderView != null) {
            // Bind each of the views to the new task data
            mThumbnailView.rebindToTask(thumbnail);
            mHeaderView.rebindToTask(headerIcon, headerTitle, headerBgColor);
            // Rebind any listeners
            mHeaderView.mApplicationIcon.setOnClickListener(this);
            mHeaderView.mDismissButton.setOnClickListener(this);
            mHeaderView.mApplicationIcon.setOnLongClickListener(this);
        }
    }
    void onDataUnloaded() {
        if (mThumbnailView != null && mHeaderView != null) {
            // Unbind each of the views from the task data and remove the task callback
            mThumbnailView.unbindFromTask();
            mHeaderView.unbindFromTask();
            // Unbind any listeners
            mHeaderView.mApplicationIcon.setOnClickListener(null);
            mHeaderView.mDismissButton.setOnClickListener(null);
            mHeaderView.mApplicationIcon.setOnLongClickListener(null);
        }
    }
    /**
     * Enables/disables handling touch on this task view.
     */
    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }
    @Override
    public void onClick(final View v) {
        final PageView<T> tv = this;
        final boolean delayViewClick = (v != this);// TODO: 2017/11/13 为何还有这种可能？
        if (delayViewClick) {
            // We purposely post the handler delayed to allow for the touch feedback to draw
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (DVConstants.DebugFlags.App.EnableTaskFiltering
                            && v == mHeaderView.mApplicationIcon) {
                        if (mCb != null) {
                            mCb.onPageViewAppIconClicked(tv);
                        }
                    } else if (v == mHeaderView.mDismissButton) {
                        dismissTask();
                    }
                }
            }, 125);
        } else {
            if (mCb != null) {
                mCb.onPageViewClicked(tv, tv.getAttachedKey());
            }
        }
    }
    @Override
    public boolean onLongClick(View v) {
        if (v == mHeaderView.mApplicationIcon) {
            if (mCb != null) {
                mCb.onPageViewAppInfoClicked(this);
                return true;
            }
        }
        return false;//true为不加短按,false为加入短按
    }
}
