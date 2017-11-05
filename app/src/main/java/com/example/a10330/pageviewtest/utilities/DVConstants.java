package com.example.a10330.pageviewtest.utilities;
//ok
/**
 * Created by 10330 on 2017/11/5.
 */

public class DVConstants {
    public static class DebugFlags {
        public static class App {
            // Enables the filtering of tasks according to their grouping
            public static final boolean EnableTaskFiltering = false;
            // Enables clipping of tasks against each other
            public static final boolean EnablePageStackClipping = true;
            // Enables tapping on the TaskBar to launch the task
            public static final boolean EnableTaskBarTouchEvents = true;
            // Enables app-info pane on long-pressing the icon
            public static final boolean EnableDevAppInfoOnLongPress = true;
        }
    }
    public static class Values {
        public static class App {
            public static String Key_DebugModeEnabled = "debugModeEnabled";
        }

        public static class DView {
            public static final int PageStackMinOverscrollRange = 32;
            public static final int PageStackMaxOverscrollRange = 128;
        }
    }
}
