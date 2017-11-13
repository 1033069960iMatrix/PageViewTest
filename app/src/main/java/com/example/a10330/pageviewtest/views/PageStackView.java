package com.example.a10330.pageviewtest.views;

import android.widget.FrameLayout;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;

import com.example.a10330.pageviewtest.R;
import com.example.a10330.pageviewtest.helpers.PageStackViewConfig;
import com.example.a10330.pageviewtest.helpers.PageViewTransform;
import com.example.a10330.pageviewtest.utilities.DVConstants;
import com.example.a10330.pageviewtest.utilities.DVUtils;
import com.example.a10330.pageviewtest.utilities.DozeTrigger;
import com.example.a10330.pageviewtest.utilities.ReferenceCountedTrigger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
/**
 * Created by 10330 on 2017/11/5.
 */

public class PageStackView<T> extends FrameLayout implements PageView.PageViewCallbacks<T>,PageStackViewScroller.PageStackViewScrollerCallbacks,ViewPool.ViewPoolConsumer<PageView<T>,T>{
    public interface Callback<T> {// TODO: 2017/11/11 接口存在的意义？
        ArrayList<T> getData();
        void loadViewData(WeakReference<PageView<T>> dcv, T item);
        void unloadViewData(T item);
        void onViewDismissed(T item);
        void onItemClick(T item);
        void onNoViewsToPageStack();
    }
    private PageStackViewConfig mConfig;
    private Callback<T> mCallback;
    private PageStackViewLayoutAlgorithm<T> mLayoutAlgorithm;
    private PageStackViewScroller mStackScroller;
    private PageStackViewTouchHandler mTouchHandler;
    private ViewPool<PageView<T>, T> mViewPool;
    private LayoutInflater mInflater;
    private ArrayList<PageViewTransform> mCurrentTaskTransforms = new ArrayList<PageViewTransform>();
    private DozeTrigger mUIDozeTrigger;
    private Rect mPageStackBounds = new Rect();
    private int mFocusedTaskIndex = -1;
    // Optimizations
    private int mStackViewsAnimationDuration;
    private boolean mPageStackViewDirty = true;
    private boolean mPageStackViewsClipDirty = true;// TODO: 2017/11/11 这两个变量一直没弄懂,应该是优化用的
    private boolean mAwaitingFirstLayout = true;
    private boolean mStartEnterAnimationRequestedAfterLayout;
    private boolean mStartEnterAnimationCompleted;
    private ViewAnimation.PageViewEnterContext mStartEnterAnimationContext;
    private Rect mTmpRect = new Rect();
    private PageViewTransform mTmpTransform = new PageViewTransform();
    private HashMap<T, PageView> mTmpTaskViewMap = new HashMap<T, PageView>();
    // A convenience update listener to request updating clipping of tasks
    private ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    requestUpdateStackViewsClip();
                }
            };

    public PageStackView(Context context) {
        this(context, null);
    }
    public PageStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public PageStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public PageStackView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public void initialize(Callback<T> callback) {
        mConfig=PageStackViewConfig.reinitialize(getContext());
        mCallback = callback;
        mViewPool = new ViewPool<PageView<T>, T>(getContext(), this);
        mInflater = LayoutInflater.from(getContext());
        mLayoutAlgorithm = new PageStackViewLayoutAlgorithm<T>(mConfig);
        mStackScroller = new PageStackViewScroller(getContext(), mConfig, mLayoutAlgorithm);
        mStackScroller.setCallbacks(this);
        mTouchHandler = new PageStackViewTouchHandler(getContext(), this, mConfig, mStackScroller);
        mUIDozeTrigger = new DozeTrigger(mConfig.taskBarDismissDozeDelaySeconds, new Runnable() {
            @Override
            public void run() {
                // Show the task bar dismiss buttons
                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    PageView tv = (PageView) getChildAt(i);
                    tv.startNoUserInteractionAnimation();
                }
            }
        });
    }
    /**
     * Ensures that there is a task focused, if nothing is focused, then we will use the task
     * at the center of the visible stack.
     */
    boolean ensureFocusedTask() {
        if (mFocusedTaskIndex < 0) {
            // If there is no task focused, then find the task that is closes to the center
            // of the screen and use that as the currently focused task
            int x = mLayoutAlgorithm.mStackVisibleRect.centerX();
            int y = mLayoutAlgorithm.mStackVisibleRect.centerY();
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                PageView tv = (PageView) getChildAt(i);
                tv.getHitRect(mTmpRect);
                if (mTmpRect.contains(x, y)) {
                    mFocusedTaskIndex = i;
                    break;
                }
            }
            // If we can't find the center task, then use the front most index
            if (mFocusedTaskIndex < 0 && childCount > 0) {
                mFocusedTaskIndex = childCount - 1;
            }
        }
        return mFocusedTaskIndex >= 0;
    }
    /**
     * Focuses the next task in the stack.
     *
     * @param animateFocusedState determines whether to actually draw the highlight along with
     *                            the change in focus, as well as whether to scroll to fit the
     *                            task into view.
     */
    void focusNextTask(boolean forward, boolean animateFocusedState) {
        // Find the next index to focus
        int numTasks = mCallback.getData().size();
        if (numTasks == 0) return;

        int direction = (forward ? -1 : 1);
        int newIndex = mFocusedTaskIndex + direction;
        if (newIndex >= 0 && newIndex <= (numTasks - 1)) {
            newIndex = Math.max(0, Math.min(numTasks - 1, newIndex));
            focusTask(newIndex, true, animateFocusedState);
        }
    }
    /**
     * Resets the focused task.
     */
