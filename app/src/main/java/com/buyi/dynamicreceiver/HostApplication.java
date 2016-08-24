package com.buyi.dynamicreceiver;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by chan on 16/5/15.
 */
public class HostApplication extends Application {

    private File m_apk;
    private DexClassLoader m_dexClassLoader;

    private static Context sContext;

    /**
     * 两个反射工具类
     *
     * @param obj
     * @param fieldName
     * @return
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        Class<?> c = obj.getClass();
        Object value = null;
        while (c != null) {
            try {
                Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                value = field.get(obj);
                if (value != null) {
                    return value;
                }
            } catch (SecurityException e) {
            } catch (NoSuchFieldException e) {
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } finally {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setFieldValue(Object obj, String fieldName,
                                      Object fieldValue) {
        if (obj == null || fieldName == null) {
            return;
        }
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, fieldValue);
                return;
            } catch (SecurityException e) {
            } catch (NoSuchFieldException e) {
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } finally {
                clazz = clazz.getSuperclass();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(new ContextWrapperSelf(base));
        sContext = base;




//        try {
//            Context mBase = (Context) getFieldValue(this, "mBase");
//            Class<?> IContext = Class.forName("com.buyi.dynamicreceiver.IContext");
//            Object baseProxy = Proxy.newProxyInstance(getClassLoader(),
//                    new Class<?>[]{IContext}, new IContextHandler(mBase));
//            setFieldValue(this, "mBase", baseProxy);
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }

    }

    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            obtainApkFromServer();
            setupClazzLoader();
            setupPluginReceiverLoader();
//            hookActivityManagerNative();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

//    public void hookActivityManagerNative() throws ClassNotFoundException,
//            NoSuchMethodException, InvocationTargetException,
//            IllegalAccessException, NoSuchFieldException {
//
//        //        17package android.util;
//        //        18
//        //        19/**
//        //         20 * Singleton helper class for lazily initialization.
//        //         21 *
//        //         22 * Modeled after frameworks/base/include/utils/Singleton.h
//        //         23 *
//        //         24 * @hide
//        //         25 */
//        //        26public abstract class Singleton<T> {
//        //            27    private T mInstance;
//        //            28
//        //                    29    protected abstract T create();
//        //            30
//        //                    31    public final T get() {
//        //                32        synchronized (this) {
//        //                    33            if (mInstance == null) {
//        //                        34                mInstance = create();
//        //                        35            }
//        //                    36            return mInstance;
//        //                    37        }
//        //                38    }
//        //            39}
//        //        40
//
//        Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
//
//        Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
//        gDefaultField.setAccessible(true);
//
//        Object gDefault = gDefaultField.get(null);
//
//        // gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
//        Class<?> singleton = Class.forName("android.util.Singleton");
//        Field mInstanceField = singleton.getDeclaredField("mInstance");
//        mInstanceField.setAccessible(true);
//
//        // ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
//        Object rawIActivityManager = mInstanceField.get(gDefault);
//
//        // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
//        Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
//        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
//                new Class<?>[]{iActivityManagerInterface}, new IActivityManagerHandler(rawIActivityManager));
//        mInstanceField.set(gDefault, proxy);
//
//    }

