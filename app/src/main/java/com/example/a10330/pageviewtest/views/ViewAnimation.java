package com.example.a10330.pageviewtest.views;

import android.animation.ValueAnimator;
import android.graphics.Rect;

import com.example.a10330.pageviewtest.helpers.PageViewTransform;
import com.example.a10330.pageviewtest.utilities.ReferenceCountedTrigger;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

class ViewAnimation {

    /* The animation context for a task view animation into Recents */
    public static class PageViewEnterContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;
        // An update listener to notify as the enter animation progresses (used for the home transition)
        ValueAnimator.AnimatorUpdateListener updateListener;

        // These following properties are updated for each task view we start the enter animation on

        // Whether or not the current task occludes the launch target
        boolean currentTaskOccludesLaunchTarget;
        // The task rect for the current stack
        Rect currentTaskRect;
        // The transform of the current task view
        PageViewTransform currentTaskTransform;
        // The view index of the current task view
        int currentStackViewIndex;
        // The total number of task views
        int currentStackViewCount;

        public PageViewEnterContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }
}
