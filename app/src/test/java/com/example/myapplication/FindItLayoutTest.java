package com.example.myapplication;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public final class FindItLayoutTest {
    private static final int[] LAYOUTS = {R.layout.activity_main, R.layout.screen_login,
            R.layout.screen_register, R.layout.screen_home, R.layout.screen_report, R.layout.screen_detail,
            R.layout.screen_claims, R.layout.screen_claim_detail, R.layout.screen_history,
            R.layout.screen_account, R.layout.screen_edit, R.layout.row_item, R.layout.row_event,
            R.layout.row_review, R.layout.row_proof, R.layout.panel_pickup, R.layout.panel_timeline};

    @Test public void layoutsMeasureAtCompactAndTabletWidthsWithLargerText() {
        for (String size : new String[]{"w320dp-h660dp", "w800dp-h1000dp"}) {
            RuntimeEnvironment.setQualifiers(size);
            Context app = RuntimeEnvironment.getApplication();
            for (float scale : new float[]{1.0f, 1.5f}) {
                Configuration config = new Configuration(app.getResources().getConfiguration());
                config.fontScale = scale;
                Context context = new ContextThemeWrapper(app.createConfigurationContext(config),
                        R.style.Theme_FindIt);
                measureLayouts(context);
            }
        }
    }

    private static void measureLayouts(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        FrameLayout container = new FrameLayout(context);
        int width = context.getResources().getDisplayMetrics().widthPixels;
        for (int layout : LAYOUTS) {
            View screen = inflater.inflate(layout, container, false);
            assertNotNull(screen);
            screen.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            assertEquals(width, screen.getMeasuredWidth());
            assertTrue(screen.getMeasuredHeight() > 0);
        }
    }
}
