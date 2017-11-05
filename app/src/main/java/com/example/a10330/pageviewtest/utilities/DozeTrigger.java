package com.example.a10330.pageviewtest.utilities;

import android.os.Handler;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class DozeTrigger {

    Handler mHandler;

    boolean mIsDozing;
    boolean mHasTriggered;
    int mDozeDurationSeconds;
    Runnable mSleepRunnable;

    // Sleep-runnable
    Runnable mDozeRunnable = new Runnable() {
        @Override
        public void run() {
            mSleepRunnable.run();
            mIsDozing = false;
            mHasTriggered = true;
        }
    };

    public DozeTrigger(int dozeDurationSeconds, Runnable sleepRunnable) {
        mHandler = new Handler();
        mDozeDurationSeconds = dozeDurationSeconds;
        mSleepRunnable = sleepRunnable;
    }

    /**
     * Starts dozing. This also resets the trigger flag.
     */
    public void startDozing() {
        forcePoke();
        mHasTriggered = false;
    }

    /**
     * Stops dozing.
     */
    public void stopDozing() {
        mHandler.removeCallbacks(mDozeRunnable);
        mIsDozing = false;
    }

    /**
     * Poke this dozer to wake it up for a little bit, if it is dozing.
     */
    public void poke() {
        if (mIsDozing) {
            forcePoke();
        }
    }

    /**
     * Poke this dozer to wake it up for a little bit.
     */
    void forcePoke() {
        mHandler.removeCallbacks(mDozeRunnable);
        mHandler.postDelayed(mDozeRunnable, mDozeDurationSeconds * 1000);
        mIsDozing = true;
    }
    /**
     * Returns whether the trigger has fired at least once.
     */
    public boolean hasTriggered() {
        return mHasTriggered;
    }

    /**
     * Resets the doze trigger state.
     */
    public void resetTrigger() {
        mHasTriggered = false;
    }
}
