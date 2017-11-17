package com.example.a10330.pageviewtest.views;

import android.graphics.Rect;

import com.example.a10330.pageviewtest.helpers.PageStackViewConfig;
import com.example.a10330.pageviewtest.helpers.PageViewTransform;
import com.example.a10330.pageviewtest.utilities.DVUtils;

import java.util.ArrayList;
import java.util.HashMap;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */
/* The layout logic for a PageStackView.
 *
 * We are using a curve that defines the curve of the tasks as that go back in the recents list.
 * The curve is defined such that at curve progress p = 0 is the end of the curve (the top of the
 * stack rect), and p = 1 at the start of the curve and the bottom of the stack rect.
 */
class PageStackViewLayoutAlgorithm<T> {
    //曾经有个VisibilityReport内部类，不知道有什么用
    private static final float StackPeekMinScale = 0.8f; // The min scale of the last card in the peek area 初始0.8负责最上面卡片的缩放比，
    private PageStackViewConfig mConfig;
    //mViewRect就是mStackVisibleRect   mStackRect和mTaskRect是加入上下padding的
    Rect mViewRect = new Rect();
    Rect mStackVisibleRect = new Rect();
    Rect mTaskRect = new Rect();
    private Rect mStackRect = new Rect();
    // The min/max scroll progress
    float mMinScrollP;
    float mMaxScrollP;
    float mInitialScrollP;
    private int mBetweenAffiliationOffset;
    private HashMap<T, Float> mTaskProgressMap = new HashMap<T, Float>();
    // Log function
    private static final float XScale = 1.75f;  // The large the XScale, the longer the flat area of the curve调整矩形队列同一侧上下间角的路径
    private static final float LogBase = 3000;
    private static final int PrecisionSteps = 250;
    private static float[] xp;
    private static float[] px;
    PageStackViewLayoutAlgorithm(PageStackViewConfig config) {
        mConfig = config;
        // Precompute the path
        initializeCurve();
    }
    /**
     * Computes the stack and task rects
     */
    void computeRects(int windowWidth, int windowHeight, Rect pageStackBounds) {//此时，pageStackBounds设置的也是前面的宽高，就是stack View的onmeasure得出的
        mViewRect.set(0, 0, windowWidth, windowHeight);
        mStackRect.set(pageStackBounds);//mViewRect和mStackViewRect和mStackVisibleRect在这个程序里是一样的
        mStackVisibleRect.set(pageStackBounds);
        int widthPadding = (int) (mConfig.pageStackWidthPaddingPct * mStackRect.width());//47，主要负责每个卡片的宽度，100是自己加的
        int heightPadding = mConfig.pageStackTopPaddingPx;
        mStackRect.inset(widthPadding, heightPadding);
        mTaskRect.set(mStackRect);
        // Update the affiliation offset
        float visibleTaskPct = 0.5f;//原来0.5
        mBetweenAffiliationOffset = (int) (visibleTaskPct * mTaskRect.height());//每个卡片上下间的距离
    }
    /**
     * Computes the minimum and maximum scroll progress values.  This method may be called before
     * the RecentsConfiguration is set, so we need to pass in the alt-tab state.
     */
    void computeMinMaxScroll(ArrayList<T> data, boolean launchedWithAltTab, boolean launchedFromHome) {
        // Clear the progress map
        mTaskProgressMap.clear();// TODO: 2017/11/12 为何是用data绑定的呢 而不是pageView？
        // Return early if we have no tasks
        if (data.isEmpty()) {
            mMinScrollP = mMaxScrollP = 0;
            return;
        }
        // Note that we should account for the scale difference of the offsets at the screen bottom
        int taskHeight = mTaskRect.height();
        float pAtBottomOfStackRect = screenYToCurveProgress(mStackVisibleRect.bottom);
        float pBetweenAffiliateOffset = pAtBottomOfStackRect -
                screenYToCurveProgress(mStackVisibleRect.bottom - mBetweenAffiliationOffset);
        float pTaskHeightOffset = pAtBottomOfStackRect -
                screenYToCurveProgress(mStackVisibleRect.bottom - taskHeight);
        float pNavBarOffset = pAtBottomOfStackRect -
                screenYToCurveProgress(mStackVisibleRect.bottom - (mStackVisibleRect.bottom -
                        mStackRect.bottom));
        // Update the task offsets
        float pAtFrontMostCardTop = 0.1f;//0.5f默认,数值改大了会导致最上面的划走了回不来，负责1号卡片的最下滑动位置
        int taskCount = data.size();
        for (int i = 0; i < taskCount; i++) {
            //Task task = tasks.get(i);
            //mTaskProgressMap.put(task.key, pAtFrontMostCardTop);
            mTaskProgressMap.put(data.get(i), pAtFrontMostCardTop);
            if (i < (taskCount - 1)) {
                // Increment the peek height
                //float pPeek = task.group.isFrontMostTask(task) ?
                //pBetweenAffiliateOffset : pWithinAffiliateOffset;
                pAtFrontMostCardTop += pBetweenAffiliateOffset;//这个值我给改成/2后能控制每页显示的卡片数
            }
        }
        mMaxScrollP = pAtFrontMostCardTop - ((1f - pTaskHeightOffset - pNavBarOffset));
        mMinScrollP = data.size() == 1 ? Math.max(mMaxScrollP, 0f) : 0f;
        if (launchedWithAltTab && launchedFromHome) {//如果条件为真，初始的画面就会在高处
            // Center the top most task, since that will be focused first
            mInitialScrollP = mMaxScrollP;
        } else {
            mInitialScrollP = pAtFrontMostCardTop - 0.825f;
        }
        mInitialScrollP = Math.min(mMaxScrollP, Math.max(0, mInitialScrollP));
    }
    /**
     * Update/get the transform
     */
    PageViewTransform getStackTransform(T key, float stackScroll,
                                               PageViewTransform transformOut,
                                               PageViewTransform prevTransform) {
        // Return early if we have an invalid index
        if (!mTaskProgressMap.containsKey(key)) {
            transformOut.reset();
            return transformOut;
        }
        return getStackTransform(mTaskProgressMap.get(key), stackScroll, transformOut, prevTransform);
    }
    /**
     * Update/get the transform
     */
    PageViewTransform getStackTransform(float taskProgress, float stackScroll,
                                                    PageViewTransform transformOut,
                                                    PageViewTransform prevTransform) {
        float pTaskRelative = taskProgress - stackScroll;
        float pBounded = Math.max(0, Math.min(pTaskRelative, 1f));
        // If the task top is outside of the bounds below the screen, then immediately reset it
        if (pTaskRelative > 1f) {
            transformOut.reset();
            transformOut.rect.set(mTaskRect);
            return transformOut;
        }
        // The check for the top is trickier狡猾的, since we want to show the next task if it is at all
        // visible, even if p < 0.
        if (pTaskRelative < 0f) {
            if (prevTransform != null && Float.compare(prevTransform.p, 0f) <= 0) {
                transformOut.reset();
                transformOut.rect.set(mTaskRect);
                return transformOut;
            }
        }
        float scale = curveProgressToScale(pBounded);//0.8和1之间的值
        int scaleYOffset = (int) (((1f - scale) * mTaskRect.height()) / 2);
        int minZ = mConfig.pageViewTranslationZMinPx;
        int maxZ = mConfig.pageViewTranslationZMaxPx;
        transformOut.scale = scale;
        transformOut.translationY = curveProgressToScreenY(pBounded) - mStackVisibleRect.top -
                scaleYOffset;
        transformOut.translationZ = Math.max(minZ, minZ + (pBounded * (maxZ - minZ)));//每个卡片的阴影是由这个控制的
        transformOut.rect.set(mTaskRect);
        transformOut.rect.offset(0, transformOut.translationY);
        DVUtils.scaleRectAboutCenter(transformOut.rect, transformOut.scale);
        transformOut.visible = true;
        transformOut.p = pTaskRelative;
        return transformOut;
    }
    /**
     * Returns the scroll to such task top = 1f;
     */
    float getStackScrollForTask(T key) {
        if (!mTaskProgressMap.containsKey(key)) return 0f;
        return mTaskProgressMap.get(key);
    }
    /**
     * Converts from the progress along the curve to a screen coordinate.
     */
    private int curveProgressToScreenY(float p) {
        if (p < 0 || p > 1) return mStackVisibleRect.top + (int) (p * mStackVisibleRect.height());
        float pIndex = p * PrecisionSteps;
        int pFloorIndex = (int) Math.floor(pIndex);
        int pCeilIndex = (int) Math.ceil(pIndex);
        float xFraction = 0;
        if (pFloorIndex < PrecisionSteps && (pCeilIndex != pFloorIndex)) {
            float pFraction = (pIndex - pFloorIndex) / (pCeilIndex - pFloorIndex);
            xFraction = (xp[pCeilIndex] - xp[pFloorIndex]) * pFraction;
        }
        float x = xp[pFloorIndex] + xFraction;
        return mStackVisibleRect.top + (int) (x * mStackVisibleRect.height());
    }
    /**
     * Converts from the progress along the curve to a scale.
     */
    private float curveProgressToScale(float p) {
        if (p < 0) return StackPeekMinScale;
        if (p > 1) return 1f;
        float scaleRange = (1f - StackPeekMinScale);
        return StackPeekMinScale + (p * scaleRange);
    }
    /**
     * Converts from a screen coordinate to the progress along the curve.
     */
    float screenYToCurveProgress(int screenY) {
        float x = (float) (screenY - mStackVisibleRect.top) / mStackVisibleRect.height();
//        if (x < 0 || x > 1) return x;
        float xIndex = x * PrecisionSteps;
        int xFloorIndex = (int) Math.floor(xIndex);
        int xCeilIndex = (int) Math.ceil(xIndex);
        float pFraction = 0;
        if (xFloorIndex < PrecisionSteps && (xCeilIndex != xFloorIndex)) {
            float xFraction = (xIndex - xFloorIndex) / (xCeilIndex - xFloorIndex);
            pFraction = (px[xCeilIndex] - px[xFloorIndex]) * xFraction;
        }
        return px[xFloorIndex] + pFraction;
    }
    /**
     * Initializes the curve.
     */
    private static void initializeCurve() {
        if (xp != null && px != null) return;
        xp = new float[PrecisionSteps + 1];
        px = new float[PrecisionSteps + 1];
        // Approximate f(x)
        float[] fx = new float[PrecisionSteps + 1];
        float step = 1f / PrecisionSteps;
        float x = 0;
        for (int xStep = 0; xStep <= PrecisionSteps; xStep++) {
            fx[xStep] = logFunc(x);
            x += step;
        }
        // Calculate the arc length for x:1->0
        float pLength = 0;
        float[] dx = new float[PrecisionSteps + 1];
        dx[0] = 0;
        for (int xStep = 1; xStep < PrecisionSteps; xStep++) {
            dx[xStep] = (float) Math.sqrt(Math.pow(fx[xStep] - fx[xStep - 1], 2) + Math.pow(step, 2));
            pLength += dx[xStep];
        }
        // Approximate近似的 p(x), a function of cumulative累计的 progress with x, normalized to 0..1
        float p = 0;
        px[0] = 0f;
        px[PrecisionSteps] = 1f;
        for (int xStep = 1; xStep <= PrecisionSteps; xStep++) {
            p += Math.abs(dx[xStep] / pLength);
            px[xStep] = p;
        }
        // Given p(x), calculate the inverse function x(p). This assumes that x(p) is also a valid
        // function.
        int xStep = 0;
        p = 0;
        xp[0] = 0f;
        xp[PrecisionSteps] = 1f;
        for (int pStep = 0; pStep < PrecisionSteps; pStep++) {
            // Walk forward in px and find the x where px <= p && p < px+1
            while (xStep < PrecisionSteps) {
                if (px[xStep] > p) break;
                xStep++;
            }
            // Now, px[xStep-1] <= p < px[xStep]
            if (xStep == 0) {
                xp[pStep] = 0;
            } else {
                // Find x such that proportionally, x is correct
                float fraction = (p - px[xStep - 1]) / (px[xStep] - px[xStep - 1]);
                x = (xStep - 1 + fraction) * step;
                xp[pStep] = x;
            }
            p += step;
        }
    }
    /**
     * Reverses and scales out x.
     */
    private static float reverse(float x) {
        return (-x * XScale) + 1;
    }
    /**
     * The log function describing the curve.
     */
    private static float logFunc(float x) {
        return 1f - (float) (Math.pow(LogBase,reverse(x)) / (LogBase));
//        return (float) 1/x;
    }
}