/*    void resetFocusedTask() {
        if ((0 <= mFocusedTaskIndex) && (mFocusedTaskIndex < mCallback.getData().size())) {
            PageView tv = getPageViewForTask(mCallback.getData().get(mFocusedTaskIndex));
            if (tv != null) {
                tv.unsetFocusedTask();
            }
        }
        mFocusedTaskIndex = -1;
    }*/
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHandler.onInterceptTouchEvent(ev);
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mTouchHandler.onTouchEvent(ev);
    }
    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        return mTouchHandler.onGenericMotionEvent(ev);
    }
    @Override
    public void computeScroll() {
//        mStackScroller.computeScroll();//手指松开后继续滑动就靠它了
        // Synchronize the views
        synchronizeStackViewsWithModel();//去掉这个只是不能滑动，还能显示，如果onMeasure里再去掉，就不行了
//        clipTaskViews();
    }
    /**
     * This is called with the full window width and height to allow stack view children to
     * perform the full screen transition down.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mPageStackBounds.set(0,0,width,height);
        computeRects(width, height, mPageStackBounds, mConfig.launchedWithAltTab, mConfig.launchedFromHome);
        // If this is the first layout, then scroll to the front of the stack and synchronize the
        // stack views immediately to load all the views
/*        if (mAwaitingFirstLayout) {
            mStackScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }*/
        //以后去掉注释 上面的代码跟布局没关系，只是一开始跳到前几个显示
        // Measure each of the TaskViews
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            PageView tv = (PageView) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            tv.measure(
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mTaskRect.width() + mTmpRect.left + mTmpRect.right,
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mTaskRect.height() + mTmpRect.top + mTmpRect.bottom,
                            MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }
    /**
     * Returns the stack algorithm for this task stack.
     */
    PageStackViewLayoutAlgorithm getStackAlgorithm() {
        return mLayoutAlgorithm;
    }
    public int getCurrentChildIndex() {
        if (getChildCount() == 0)
            return -1;

        PageView<T> frontMostChild = (PageView) getChildAt(getChildCount() / 2);

        if (frontMostChild != null) {
            return mCallback.getData().indexOf(frontMostChild.getAttachedKey());
        }
        return -1;
    }
    /**
     * Focuses the task at the specified index in the stack
     */
    public void scrollToChild(int childIndex) {
        if (getCurrentChildIndex() == childIndex)
            return;

        if (0 <= childIndex && childIndex < mCallback.getData().size()) {
            // Scroll the view into position (just center it in the curve)
            float newScroll = mLayoutAlgorithm.getStackScrollForTask(
                    mCallback.getData().get(childIndex)) - 0.5f;
            newScroll = mStackScroller.getBoundedStackScroll(newScroll);
            mStackScroller.setStackScroll(newScroll);
            //Alternate (animated) way
            //mStackScroller.animateScroll(mStackScroller.getStackScroll(), newScroll, null);
        }
    }
    /**
     * This is called with the size of the space not including the top or right insets, or the
     * search bar height in portrait (but including the search bar width in landscape, since we want
     * to draw under it.
     */
    @Override//查看后如果没重写这个方法只会导致画面整体左移，所以其他的东西应该不是在这里实现的，先给注释掉
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Layout each of the children
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            PageView tv = (PageView) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            tv.layout(mLayoutAlgorithm.mTaskRect.left - mTmpRect.left,
                    mLayoutAlgorithm.mTaskRect.top - mTmpRect.top,
                    mLayoutAlgorithm.mTaskRect.right + mTmpRect.right,
                    mLayoutAlgorithm.mTaskRect.bottom + mTmpRect.bottom);
        }
        if (mAwaitingFirstLayout) {
            mAwaitingFirstLayout = false;
//            onFirstLayout();//好像没什么用，只是小差差没有了
        }
        mUIDozeTrigger.startDozing();//从下面移动上来的
    }
    boolean isTransformedTouchPointInView(float x, float y, View child) {
        // TODO: confirm if this is the right approach
        if (child == null)
            return false;
        final Rect frame = new Rect();
        child.getHitRect(frame);
        return frame.contains((int) x, (int) y);
    }
    public void notifyDataSetChanged() {
        // Get the stack scroll of the task to anchor to (since we are removing something, the front
        // most task will be our anchor task)
        T anchorPageData = null;
        float prevAnchorTaskScroll = 0;
        boolean pullStackForward = mCallback.getData().size() > 0;
        if (pullStackForward) {
            anchorPageData = mCallback.getData().get(mCallback.getData().size() - 1);
            prevAnchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPageData);
        }
        // Update the min/max scroll and animate other task views into their new positions
        updateMinMaxScroll(true, mConfig.launchedWithAltTab, mConfig.launchedFromHome);
        // Offset the stack by as much as the anchor task would otherwise move back
        if (pullStackForward) {
            float anchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPageData);
            mStackScroller.setStackScroll(mStackScroller.getStackScroll() + (anchorTaskScroll
                    - prevAnchorTaskScroll));
            mStackScroller.boundScroll();
        }
        // Animate all the tasks into place
