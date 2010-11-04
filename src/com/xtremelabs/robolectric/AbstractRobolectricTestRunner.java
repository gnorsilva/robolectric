package com.xtremelabs.robolectric;

import com.xtremelabs.robolectric.util.RealObject;
import com.xtremelabs.robolectric.util.TestHelperInterface;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class AbstractRobolectricTestRunner extends BlockJUnit4ClassRunner {
    private static RobolectricClassLoader defaultLoader;
    static final boolean USE_REAL_ANDROID_SOURCES = true;

    private RobolectricClassLoader loader;
    private ClassHandler classHandler;
    private Class<? extends TestHelperInterface> testHelperClass;
    private TestHelperInterface testHelper;

    private static RobolectricClassLoader getDefaultLoader() {
        if (defaultLoader == null) {
            if (USE_REAL_ANDROID_SOURCES) {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{
                        parseUrl("file:///Users/pivotal/android/add-ons/addon_google_apis_google_inc_8/libs/maps.jar"),
                        parseUrl("file:///Volumes/AndroidSource/out/host/common/obj/JAVA_LIBRARIES/layoutlib_intermediates/javalib.jar"),
                        parseUrl("file:///Volumes/AndroidSource/out/host/common/obj/JAVA_LIBRARIES/layoutlib_api_intermediates/javalib.jar")
                }, null);
                defaultLoader = new RobolectricClassLoader(classLoader, ShadowWrangler.getInstance());
            } else {
                defaultLoader = new RobolectricClassLoader(ShadowWrangler.getInstance());
            }
        }
        return defaultLoader;
    }

    private static URL parseUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        this(testClass, ShadowWrangler.getInstance(), getDefaultLoader());
    }

    public AbstractRobolectricTestRunner(Class<?> testClass, Class<? extends TestHelperInterface> testHelperClass) throws InitializationError {
        this(testClass);
        setTestHelperClass(testHelperClass);
    }

    public AbstractRobolectricTestRunner(Class<?> testClass, ClassHandler classHandler, RobolectricClassLoader loader) throws InitializationError {
        super(loader.bootstrap(testClass));
        this.classHandler = classHandler;
        this.loader = loader;

        this.loader.delegateLoadingOf(TestHelperInterface.class.getName());
        this.loader.delegateLoadingOf(RealObject.class.getName());
        this.loader.delegateLoadingOf(ShadowWrangler.class.getName());
    }

    public void delegateLoadingOf(String className) {
        loader.delegateLoadingOf(className);
    }

    public void setTestHelperClass(Class<? extends TestHelperInterface> testHelperClass) {
        this.testHelperClass = testHelperClass;
    }

    @Override protected Statement methodBlock(final FrameworkMethod method) {
        if (classHandler != null) classHandler.beforeTest();
        beforeTest(method.getMethod());

        final Statement statement = super.methodBlock(method);
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                // todo: this try/finally probably isn't right -- should mimic RunAfters? [xw]
                try {
                    statement.evaluate();
                } finally {
                    afterTest(method.getMethod());
                    if (classHandler != null) classHandler.afterTest();
                }
            }
        };
    }

     protected void beforeTest(Method method) {
         if (testHelperClass != null) {
             testHelper = createTestHelper(method);
             testHelper.before(method);
         }
     }

     protected void afterTest(Method method) {
         if (testHelperClass != null) {
             testHelper.after(method);
         }
     }

     @Override
     protected Object createTest() throws Exception {
         Object test = super.createTest();
         if (testHelperClass != null) {
             testHelper.prepareTest(test);
         }
         return test;
     }

     private TestHelperInterface createTestHelper(Method method) {
         Class<?> testClass = method.getDeclaringClass();
         try {
             return (TestHelperInterface) testClass.getClassLoader().loadClass(testHelperClass.getName()).newInstance();
         } catch (ClassNotFoundException e) {
             throw new RuntimeException(e);
         } catch (InstantiationException e) {
             throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
             throw new RuntimeException(e);
         }
     }

}
