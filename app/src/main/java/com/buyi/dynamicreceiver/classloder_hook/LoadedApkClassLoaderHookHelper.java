package com.buyi.dynamicreceiver.classloder_hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.DisplayMetrics;

import com.buyi.dynamicreceiver.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author weishu
 * @date 16/3/29
 */
public class LoadedApkClassLoaderHookHelper {

    public static Map<String, Object> sLoadedApk = new HashMap<String, Object>();

    public static void hookLoadedApkInActivityThread(Context context, File apkFile) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, InstantiationException {

        // 先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        // 获取到 mPackages 这个静态成员变量, 这里缓存了dex包的信息
        Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        Map mPackages = (Map) mPackagesField.get(currentActivityThread);

        // android.content.res.CompatibilityInfo
        Class<?> compatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Method getPackageInfoNoCheckMethod = activityThreadClass.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, compatibilityInfoClass);

        Field defaultCompatibilityInfoField = compatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        defaultCompatibilityInfoField.setAccessible(true);

        Object defaultCompatibilityInfo = defaultCompatibilityInfoField.get(null);
        ApplicationInfo applicationInfo = generateApplicationInfo(context, apkFile);

        Object loadedApk = getPackageInfoNoCheckMethod.invoke(currentActivityThread, applicationInfo, defaultCompatibilityInfo);

        String odexPath = Utils.getPluginOptDexDir(applicationInfo.packageName).getPath();
        String libDir = Utils.getPluginLibDir(applicationInfo.packageName).getPath();
        ClassLoader classLoader = new CustomClassLoader(apkFile.getPath(), odexPath, libDir, ClassLoader.getSystemClassLoader());
        Field mClassLoaderField = loadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        mClassLoaderField.set(loadedApk, classLoader);

        // 由于是弱引用, 因此我们必须在某个地方存一份, 不然容易被GC; 那么就前功尽弃了.
        sLoadedApk.put(applicationInfo.packageName, loadedApk);

        WeakReference weakReference = new WeakReference(loadedApk);
        mPackages.put(applicationInfo.packageName, weakReference);
    }

    /**
     * 这个方法的最终目的是调用
     * android.content.pm.PackageParser#generateActivityInfo(android.content.pm.PackageParser.Activity, int, android.content.pm.PackageUserState, int)
     */
    public static ApplicationInfo generateApplicationInfo(Context context, File apkFile)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchFieldException {

        // 找出需要反射的核心类: android.content.pm.PackageParser
        Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");

        // 我们的终极目标: android.content.pm.PackageParser#generateApplicationInfo(android.content.pm.PackageParser.Package,
        // int, android.content.pm.PackageUserState)
        // 要调用这个方法, 需要做很多准备工作; 考验反射技术的时候到了 - -!
        // 下面, 我们开始这场Hack之旅吧!

        // 首先拿到我们得终极目标: generateApplicationInfo方法
        // API 23 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // public static ApplicationInfo generateApplicationInfo(Package p, int flags,
        //    PackageUserState state) {
        // 其他Android版本不保证也是如此.
        Class<?> packageParser$PackageClass = Class.forName("android.content.pm.PackageParser$Package");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Method generateApplicationInfoMethod = packageParserClass.getDeclaredMethod("generateApplicationInfo",
                packageParser$PackageClass,
                int.class,
                packageUserStateClass);

        // 接下来构建需要得参数

//        // 首先, 我们得创建出一个Package对象出来供这个方法调用
//        // 而这个需要得对象可以通过 android.content.pm.PackageParser#parsePackage 这个方法返回得 Package对象得字段获取得到
//        // 创建出一个PackageParser对象供使用
//        Object packageParser = packageParserClass.newInstance();
//        // 调用 PackageParser.parsePackage 解析apk的信息
//        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
//
//        // 实际上是一个 android.content.pm.PackageParser.Package 对象
//        Object packageObj = parsePackageMethod.invoke(packageParser, apkFile, 0);


        String apkPath = apkFile.getPath();
        Class<?>[] arrayParseType = new Class[4];
        arrayParseType[0] = File.class;
        arrayParseType[1] = String.class;
        arrayParseType[2] = DisplayMetrics.class;
        arrayParseType[3] = Integer.TYPE;

        Object[] arrayParseParam = new Object[4];
        arrayParseParam[0] = apkFile;
        arrayParseParam[1] = apkPath;
        arrayParseParam[2] = context.getResources().getDisplayMetrics();
        arrayParseParam[3] = Integer.valueOf(0);

        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", arrayParseType);


        // 第三个参数 mDefaultPackageUserState 我们直接使用默认构造函数构造一个出来即可
        Object defaultPackageUserState = packageUserStateClass.newInstance();

        //生成一个package parser 对象
        Constructor<?> packageParseConstructorMethod = packageParserClass.getConstructor(String.class);
        // 根据构造方法信息构造PackageParser类实例
        Object packageParserObject = packageParseConstructorMethod.newInstance(apkPath);

        //获得Package对象
        Object packageObject = parsePackageMethod.invoke(packageParserObject, arrayParseParam);

        // 万事具备!!!!!!!!!!!!!!
        ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(packageParserObject,
                packageObject, 0, defaultPackageUserState);


        applicationInfo.sourceDir = apkPath;
        applicationInfo.publicSourceDir = apkPath;

        return applicationInfo;
    }
}