//        requestSynchronizeStackViewsWithModel(200);// TODO: 2017/11/13 别忘了加上
        T newFrontMostPageData = mCallback.getData().size() > 0 ?
                mCallback.getData().get(mCallback.getData().size() - 1)
                : null;
        // Update the new front most task
        if (newFrontMostPageData != null) {
            PageView<T> frontTv = getPageViewForTask(newFrontMostPageData);
            if (frontTv != null) {
                frontTv.onTaskBound(newFrontMostPageData);
            }
        }
        // If there are no remaining tasks
        if (mCallback.getData().size() == 0) {
            mCallback.onNoViewsToPageStack();
        }
    }
    /**
     * Resets this PageStackView for reuse.
     */
/*    void reset() {
        // Reset the focused task
        resetFocusedTask();

        // Return all the views to the pool
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            PageView<T> tv = (PageView) getChildAt(i);
            mViewPool.returnViewToPool(tv);
        }

        // Mark each task view for relayout
        if (mViewPool != null) {
            Iterator<PageView<T>> iter = mViewPool.poolViewIterator();
            if (iter != null) {
                while (iter.hasNext()) {
                    PageView tv = iter.next();
                    tv.reset();
                }
            }
        }
        // Reset the stack state
        mPageStackViewDirty = true;
        mPageStackViewsClipDirty = true;
        mAwaitingFirstLayout = true;
        mPrevAccessibilityFocusedIndex = -1;
        if (mUIDozeTrigger != null) {
            mUIDozeTrigger.stopDozing();
            mUIDozeTrigger.resetTrigger();
        }
        mStackScroller.reset();
    }*/

    /**
     * Requests that the views be synchronized with the model
     */
    private void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }
    private void requestSynchronizeStackViewsWithModel(int duration) {
        if (!mPageStackViewDirty) {
            invalidate();
            mPageStackViewDirty = true;
        }
/*        if (mAwaitingFirstLayout) {
            // Skip the animation if we are awaiting first layout
            mStackViewsAnimationDuration = 0;
        } else {
            mStackViewsAnimationDuration = Math.max(mStackViewsAnimationDuration, duration);
        }*/
    }
    /**
     * Requests that the views clipping be updated.
     */
    private void requestUpdateStackViewsClip() {
        if (!mPageStackViewsClipDirty) {
            invalidate();
            mPageStackViewsClipDirty = true;
        }
    }
    /**
     * Finds the child view given a specific task.
     */
    private PageView getPageViewForTask(T key) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            PageView tv = (PageView) getChildAt(i);
            if (tv.getAttachedKey().equals(key)) {
                return tv;
            }
        }
        return null;
    }
    /**
     * Gets the stack transforms of a list of tasks, and returns the visible range of tasks.
     */
    private boolean updateStackTransforms(ArrayList<PageViewTransform> taskTransforms,
                                          ArrayList<T> data,
                                          float stackScroll,
                                          int[] visibleRangeOut,
                                          boolean boundTranslationsToRect) {// TODO: 2017/11/11 还可以在看细点
        int taskTransformCount = taskTransforms.size();
        int taskCount = data.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;
        // We can reuse the task transforms where possible to reduce object allocation
        if (taskTransformCount < taskCount) {
            // If there are less transforms than tasks, then add as many transforms as necessary
            for (int i = taskTransformCount; i < taskCount; i++) {
                taskTransforms.add(new PageViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            // If there are more transforms than tasks, then just subset the transform list
            taskTransforms.subList(0, taskCount);
        }
        // Update the stack transforms
        PageViewTransform prevTransform = null;
        for (int i = taskCount - 1; i >= 0; i--) {
            PageViewTransform transform =
                    mLayoutAlgorithm.getStackTransform(data.get(i),
                            stackScroll, taskTransforms.get(i), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i;
                }
                backMostVisibleIndex = i;
            } else {
                if (backMostVisibleIndex != -1) {
                    // We've reached the end of the visible range, so going down the rest of the
                    // stack, we can just reset the transforms accordingly
                    while (i >= 0) {
                        taskTransforms.get(i).reset();
                        i--;
                    }
                    break;
                }
            }

            if (boundTranslationsToRect) {
                transform.translationY = Math.min(transform.translationY,
                        mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        return frontMostVisibleIndex != -1 && backMostVisibleIndex != -1;
    }
    /**
     * Synchronizes the views with the model
     */
    private boolean synchronizeStackViewsWithModel() {// TODO: 2017/11/11 再细看
        if (mPageStackViewDirty) {
            // Get all the task transforms
            ArrayList<T> data = mCallback.getData();
            float stackScroll = mStackScroller.getStackScroll();
            int[] visibleRange =new int[2];
            boolean isValidVisibleRange = updateStackTransforms(mCurrentTaskTransforms,
                    data, stackScroll, visibleRange, false);
            // Return all the invisible children to the pool
            mTmpTaskViewMap.clear();
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                PageView<T> tv = (PageView) getChildAt(i);
                T key = tv.getAttachedKey();
                int taskIndex = data.indexOf(key);
                if (visibleRange[1] <= taskIndex
                        && taskIndex <= visibleRange[0]) {
                    mTmpTaskViewMap.put(key, tv);
                } else {
                    mViewPool.returnViewToPool(tv);
                }
            }
            for (int i = visibleRange[0]; isValidVisibleRange && i >= visibleRange[1]; i--) {
                T key = data.get(i);
                PageViewTransform transform = mCurrentTaskTransforms.get(i);
                PageView tv = mTmpTaskViewMap.get(key);
                if (tv == null) {
                    // TODO Check
                    tv = mViewPool.pickUpViewFromPool(key, key);//回调了方法导致加载了多个PageView
                    if (mStackViewsAnimationDuration > 0) {
                        // For items in the list, put them in start animating them from the
                        // approriate ends of the list where they are expected to appear
                        if (Float.compare(transform.p, 0f) <= 0) {
                            mLayoutAlgorithm.getStackTransform(0f, 0f, mTmpTransform, null);
                        } else {
                            mLayoutAlgorithm.getStackTransform(1f, 0f, mTmpTransform, null);
                        }
                        tv.updateViewPropertiesToPageTransform(mTmpTransform, 0);
                    }
                }
                // Animate the task into place
//                mStackViewsAnimationDuration=2500;//把数值改大发现动画有问题
                tv.updateViewPropertiesToPageTransform(mCurrentTaskTransforms.get(i),
                        mStackViewsAnimationDuration, mRequestUpdateClippingListener);
            }
            // Reset the request-synchronize params
            mStackViewsAnimationDuration = 0;
            mPageStackViewDirty = false;
            mPageStackViewsClipDirty = true;
            return true;
        }
        return false;
    }
    /**
     * Updates the clip for each of the task views.裁边的功能
     */
    private void clipTaskViews() {
        // Update the clip on each task child
        if (DVConstants.DebugFlags.App.EnablePageStackClipping) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                PageView tv = (PageView) getChildAt(i);
                PageView nextTv = null;
                PageView tmpTv ;
                int clipBottom = 0;
                if (tv.shouldClipViewInStack()) {
                    // Find the next view to clip against
                    int nextIndex = i;
                    while (nextIndex < getChildCount()) {
                        tmpTv = (PageView) getChildAt(++nextIndex);
                        if (tmpTv != null && tmpTv.shouldClipViewInStack()) {
                            nextTv = tmpTv;
                            break;
                        }
                    }
                    // Clip against the next view, this is just an approximation近似 since we are
                    // stacked and we can make assumptions about the visibility of the this
                    // task relative to the ones in front of it.
                    if (nextTv != null) {
                        // Map the top edge of next task view into the local space of the current
                        // task view to find the clip amount in local space
                        float[] mTmpCoord = new float[2];
                        Matrix mTmpMatrix = new Matrix();
                        mTmpCoord[0] = mTmpCoord[1] = 0;
                        DVUtils.mapCoordInDescendentToSelf(nextTv, this, mTmpCoord, false);
                        DVUtils.mapCoordInSelfToDescendent(tv, this, mTmpCoord, mTmpMatrix);
                        clipBottom = (int) Math.floor(tv.getMeasuredHeight() - mTmpCoord[1]
                                - nextTv.getPaddingTop() - 1);
                    }
                }
                tv.getViewBounds().setClipBottom(clipBottom);
            }
            if (getChildCount() > 0) {
                // The front most task should never be clipped
                PageView tv = (PageView) getChildAt(getChildCount() - 1);
                tv.getViewBounds().setClipBottom(0);
            }
        }
        mPageStackViewsClipDirty = false;
    }
    /**
     * The stack insets to apply to the stack contents
     */
//    public void setStackInsetRect(Rect r) {
//        mPageStackBounds.set(r);
//    }

    /**
     * Updates the min and max virtual scroll bounds
     */
    private void updateMinMaxScroll(boolean boundScrollToNewMinMax, boolean launchedWithAltTab,
                            boolean launchedFromHome) {
        // Compute the min and max scroll values
        mLayoutAlgorithm.computeMinMaxScroll(mCallback.getData(), launchedWithAltTab, launchedFromHome);
        // Debug logging
        if (boundScrollToNewMinMax) {
            mStackScroller.boundScroll();
        }
    }
    /**
     * Focuses the task at the specified index in the stack
     */
    private void focusTask(int childIndex, boolean scrollToNewPosition, final boolean animateFocusedState) {
        if (childIndex == mFocusedTaskIndex) return;
        ArrayList<T> data = mCallback.getData();
        if (0 <= childIndex && childIndex < data.size()) {
            mFocusedTaskIndex = childIndex;
            // Focus the view if possible, otherwise, focus the view after we scroll into position
            T key = data.get(childIndex);
            PageView tv = getPageViewForTask(key);
            Runnable postScrollRunnable = null;
            if (tv != null) {
                tv.setFocusedTask(animateFocusedState);
            } else {
                postScrollRunnable = new Runnable() {
                    @Override
                    public void run() {
                        PageView tv = getPageViewForTask(mCallback.getData().get(mFocusedTaskIndex));
                        if (tv != null) {
                            tv.setFocusedTask(animateFocusedState);
                        }
                    }
                };
            }
            // Scroll the view into position (just center it in the curve)
            if (scrollToNewPosition) {
                float newScroll = mLayoutAlgorithm.getStackScrollForTask(key) - 0.5f;
                newScroll = mStackScroller.getBoundedStackScroll(newScroll);
                mStackScroller.animateScroll(mStackScroller.getStackScroll(), newScroll, postScrollRunnable);
            } else {
                if (postScrollRunnable != null) {
                    postScrollRunnable.run();
                }
            }

        }
    }
    /**
     * Computes the stack and task rects
     */
    private void computeRects(int windowWidth, int windowHeight, Rect pageStackBounds,
                             boolean launchedWithAltTab, boolean launchedFromHome) {
        // Compute the rects in the stack algorithm
        mLayoutAlgorithm.computeRects(windowWidth, windowHeight, pageStackBounds);
        // Update the scroll bounds
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }
    /**
     * Handler for the first layout.
     */
/*    private void onFirstLayout() {
        int offscreenY = mLayoutAlgorithm.mViewRect.bottom -
                (mLayoutAlgorithm.mTaskRect.top - mLayoutAlgorithm.mViewRect.top);
        int childCount = getChildCount();
        // Prepare the first view for its enter animation
        for (int i = childCount - 1; i >= 0; i--) {
            PageView tv = (PageView) getChildAt(i);
            // TODO: The false needs to go!
            tv.prepareEnterRecentsAnimation(i == childCount - 1, false, offscreenY);
        }
        // If the enter animation started already and we haven't completed a layout yet, do the
        // enter animation now
        if (mStartEnterAnimationRequestedAfterLayout) {
            startEnterRecentsAnimation(mStartEnterAnimationContext);
            mStartEnterAnimationRequestedAfterLayout = false;
            mStartEnterAnimationContext = null;
        }
        // When Alt-Tabbing, focus the previous task (but leave the animation until we finish the
        // enter animation).
        if (mConfig.launchedWithAltTab) {
            if (mConfig.launchedFromAppWithThumbnail) {
                focusTask(Math.max(0, mCallback.getData().size() - 2), false,
                        mConfig.launchedHasConfigurationChanged);
            } else {
                focusTask(Math.max(0, mCallback.getData().size() - 1), false,
                        mConfig.launchedHasConfigurationChanged);
            }
        }
        mUIDozeTrigger.startDozing();
    }*/
    /**
     * Requests this task stacks to start it's enter-recents animation
     */
/*    private void startEnterRecentsAnimation(ViewAnimation.PageViewEnterContext ctx) {
        // If we are still waiting to layout, then just defer until then
        if (mAwaitingFirstLayout) {
            mStartEnterAnimationRequestedAfterLayout = true;
            mStartEnterAnimationContext = ctx;
            return;
        }
        if (mCallback.getData().size() > 0) {
            int childCount = getChildCount();

            // Animate all the task views into view
            for (int i = childCount - 1; i >= 0; i--) {
                PageView<T> tv = (PageView) getChildAt(i);
                T key = tv.getAttachedKey();
                ctx.currentTaskTransform = new PageViewTransform();
                ctx.currentStackViewIndex = i;
                ctx.currentStackViewCount = childCount;
                ctx.currentTaskRect = mLayoutAlgorithm.mTaskRect;
                // TODO: this needs to go
                ctx.currentTaskOccludesLaunchTarget = false;
                ctx.updateListener = mRequestUpdateClippingListener;
                mLayoutAlgorithm.getStackTransform(key, mStackScroller.getStackScroll(),
                        ctx.currentTaskTransform, null);
                tv.startEnterRecentsAnimation(ctx);
            }
            // Add a runnable to the post animation ref counter to clear all the views
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    mStartEnterAnimationCompleted = true;
                    // Poke the dozer to restart the trigger after the animation completes
                    mUIDozeTrigger.poke();
                }
            });
        }
    }*/
/*
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Update the configuration with the latest system insets and trigger a relayout
        // mConfig.updateSystemInsets(insets.getSystemWindowInsets());
        mConfig.updateSystemInsets(new Rect(insets.getSystemWindowInsetLeft(),//0
                insets.getSystemWindowInsetTop(),//196
                insets.getSystemWindowInsetRight(),//0
                insets.getSystemWindowInsetBottom()));//0
        requestLayout();
        return insets.consumeSystemWindowInsets();
    }*/
    private void onStackPageRemoved(PageView<T> removedView) {
        // Remove the view associated with this task, we can't rely on updateTransforms
        // to work here because the task is no longer in the list
        if (removedView != null) {
            T key = removedView.getAttachedKey();
            int removedPosition = mCallback.getData().indexOf(key);
            mViewPool.returnViewToPool(removedView);
            // Notify the callback that we've removed the task and it can clean up after it
            mCallback.onViewDismissed(key);
        }

/*        // Get the stack scroll of the task to anchor to (since we are removing something, the front
        // most task will be our anchor task)
        T anchorPageData = null;
        float prevAnchorTaskScroll = 0;
        boolean pullStackForward = mCallback.getData().size() > 0;
        if (pullStackForward) {
            anchorPageData = mCallback.getData().get(mCallback.getData().size() - 1);
            prevAnchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPageData);
        }

        // Update the min/max scroll and animate other task views into their new positions
        updateMinMaxScroll(true, mConfig.launchedWithAltTab, mConfig.launchedFromHome);

        // Offset the stack by as much as the anchor task would otherwise move back
        if (pullStackForward) {
            float anchorTaskScroll = mLayoutAlgorithm.getStackScrollForTask(anchorPageData);
            mStackScroller.setStackScroll(mStackScroller.getStackScroll() + (anchorTaskScroll
                    - prevAnchorTaskScroll));
            mStackScroller.boundScroll();
        }

        // Animate all the tasks into place
        requestSynchronizeStackViewsWithModel(200);

        T newFrontMostPageData = mCallback.getData().get(mCallback.getData().size() - 1);
        // Update the new front most task
        if (newFrontMostPageData != null) {
            PageView<T> frontTv = getPageViewForTask(newFrontMostPageData);
            if (frontTv != null) {
                frontTv.onTaskBound(newFrontMostPageData);
            }
        }

        // If there are no remaining tasks
        if (mCallback.getData().size() == 0) {
            mCallback.onNoViewsToPageStack();
        }*/
//试着把上面的代码恢复，但在最后一个卡片删除后会闪退
    }
    /**
     * * ViewPool Implementation ***
     */
    @Override
    public PageView createView(Context context) {
        return (PageView) mInflater.inflate(R.layout.page_view, this, false);
    }
    @Override
    public void prepareViewToEnterPool(PageView<T> tv) {
        T key = tv.getAttachedKey();
        mCallback.unloadViewData(key);
        tv.onTaskUnbound();
        tv.onDataUnloaded();
        detachViewFromParent(tv);
        tv.resetViewProperties();
        tv.setClipViewInStack(false);
    }
    @Override
    public void prepareViewToLeavePool(PageView<T> pv, T data, boolean isNewView) {
        // It is possible for a view to be returned to the view pool before it is laid out,
        // which means that we will need to relayout the view when it is first used next.
        boolean requiresRelayout = pv.getWidth() <= 0 && !isNewView;
        // Rebind the task and request that this task's data be filled into the TaskView
        pv.onTaskBound(data);
        // Load the task data
        mCallback.loadViewData(new WeakReference<PageView<T>>(pv), data);
        // If the doze trigger has already fired, then update the state for this task view
        if (mUIDozeTrigger.hasTriggered()) {
            pv.setNoUserInteractionState();
        }
        // If we've finished the start animation, then ensure we always enable the focus animations
        if (mStartEnterAnimationCompleted) {
            pv.enableFocusAnimations();
        }
        // Find the index where this task should be placed in the stack
        int insertIndex = -1;
        int position = mCallback.getData().indexOf(data);
        if (position != -1) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                T otherKey = ((PageView<T>) getChildAt(i)).getAttachedKey();
                int pos = mCallback.getData().indexOf(otherKey);
                if (position < pos) {
                    insertIndex = i;
                    break;
                }
            }
        }
        if (isNewView) {// TODO: 2017/11/12 可能找到view层级控制
            addView(pv, insertIndex);//这个可能是控制view层级的东西，试着改下,数越大越在？
        } else {
            attachViewToParent(pv, insertIndex, pv.getLayoutParams());
            if (requiresRelayout) {
                pv.requestLayout();
            }
        }
        // Set the new state for this view, including the callbacks and view clipping
        pv.setCallbacks(this);
        pv.setTouchEnabled(true);
        pv.setClipViewInStack(true);
    }
    @Override
    public boolean hasPreferredData(PageView<T> tv, T preferredData) {
        return (tv.getAttachedKey() != null && tv.getAttachedKey().equals(preferredData));
    }
    /**
     * * PageView Implementation ***
     */
    @Override
    public void onPageViewAppIconClicked(PageView tv) {
        //
    }
    @Override
    public void onPageViewAppInfoClicked(PageView tv) {
        //
    }
    @Override
    public void onPageViewClicked(PageView<T> dcv, T key) {
        // Cancel any doze triggers
        mUIDozeTrigger.stopDozing();
        mCallback.onItemClick(key);
    }
    @Override
    public void onPageViewDismissed(PageView<T> dcv) {
        boolean taskWasFocused = dcv.isFocusedTask();
        T key = dcv.getAttachedKey();
        int taskIndex = mCallback.getData().indexOf(key);
        onStackPageRemoved(dcv);
        // If the dismissed task was focused, then we should focus the new task in the same index
        if (taskIndex != -1 && taskWasFocused) {
            int nextTaskIndex = Math.min(mCallback.getData().size() - 1, taskIndex - 1);
            if (nextTaskIndex >= 0) {
                PageView nextTv = getPageViewForTask(mCallback.getData().get(nextTaskIndex));
                if (nextTv != null) {
                    // Focus the next task, and only animate the visible state if we are launched
                    // from Alt-Tab
                    nextTv.setFocusedTask(mConfig.launchedWithAltTab);
                }
            }
        }
    }
    @Override
    public void onPageViewClipStateChanged(PageView tv) {
        if (!mPageStackViewDirty) {
            invalidate();
        }
    }
    @Override
    public void onPageViewFocusChanged(PageView<T> tv, boolean focused) {
        if (focused) {
            mFocusedTaskIndex = mCallback.getData().indexOf(tv.getAttachedKey());
        }
    }
    /**
     * * PageStackViewScroller implementation ***
     */
    @Override
    public void onScrollChanged(float p) {
        mUIDozeTrigger.poke();
        requestSynchronizeStackViewsWithModel();
        postInvalidateOnAnimation();
    }
}
