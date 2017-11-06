package com.example.a10330.pageviewtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.a10330.pageviewtest.utilities.Datum;
import com.example.a10330.pageviewtest.views.PageStackView;
import com.example.a10330.pageviewtest.views.PageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

/**
 * Basic sample for PageStackView.
 * Images are downloaded and cached using
 * Picasso "http://square.github.io/picasso/".
 * PageStackView is *very* young & can only
 * afford basic functionality.
 */
public class PageStackViewActivity extends AppCompatActivity {
    // View that stacks its children like a PageStack of cards
    PageStackView<Datum> mPageStackView;

    Drawable mDefaultHeaderIcon;
    ArrayList<Datum> mEntries;

    // Placeholder for when the image is being downloaded
    Bitmap mDefaultThumbnail;

    // Retain position on configuration change
    // imageSize to pass to http://lorempixel.com
    int scrollToChildIndex = -1, imageSize = 500;

    // SavedInstance bundle keys
    final String CURRENT_SCROLL = "current.scroll", CURRENT_LIST = "current.list";
    private Drawable mBackgroundScrim=new ColorDrawable(Color.argb((int)(0.93*255),255,165,0)).mutate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }*/
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_page_view);
//        getWindow().setBackgroundDrawable(mBackgroundScrim);

        mPageStackView = (PageStackView) findViewById(R.id.pageStackView);
        mDefaultThumbnail = BitmapFactory.decodeResource(getResources(),
                R.drawable.default_thumbnail);
        mDefaultHeaderIcon = getResources().getDrawable(R.drawable.default_header_icon);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CURRENT_LIST)) {
                mEntries = savedInstanceState.getParcelableArrayList(CURRENT_LIST);
            }

            if (savedInstanceState.containsKey(CURRENT_SCROLL)) {
                scrollToChildIndex = savedInstanceState.getInt(CURRENT_SCROLL);
            }
        }

        if (mEntries == null) {
            mEntries = new ArrayList<>();

            for (int i = 1; i < 100; i++) {
                Datum datum = new Datum();
                datum.id = generateUniqueKey();
                datum.link = "http://lorempixel.com/" + imageSize + "/" + imageSize
                        + "/sports/" + "ID " + datum.id + "/";
                datum.headerTitle = "Image ID " + datum.id;
                mEntries.add(datum);
            }
        }

        // Callback implementation
        PageStackView.Callback<Datum> PageStackViewCallback = new PageStackView.Callback<Datum>() {
            @Override
            public ArrayList<Datum> getData() {
                return mEntries;
            }

            @Override
            public void loadViewData(WeakReference<PageView<Datum>> dcv, Datum item) {
                loadViewDataInternal(item, dcv);
            }

            @Override
            public void unloadViewData(Datum item) {
                Picasso.with(PageStackViewActivity.this).cancelRequest(item.target);
            }

            @Override
            public void onViewDismissed(Datum item) {
                mEntries.remove(item);
                mPageStackView.notifyDataSetChanged();
            }

            @Override
            public void onItemClick(Datum item) {
                Toast.makeText(PageStackViewActivity.this,
                        "Item with title: '" + item.headerTitle + "' clicked",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoViewsToPageStack() {
                Toast.makeText(PageStackViewActivity.this,
                        "No views to show",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mPageStackView.initialize(PageStackViewCallback);

        if (scrollToChildIndex != -1) {
            mPageStackView.post(new Runnable() {
                @Override
                public void run() {
                    // Restore scroll position
                    mPageStackView.scrollToChild(scrollToChildIndex);
                }
            });
        }
    }
    void loadViewDataInternal(final Datum datum,
                              final WeakReference<PageView<Datum>> weakView) {
        // datum.target can be null
        Picasso.with(PageStackViewActivity.this).cancelRequest(datum.target);

        datum.target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // Pass loaded Bitmap to view
                if (weakView.get() != null) {
                    weakView.get().onDataLoaded(datum, bitmap,
                            mDefaultHeaderIcon, datum.headerTitle, Color.DKGRAY);
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                // Loading failed. Pass default thumbnail instead
                if (weakView.get() != null) {
                    weakView.get().onDataLoaded(datum, mDefaultThumbnail,
                            mDefaultHeaderIcon, datum.headerTitle + " Failed", Color.DKGRAY);
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // Pass the default thumbnail for now. It will
                // be replaced once the target Bitmap has been loaded
                if (weakView.get() != null) {
                    weakView.get().onDataLoaded(datum, mDefaultThumbnail,
                            mDefaultHeaderIcon, "Loading...", Color.DKGRAY);
                }
            }
        };

        // Begin loading
        Picasso.with(PageStackViewActivity.this).load(datum.link).into(datum.target);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pagestack_view_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Add a new item to the end of the list
        if (id == R.id.action_add) {
            Datum datum = new Datum();
            datum.id = generateUniqueKey();
            datum.headerTitle = "(New) Image ID " + datum.id;
            datum.link = "http://lorempixel.com/" + imageSize + "/" + imageSize
                    + "/sports/" + "ID " + datum.id + "/";
            mEntries.add(datum);
            mPageStackView.notifyDataSetChanged();
            return true;
        } else if (id == R.id.action_add_multiple) {
            // Add multiple items (between 5 & 10 items)
            // at random indices
            Random rand = new Random();

            // adding between 5 and 10 items
            int numberOfItemsToAdd = rand.nextInt(6) + 5;

            for (int i = 0; i < numberOfItemsToAdd; i++) {
                int atIndex = mEntries.size() > 0 ?
                        rand.nextInt(mEntries.size()) : 0;

                Datum datum = new Datum();
                datum.id = generateUniqueKey();
                datum.link = "http://lorempixel.com/" + imageSize + "/" + imageSize
                        + "/sports/" + "ID " + datum.id + "/";
                datum.headerTitle = "(New) Image ID " + datum.id;
                mEntries.add(atIndex, datum);
            }

            mPageStackView.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save current scroll and the list
        int currentChildIndex = mPageStackView.getCurrentChildIndex();
        outState.putInt(CURRENT_SCROLL, currentChildIndex);
        outState.putParcelableArrayList(CURRENT_LIST, mEntries);

        super.onSaveInstanceState(outState);
    }

    // Generates a key that will remain unique
    // during the application's lifecycle
    private static int generateUniqueKey() {
        return ++KEY;
    }

    private static int KEY = 0;
}
