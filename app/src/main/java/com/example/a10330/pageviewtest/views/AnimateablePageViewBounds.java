package com.example.a10330.pageviewtest.views;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.example.a10330.pageviewtest.helpers.PageStackViewConfig;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */
/* An outline provider that has a clip and outline that can be animated. */
public class AnimateablePageViewBounds extends ViewOutlineProvider {
    private static final float MIN_ALPHA = 0.25f;
    private PageStackViewConfig mConfig;
    private PageView mSourceView;
    private Rect mClipRect = new Rect();// TODO: 2017/11/11 还没弄清楚两个矩形什么区别
    private Rect mClipBounds = new Rect();
    private int mCornerRadius;
    private float mAlpha = 1f;

    AnimateablePageViewBounds(PageView source, int cornerRadius) {
        mConfig = PageStackViewConfig.getInstance();
        mSourceView = source;
        mCornerRadius = cornerRadius;
        setClipBottom(mClipRect.bottom);
    }

    @Override
    public void getOutline(View view, Outline outline) {
        outline.setAlpha(MIN_ALPHA + mAlpha / (1f - MIN_ALPHA));
        outline.setRoundRect(mClipRect.left, mClipRect.top,
                mSourceView.getWidth() - mClipRect.right,
                mSourceView.getHeight() - mClipRect.bottom,// TODO: 2017/11/11不明白为何右边设置
                mCornerRadius);
    }

    /**
     * Sets the view outline alpha.
     */
    void setAlpha(float alpha) {
        if (Float.compare(alpha, mAlpha) != 0) {
            mAlpha = alpha;
            mSourceView.invalidateOutline();
        }
    }

    /**
     * Sets the bottom clip.
     */
    void setClipBottom(int bottom) {
        if (bottom != mClipRect.bottom) {
            mClipRect.bottom = bottom;
            mSourceView.invalidateOutline();
            updateClipBounds();
            if (!mConfig.useHardwareLayers) {
                mSourceView.mThumbnailView.updateThumbnailVisibility(
                        bottom - mSourceView.getPaddingBottom());
            }
        }
    }

    private void updateClipBounds() {
        mClipBounds.set(mClipRect.left, mClipRect.top,
                mSourceView.getWidth() - mClipRect.right,
                mSourceView.getHeight() - mClipRect.bottom);
        mSourceView.setClipBounds(mClipBounds);
    }
}
