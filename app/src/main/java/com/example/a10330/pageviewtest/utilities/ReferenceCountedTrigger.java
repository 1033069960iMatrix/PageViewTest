package com.example.a10330.pageviewtest.utilities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;

import java.util.ArrayList;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class ReferenceCountedTrigger {

    private Context mContext;
    private int mCount;
    private ArrayList<Runnable> mFirstIncRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> mLastDecRunnables = new ArrayList<Runnable>();
    private Runnable mErrorRunnable;
    // Convenience runnables
    private Runnable mDecrementRunnable = new Runnable() {
        @Override
        public void run() {
            decrement();
        }
    };

    public ReferenceCountedTrigger(Context context, Runnable firstIncRunnable, Runnable lastDecRunnable, Runnable errorRunanable) {
        mContext = context;
        if (firstIncRunnable != null) mFirstIncRunnables.add(firstIncRunnable);
        if (lastDecRunnable != null) mLastDecRunnables.add(lastDecRunnable);
        mErrorRunnable = errorRunanable;
    }

    /**
     * Increments the ref count
     */
    public void increment() {
        if (mCount == 0 && !mFirstIncRunnables.isEmpty()) {
            int numRunnables = mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mFirstIncRunnables.get(i).run();
            }
        }
        mCount++;
    }
    /**
     * Adds a runnable to the last-decrement runnables list.
     */
    public void addLastDecrementRunnable(Runnable r) {
        // To ensure that the last decrement always calls, we increment and decrement after setting
        // the last decrement runnable
        boolean ensureLastDecrement = (mCount == 0);
        if (ensureLastDecrement) increment();
        mLastDecRunnables.add(r);
        if (ensureLastDecrement) decrement();
    }

    /**
     * Decrements the ref count
     */
    public void decrement() {
        mCount--;
        if (mCount == 0 && !mLastDecRunnables.isEmpty()) {
            int numRunnables = mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mLastDecRunnables.get(i).run();
            }
        } else if (mCount < 0) {
            if (mErrorRunnable != null) {
                mErrorRunnable.run();
            } else {
                new Throwable("Invalid ref count").printStackTrace();
                //Console.logError(mContext, "Invalid ref count");
            }
        }
    }
    /**
     * Convenience method to decrement this trigger as a runnable.
     */
    public Runnable decrementAsRunnable() {
        return mDecrementRunnable;
    }

    /**
     * Convenience method to decrement this trigger as a animator listener.
     */
    public Animator.AnimatorListener decrementOnAnimationEnd() {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decrement();
            }
        };
    }

}
