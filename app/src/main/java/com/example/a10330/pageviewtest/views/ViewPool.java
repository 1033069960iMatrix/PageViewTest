package com.example.a10330.pageviewtest.views;

import android.content.Context;

import java.util.Iterator;
import java.util.LinkedList;
/**
 * Created by 10330 on 2017/11/5.
 */

class ViewPool<V,T> {
    /* An interface to the consumer of a view pool */
    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        void prepareViewToEnterPool(V v);

        void prepareViewToLeavePool(V v, T prepareData, boolean isNewView);

        boolean hasPreferredData(V v, T preferredData);
    }
    private Context mContext;
    private ViewPoolConsumer<V, T> mViewPoolConsumer;
    private LinkedList<V> mViewList = new LinkedList<>();
    /**
     * Initializes the pool with a fixed predetermined pool size
     */
    ViewPool(Context context, ViewPoolConsumer<V, T> consumer) {
        mContext = context;
        mViewPoolConsumer = consumer;
    }
    /**
     * Returns a view into the pool
     */
    void returnViewToPool(V v) {
        mViewPoolConsumer.prepareViewToEnterPool(v);
        mViewList.push(v);
    }
    /**
     * Gets a view from the pool and prepares it
     */
    V pickUpViewFromPool(T preferredData, T prepareData) {
        V v = null;
        boolean isNewView = false;
        if (mViewList.isEmpty()) {
            v = mViewPoolConsumer.createView(mContext);
            isNewView = true;
        } else {
            // Try and find a preferred view
            Iterator<V> iter = mViewList.iterator();
            while (iter.hasNext()) {
                V vpv = iter.next();
                if (mViewPoolConsumer.hasPreferredData(vpv, preferredData)) {
                    v = vpv;
                    iter.remove();
                    break;
                }
            }
            // Otherwise, just grab the first view
            if (v == null) {
                v = mViewList.pop();
            }
        }
        mViewPoolConsumer.prepareViewToLeavePool(v, prepareData, isNewView);
        return v;
    }
}
