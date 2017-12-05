package com.example.a10330.pageviewtest.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.example.a10330.pageviewtest.R;
import com.example.a10330.pageviewtest.utilities.DVConstants;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class PageStackViewConfig {
    static PageStackViewConfig sInstance;
    static int sPrevConfigurationHashCode;
    /**
     * Animations
     */
    public float animationPxMovementPerSecond;

    /**
     * Interpolators
     */
    public Interpolator fastOutSlowInInterpolator;
    public Interpolator fastOutLinearInInterpolator;
    public Interpolator linearOutSlowInInterpolator;
    public Interpolator quintOutInterpolator;

    /**
     * Filtering
     */
//    public int filteringCurrentViewsAnimDuration;
//    public int filteringNewViewsAnimDuration;

    /**
     * Insets
     */
//    public Rect systemInsets = new Rect();
//    public Rect displayRect = new Rect();

    /**
     * Layout
     */
//    boolean isLandscape;

    /**
     * Page stack
     */
    public int pageStackScrollDuration;
    public int pageStackMaxDim;
    public int pageStackTopPaddingPx;
    public float pageStackWidthPaddingPct;
    public float pageStackOverscrollPct;

    /**
     * Transitions
     */
//    public int transitionEnterFromAppDelay;
    public int transitionEnterFromHomeDelay;

    /**
     * Task view animation and styles
     */
    public int pageViewEnterFromAppDuration;
    public int pageViewEnterFromHomeDuration;
    public int pageViewEnterFromHomeStaggerDelay;
//    private int pageViewExitToAppDuration;
//    public int pageViewExitToHomeDuration;
    public int pageViewRemoveAnimDuration;
    public int pageViewRemoveAnimTranslationXPx;
    public int pageViewTranslationZMinPx;
    public int pageViewTranslationZMaxPx;
    public int pageViewRoundedCornerRadiusPx;
    public int pageViewHighlightPx;
//    public int pageViewAffiliateGroupEnterOffsetPx;
    public float pageViewThumbnailAlpha;

    /**
     * Task bar colors
     */
//    public int taskBarViewDefaultBackgroundColor;
    public int taskBarViewLightTextColor;
//    public int taskBarViewDarkTextColor;
    public int taskBarViewHighlightColor;
//    public float taskBarViewAffiliationColorMinAlpha;

    /**
     * Task bar size & animations
     */
    public int taskBarHeight;
    public int taskBarDismissDozeDelaySeconds;

    /**
     * Nav bar scrim
     */
//    public int navBarScrimEnterDuration;

    /**
     * Launch states
     */
    public boolean launchedWithAltTab;
    public boolean launchedFromAppWithThumbnail;
    public boolean launchedFromHome;
    public boolean launchedHasConfigurationChanged;
    /**
     * Misc *
     */
    public boolean useHardwareLayers;
//    public int altTabKeyDelay;
    public boolean fakeShadows;

    /**
     * Dev options and global settings
     */
//    public boolean debugModeEnabled;
//    public int svelteLevel;
    /**
     * Private constructor
     */
    private PageStackViewConfig(Context context) {
        // Properties that don't have to be reloaded with each configuration change can be loaded
        // here.
        // Interpolators
        fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.fast_out_slow_in);
        fastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.fast_out_linear_in);
        linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.linear_out_slow_in);
        quintOutInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.decelerate_quint);
    }
    /**
     * Updates the configuration to the current context
     */
    public static PageStackViewConfig reinitialize(Context context) {
        if (sInstance == null) {
            sInstance = new PageStackViewConfig(context);
        }
        int configHashCode = context.getResources().getConfiguration().hashCode();
        if (sPrevConfigurationHashCode != configHashCode) {
            sInstance.update(context);
            sPrevConfigurationHashCode = configHashCode;
        }

//        sInstance.updateOnReinitialize(context);
        return sInstance;
    }

    /**
     * Returns the current recents configuration
     */
    public static PageStackViewConfig getInstance() {
        return sInstance;
    }

    /**
     * Updates the state, given the specified context
     */
    void update(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName(), 0);
        Resources res = context.getResources();
//        DisplayMetrics dm = res.getDisplayMetrics();
        // Debug mode
//        debugModeEnabled = settings.getBoolean(DVConstants.Values.App.Key_DebugModeEnabled, false);
        // Layout
//        isLandscape = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        // Insets
//        displayRect.set(0, 0, dm.widthPixels, dm.heightPixels);
        // Animations
        animationPxMovementPerSecond = res.getDimensionPixelSize(R.dimen.animation_movement_in_dps_per_second);
        // Filtering
