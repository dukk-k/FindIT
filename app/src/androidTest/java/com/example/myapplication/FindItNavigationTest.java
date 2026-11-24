package com.example.myapplication;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public final class FindItNavigationTest {
    @Test public void registerReportRestoreSessionAndSignOut() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String item = "Buku PBO " + unique;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitDatabase();
            var ready = new java.util.concurrent.CountDownLatch(1);
            scenario.onActivity(activity -> activity.findViewById(R.id.splash).postDelayed(ready::countDown, 450));
            org.junit.Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
            onView(withId(R.id.switch_auth)).perform(scrollTo(), click());
            fill(R.id.name, "Penguji FindIt");
            fill(R.id.email, unique + "@kampus.test");
            fill(R.id.phone, "08123456789");
            fill(R.id.password, "sandi-aman-2026");
            fill(R.id.confirm, "sandi-aman-2026");
            onView(withId(R.id.submit)).perform(scrollTo(), click());
            awaitDatabase();

            onView(withText("Daftar Barang")).check(matches(isDisplayed()));
            onView(withId(R.id.nav_report)).perform(click());
            fill(R.id.name, item);
            onView(withId(R.id.category)).perform(scrollTo(), click());
            onView(withText("Perlengkapan")).perform(click());
            fill(R.id.color, "Biru");
            fill(R.id.location, "Perpustakaan");
            fill(R.id.description, "Buku kelas PBO");
            onView(withId(R.id.submit)).perform(scrollTo(), click());
            awaitDatabase();

            fill(R.id.search, unique);
            awaitDatabase();
            onView(withText(item)).perform(scrollTo(), click());
            awaitDatabase();
            onView(withText("Detail Barang")).check(matches(isDisplayed()));
            onView(withText(item)).check(matches(isDisplayed()));
            pressBack();
            awaitDatabase();

            scenario.recreate();
            awaitDatabase();
            onView(withText("Daftar Barang")).check(matches(isDisplayed()));
            onView(withId(R.id.nav_claims)).perform(click());
            awaitDatabase();
            onView(withText("Klaim Saya")).check(matches(isDisplayed()));
            onView(withText("Kelola Klaim")).perform(click());
            awaitDatabase();

            onView(withId(R.id.nav_history)).perform(click());
            awaitDatabase();
            onView(withText(item)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_account)).perform(click());
            onView(withId(R.id.logout)).perform(scrollTo(), click());
            onView(allOf(withId(android.R.id.button1), withText("Keluar"))).perform(click());
            awaitDatabase();
            onView(withId(R.id.wordmark)).check(matches(isDisplayed()));
        }
    }

    private static void fill(int id, String text) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }

    private static void awaitDatabase() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        var app = (FindItApplication) instrumentation.getTargetContext().getApplicationContext();
        instrumentation.waitForIdleSync();
        app.worker.submit(() -> {}).get(15, TimeUnit.SECONDS);
        instrumentation.waitForIdleSync();
        app.worker.submit(() -> {}).get(15, TimeUnit.SECONDS);
        instrumentation.waitForIdleSync();
    }
}