    private void obtainApkFromServer() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.plugin);
        byte[] bytes = new byte[256];
        int length = -1;


        File dir = getDir("plugin", MODE_PRIVATE);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        m_apk = new File(dir, "plugin.apk");
        FileOutputStream fileOutputStream = new FileOutputStream(m_apk);

        while ((length = inputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, length);
        }

        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private void setupClazzLoader() {
        m_dexClassLoader = new DexClassLoader(
                m_apk.getAbsolutePath(),
                getDir("pluginOpt", MODE_PRIVATE).getAbsolutePath(),
                null, getClassLoader());
    }

    private void setupPluginReceiverLoader() throws
            ClassNotFoundException, NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, InstantiationException, InvocationTargetException {
        Class<?> packageParserClazz = Class.forName("android.content.pm.PackageParser", false, getClassLoader());

        /**
         * Parse the package at the given location. Automatically detects if the
         * package is a monolithic style (single APK file) or cluster style
         * (directory of APKs).
         * <p>
         * This performs sanity checking on cluster style packages, such as
         * requiring identical package name and version codes, a single base APK,
         * and unique split names.
         * <p>
         * Note that this <em>does not</em> perform signature verification; that
         * must be done separately in {@link #collectCertificates(Package, int)}.
         *
         * @see #parsePackageLite(File, int)
         */


        Class<?>[] arrayParseType = new Class[4];
        arrayParseType[0] = File.class;
        arrayParseType[1] = String.class;
        arrayParseType[2] = DisplayMetrics.class;
        arrayParseType[3] = Integer.TYPE;

        Object[] arrayParseParam = new Object[4];
        arrayParseParam[0] = m_apk;
        arrayParseParam[1] = m_apk.getAbsolutePath();
        arrayParseParam[2] = getResources().getDisplayMetrics();
        arrayParseParam[3] = Integer.valueOf(0);

        Method parsePackageMethod = packageParserClazz.getDeclaredMethod("parsePackage", arrayParseType);

        //生成一个package parser 对象
        Constructor<?> packageParseConstructorMethod = packageParserClazz.getConstructor(String.class);
        // 根据构造方法信息构造PackageParser类实例
        Object packageParserObject = packageParseConstructorMethod.newInstance(m_apk.getAbsolutePath());

        //获得Package对象
        Object packageObject = parsePackageMethod.invoke(packageParserObject, arrayParseParam);

        //获得package的receivers域
        Class<?> packageClazz = Class.forName("android.content.pm.PackageParser$Package", false, getClassLoader());
        Field receiversField = packageClazz.getDeclaredField("receivers");
        receiversField.setAccessible(true);

        //获得所有的receivers 他是PackageParser$Activity类型的
        List<?> receiversList = (List<?>) receiversField.get(packageObject);

        /*
         * 现在已经获得了所有的receiver
         * 就只剩获得receiver的intent filter
         * 下面就开始获得receiver的intent filter
         * 其中receiversList容器的模板实参类型是 android.content.pm.PackageParser$Activity
        * */



        /*
        * android.content.pm.PackageParser$Activity 其实是 android.content.pm.PackageParser$Component
        * public final static class Activity extends Component<ActivityIntentInfo>
        * 其中域 intents存放的是action信息
        * public final ArrayList<II> intents;
        *
        * 而II类型是 Component<II extends IntentInfo> 模板参数
        *
        * */
        Class<?> componentClazz = Class.forName("android.content.pm.PackageParser$Component");
        Field intentsField = componentClazz.getDeclaredField("intents");

        Class<?> packageParser$ActivityIntentInfoClazz = Class.forName(
                "android.content.pm.PackageParser$ActivityIntentInfo",
                false, getClassLoader());


        Method countActionsMethod = packageParser$ActivityIntentInfoClazz.getMethod("countActions");
        Method getActionMethod = packageParser$ActivityIntentInfoClazz.getMethod("getAction", int.class);

        Map<String, List<String>> receiverAndIntentFilterMap = new HashMap<>();

        /*
        * 下面的receiver 其实是 android.content.pm.PackageParser$Activity
        * 他有一个field 名为className 就是存放的receiver的className
        * 我们获得 这个className就能通过反射获得receiver对象
        * */
        Class<?> packageParser$ActivityClazz = Class.forName("android.content.pm.PackageParser$Activity", false, getClassLoader());
        Field classNameField = packageParser$ActivityClazz.getField("className");



        for (Object receiver : receiversList) {

            List<?> activityIntentInfoList = (List<?>) intentsField.get(receiver);

            if (activityIntentInfoList != null) {

                List<String> intentFilter = new ArrayList<>();
                for (Object activityIntentInfo : activityIntentInfoList) {

                    //添加所有的action 到intent filter中
                    final int count = (int) countActionsMethod.invoke(activityIntentInfo);
                    for (int i = 0; i < count; ++i) {
                        intentFilter.add((String) getActionMethod.invoke(activityIntentInfo, i));
                    }
                }

                //记录下来
                receiverAndIntentFilterMap.put((String) classNameField.get(receiver), intentFilter);
            }
        }

        PluginReceiverLoader.init(m_dexClassLoader, receiverAndIntentFilterMap);
    }

//    class IActivityManagerHandler implements InvocationHandler {
//
//        private static final String TAG = "IActivityManagerHandler";
//
//        Object mBase;
//
//        public IActivityManagerHandler(Object base) {
//            mBase = base;
//        }
//
//        @Override
//        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//            System.out.println("method:" + method.getName());
//            if ("registerReceiver".equals(method.getName())) {
//                // 只拦截这个方法
//                // 替换参数, 任你所为;甚至替换原始Activity启动别的Activity偷梁换柱
//                // API 23:
//                // public final Activity startActivityNow(Activity parent, String id,
//                // Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state,
//                // Activity.NonConfigurationInstances lastNonConfigurationInstances) {
//
//                // 找到参数里面的第一个Intent 对象
//
//                IntentFilter raw;
//                int index = 0;
//
//                for (int i = 0; i < args.length; i++) {
//                    if (args[i] instanceof IntentFilter) {
//                        index = i;
//                        break;
//                    }
//                }
//                raw = (IntentFilter) args[index];
//                System.out.println("raw:" + raw);
//
////                Intent newIntent = new Intent();
////
////                // 替身Activity的包名, 也就是我们自己的"包名", Application Id, 如果用gradle打包
////                String stubPackage = UPFApplication.getContext().getPackageName();
////
////                // 这里我们把启动的Activity临时替换为 StubActivity
////                ComponentName componentName = new ComponentName(stubPackage, StubActivity.class.getName());
////                newIntent.setComponent(componentName);
////
////                // 把我们原始要启动的TargetActivity先存起来
////                newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);
////
////                // 替换掉Intent, 达到欺骗AMS的目的
////                args[index] = newIntent;
////
//
//                Log.d(TAG, "hook success");
//                return method.invoke(mBase, args);
//
//            }
//
//            return method.invoke(mBase, args);
//        }
//    }

}

class IContextHandler implements InvocationHandler {

    private static final String TAG = "IActivityManagerHandler";
    private List<BroadcastReceiver> receivers = new ArrayList<>();
    private List<IntentFilter> filters = new ArrayList<>();
    Object mBase;

    public IContextHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("method.getName()" + method.getName());
        if ("registerReceiver".equals(method.getName()))  {
            IntentFilter raw;
            int index = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof IntentFilter) {
                    index = i;
                    break;
                }
                System.out.println("arg[i]" + args[i]);
            }
            raw = (IntentFilter) args[index];


            receivers.add( (BroadcastReceiver)args[0]);
            filters.add((IntentFilter)args[1]);
        }

        return method.invoke(mBase, args);
    }
}