//        filteringCurrentViewsAnimDuration = res.getInteger(R.integer.filter_animate_current_views_duration);
//        filteringNewViewsAnimDuration = res.getInteger(R.integer.filter_animate_new_views_duration);
        // Task stack
        pageStackScrollDuration = res.getInteger(R.integer.animate_deck_scroll_duration);
        TypedValue widthPaddingPctValue = new TypedValue();
        res.getValue(R.dimen.deck_width_padding_percentage, widthPaddingPctValue, true);
        pageStackWidthPaddingPct = widthPaddingPctValue.getFloat();
        TypedValue stackOverscrollPctValue = new TypedValue();
        res.getValue(R.dimen.deck_overscroll_percentage, stackOverscrollPctValue, true);
        pageStackOverscrollPct = stackOverscrollPctValue.getFloat();
        pageStackMaxDim = res.getInteger(R.integer.max_deck_view_dim);
        pageStackTopPaddingPx = res.getDimensionPixelSize(R.dimen.deck_top_padding);

        // Transition
//        transitionEnterFromAppDelay =
                res.getInteger(R.integer.enter_from_app_transition_duration);
        transitionEnterFromHomeDelay =
                res.getInteger(R.integer.enter_from_home_transition_duration);

        // Task view animation and styles
        pageViewEnterFromAppDuration = res.getInteger(R.integer.task_enter_from_app_duration);
        pageViewEnterFromHomeDuration = res.getInteger(R.integer.task_enter_from_home_duration);
        pageViewEnterFromHomeStaggerDelay = res.getInteger(R.integer.task_enter_from_home_stagger_delay);
//        pageViewExitToAppDuration = res.getInteger(R.integer.task_exit_to_app_duration);
//        pageViewExitToHomeDuration = res.getInteger(R.integer.task_exit_to_home_duration);
        pageViewRemoveAnimDuration = res.getInteger(R.integer.animate_task_view_remove_duration);
        pageViewRemoveAnimTranslationXPx = res.getDimensionPixelSize(R.dimen.task_view_remove_anim_translation_x);
        pageViewRoundedCornerRadiusPx = res.getDimensionPixelSize(R.dimen.task_view_rounded_corners_radius);
        pageViewHighlightPx = res.getDimensionPixelSize(R.dimen.task_view_highlight);
        pageViewTranslationZMinPx = res.getDimensionPixelSize(R.dimen.task_view_z_min);
        pageViewTranslationZMaxPx = res.getDimensionPixelSize(R.dimen.task_view_z_max);
//        pageViewAffiliateGroupEnterOffsetPx = res.getDimensionPixelSize(R.dimen.task_view_affiliate_group_enter_offset);
        TypedValue thumbnailAlphaValue = new TypedValue();
        res.getValue(R.dimen.task_view_thumbnail_alpha, thumbnailAlphaValue, true);
        pageViewThumbnailAlpha = thumbnailAlphaValue.getFloat();

        // Task bar colors
//        taskBarViewDefaultBackgroundColor =
                res.getColor(R.color.task_bar_default_background_color);
        taskBarViewLightTextColor =
                res.getColor(R.color.task_bar_light_text_color);
//        taskBarViewDarkTextColor =
                res.getColor(R.color.task_bar_dark_text_color);
        taskBarViewHighlightColor =
                res.getColor(R.color.task_bar_highlight_color);
        TypedValue affMinAlphaPctValue = new TypedValue();
        res.getValue(R.dimen.task_affiliation_color_min_alpha_percentage, affMinAlphaPctValue, true);
//        taskBarViewAffiliationColorMinAlpha = affMinAlphaPctValue.getFloat();
        // Task bar size & animations
        taskBarHeight = res.getDimensionPixelSize(R.dimen.deck_child_header_bar_height);
        taskBarDismissDozeDelaySeconds = res.getInteger(R.integer.task_bar_dismiss_delay_seconds);
        // Nav bar scrim
//        navBarScrimEnterDuration = res.getInteger(R.integer.nav_bar_scrim_enter_duration);
        // Misc
        useHardwareLayers = res.getBoolean(R.bool.config_use_hardware_layers);
//        altTabKeyDelay = res.getInteger(R.integer.deck_alt_tab_key_delay);
        fakeShadows = res.getBoolean(R.bool.config_fake_shadows);
//        svelteLevel = res.getInteger(R.integer.deck_svelte_level);
    }
    /**
     * Updates the system insets
     */
/*
    public void updateSystemInsets(Rect insets) {
        systemInsets.set(insets);
    }
*/

    /**
     * Updates the states that need to be re-read whenever we re-initialize.
     */
    void updateOnReinitialize(Context context/*, SystemServicesProxy ssp*/) {
    }
    /**
     * Returns the task stack bounds in the current orientation. These bounds do not account for
     * the system insets.
     */
    public void getPageStackBounds(int windowWidth, int windowHeight, Rect pageStackBounds) {
        pageStackBounds.set(0, 0, windowWidth, windowHeight);
    }
}
