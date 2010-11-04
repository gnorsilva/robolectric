package com.xtremelabs.robolectric;

import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.test.ClassWithNoDefaultConstructor;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.xtremelabs.robolectric.shadows.ShadowItemizedOverlay;
import com.xtremelabs.robolectric.util.Implementation;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;

import static com.xtremelabs.robolectric.Robolectric.directlyOn;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(DogfoodRobolectricTestRunner.class)
public class AndroidTranslatorTest {

    @Test
    public void should() throws Exception {
        LinearLayout linearLayout = new LinearLayout(null);
        View child = new View(null);
        directlyOn(linearLayout).addView(child);
    }

    @Test
    public void testStaticMethodsAreDelegated() throws Exception {
        Robolectric.bindShadowClass(AccountManager.class, ShadowAccountManagerForTests.class);

        Context context = mock(Context.class);
        AccountManager.get(context);
        assertThat(ShadowAccountManagerForTests.wasCalled, is(true));
        assertThat(ShadowAccountManagerForTests.context, sameInstance(context));
    }

    @Test
    public void testProtectedMethodsAreDelegated() throws Exception {
        Robolectric.bindShadowClass(ItemizedOverlay.class, ShadowItemizedOverlay.class);

        ItemizedOverlayForTests overlay = new ItemizedOverlayForTests(null);
        overlay.triggerProtectedCall();

        assertThat(shadowOf(overlay).populated, is(true));
    }

    @Test
    public void testNativeMethodsAreDelegated() throws Exception {
        Robolectric.bindShadowClass(Paint.class, ShadowPaintForTests.class);

        Paint paint = new Paint();
        paint.setColor(1234);

        assertThat(paint.getColor(), is(1234));
    }

    @Test
    public void testPrintlnWorks() throws Exception {
        Log.println(1, "tag", "msg");
    }

    @Test
    public void testGeneratedDefaultConstructorIsWired() throws Exception {
        Robolectric.bindShadowClass(ClassWithNoDefaultConstructor.class, ShadowClassWithNoDefaultConstructors.class);

        Constructor<ClassWithNoDefaultConstructor> ctor = ClassWithNoDefaultConstructor.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ClassWithNoDefaultConstructor instance = ctor.newInstance();
        assertThat(Robolectric.shadowOf_(instance), not(nullValue()));
        assertThat(Robolectric.shadowOf_(instance), instanceOf(ShadowClassWithNoDefaultConstructors.class));
    }

    @Test
    public void testDirectlyOn() throws Exception {
        View view = new View(null);
        view.bringToFront();

        Exception e = null;
        try {
            directlyOn(view).bringToFront();
        } catch (RuntimeException e1) {
            e = e1;
        }
        assertNotNull(e);
        assertEquals("Stub!", e.getMessage());

        view.bringToFront();
    }

    @Test
    public void testDirectlyOn_Statics() throws Exception {
        View.resolveSize(0, 0);

        Exception e = null;
        try {
            directlyOn(View.class);
            View.resolveSize(0, 0);
        } catch (RuntimeException e1) {
            e = e1;
        }
        assertNotNull(e);
        assertEquals("Stub!", e.getMessage());

        View.resolveSize(0, 0);
    }

    @Test
    public void testDirectlyOn_InstanceChecking() throws Exception {
        View view1 = new View(null);
        View view2 = new View(null);

        Exception e = null;
        try {
            directlyOn(view1);
            view2.bringToFront();
        } catch (RuntimeException e1) {
            e = e1;
        }
        assertNotNull(e);
        assertThat(e.getMessage(), startsWith("expected to perform direct call on <android.view.View"));
        assertThat(e.getMessage(), containsString("> but got <android.view.View"));
    }

    @Test
    public void testDirectlyOn_Statics_InstanceChecking() throws Exception {
        TextView.getTextColors(null, null);

        Exception e = null;
        try {
            directlyOn(View.class);
            TextView.getTextColors(null, null);
        } catch (RuntimeException e1) {
            e = e1;
        }
        assertNotNull(e);
        assertThat(e.getMessage(), equalTo("expected to perform direct call on <class android.view.View> but got <class android.widget.TextView>"));
    }

    @Test
    public void testDirectlyOn_CallTwiceChecking() throws Exception {
        directlyOn(View.class);

        Exception e = null;
        try {
            directlyOn(View.class);
        } catch (RuntimeException e1) {
            e = e1;
        }
        assertNotNull(e);
        assertThat(e.getMessage(), equalTo("already expecting a direct call on <class android.view.View> but here's a new request for <class android.view.View>"));
    }

    public static class ItemizedOverlayForTests extends ItemizedOverlay {
        public ItemizedOverlayForTests(Drawable drawable) {
            super(drawable);
        }

        @Override
        protected OverlayItem createItem(int i) {
            return null;
        }

        public void triggerProtectedCall() {
            populate();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class ShadowPaintForTests {
        private int color;

        @Implementation
        public void setColor(int color) {
            this.color = color;
        }

        @Implementation
        public int getColor() {
            return color;
        }
    }

    public static class ShadowAccountManagerForTests {
        public static boolean wasCalled = false;
        public static Context context;

        public static AccountManager get(Context context) {
            wasCalled = true;
            ShadowAccountManagerForTests.context = context;
            return mock(AccountManager.class);
        }
    }

    public static class ShadowClassWithNoDefaultConstructors {
    }
}
